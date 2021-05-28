package ml.karmaconfigs.locklogin.plugin.velocity.plugin;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.velocity.Console;
import ml.karmaconfigs.api.velocity.Util;
import ml.karmaconfigs.api.velocity.karmayaml.FileCopy;
import ml.karmaconfigs.api.velocity.timer.AdvancedPluginTimer;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.files.PluginConfiguration;
import ml.karmaconfigs.locklogin.api.modules.api.event.user.UserHookEvent;
import ml.karmaconfigs.locklogin.api.modules.api.event.user.UserUnHookEvent;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.utils.platform.CurrentPlatform;
import ml.karmaconfigs.locklogin.plugin.common.security.client.IpData;
import ml.karmaconfigs.locklogin.plugin.common.security.client.Proxy;
import ml.karmaconfigs.locklogin.plugin.common.session.Session;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionKeeper;
import ml.karmaconfigs.locklogin.plugin.common.utils.DataType;
import ml.karmaconfigs.locklogin.plugin.common.utils.other.ASCIIArtGenerator;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.ServerDataStorager;
import ml.karmaconfigs.locklogin.plugin.common.web.AlertSystem;
import ml.karmaconfigs.locklogin.plugin.common.web.VersionChecker;
import ml.karmaconfigs.locklogin.plugin.common.web.VersionDownloader;
import ml.karmaconfigs.locklogin.plugin.velocity.Main;
import ml.karmaconfigs.locklogin.plugin.velocity.command.util.BungeeLikeCommand;
import ml.karmaconfigs.locklogin.plugin.velocity.command.util.SystemCommand;
import ml.karmaconfigs.locklogin.plugin.velocity.listener.ChatListener;
import ml.karmaconfigs.locklogin.plugin.velocity.listener.JoinListener;
import ml.karmaconfigs.locklogin.plugin.velocity.listener.MessageListener;
import ml.karmaconfigs.locklogin.plugin.velocity.listener.QuitListener;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.client.PlayerFile;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.Config;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.data.RestartCache;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.data.lock.LockedAccount;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.velocity.util.filter.ConsoleFilter;
import ml.karmaconfigs.locklogin.plugin.velocity.util.filter.PluginFilter;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.SessionCheck;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;
import org.reflections.Reflections;

import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static ml.karmaconfigs.api.common.Console.Colors.YELLOW_BRIGHT;
import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.*;
import static ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender.*;
import static ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender.CHANNEL_PLAYER;

public final class Manager {

    private static int changelog_requests = 0;
    private static int updater_id = 0;
    private static int alert_id = 0;

    public static void initialize() {
        int size = 10;
        String character = "*";
        try {
            size = Integer.parseInt(properties.getProperty("ascii_art_size", "10"));
            character = properties.getProperty("ascii_art_character", "*").substring(0, 1);
        } catch (Throwable ignored) {
        }

        System.out.println();
        artGen.print(YELLOW_BRIGHT, "LockLogin", size, ASCIIArtGenerator.ASCIIArtFont.ART_FONT_SANS_SERIF, character);
        Console.send("&eversion:&6 {0}", versionID);

        Proxy.scan();

        PlayerFile.migrateV1();
        PlayerFile.migrateV2();

        setupFiles();
        registerCommands();
        registerListeners();

        Console.send(" ");
        Console.send("&e-----------------------");

        if (!CurrentPlatform.isValidAccountManager()) {
            CurrentPlatform.setAccountsManager(PlayerFile.class);
            Console.send(plugin, "Loaded native player account manager", Level.INFO);
        } else {
            Console.send(plugin, "Loaded custom player account manager", Level.INFO);
        }
        if (!CurrentPlatform.isValidSessionManager()) {
            CurrentPlatform.setSessionManager(Session.class);
            Console.send(plugin, "Loaded native player session manager", Level.INFO);
        } else {
            Console.send(plugin, "Loaded custom player session manager", Level.INFO);
        }

        loadCache();

        server.getChannelRegistrar().register(new LegacyChannelIdentifier(DataSender.CHANNEL_PLAYER));
        server.getChannelRegistrar().register(new LegacyChannelIdentifier(DataSender.PLUGIN_CHANNEL));
        server.getChannelRegistrar().register(new LegacyChannelIdentifier(DataSender.ACCESS_CHANNEL));

        AccountManager manager = CurrentPlatform.getAccountManager(null);
        if (manager != null) {
            Set<AccountManager> accounts = manager.getAccounts();
            Set<AccountManager> nonLocked = new HashSet<>();
            for (AccountManager account : accounts) {
                LockedAccount locked = new LockedAccount(account.getUUID());
                if (!locked.getData().isLocked())
                    nonLocked.add(account);
            }

            SessionDataContainer.setRegistered(nonLocked.size());

            SessionDataContainer.onDataChange(data -> {
                try {
                    Collection<RegisteredServer> servers = server.getAllServers();

                    switch (data.getDataType()) {
                        case LOGIN:
                            for (RegisteredServer server : servers) {
                                DataSender.send(server, DataSender.getBuilder(DataType.LOGGED, DataSender.PLUGIN_CHANNEL, null).addIntData(SessionDataContainer.getLogged()).build());
                            }
                            break;
                        case REGISTER:
                            for (RegisteredServer server : servers) {
                                DataSender.send(server, DataSender.getBuilder(DataType.REGISTERED, DataSender.PLUGIN_CHANNEL, null).addIntData(SessionDataContainer.getRegistered()).build());
                            }
                            break;
                        default:
                            break;
                    }
                } catch (Throwable ignored) {
                }
            });
        }

        PluginConfiguration config = CurrentPlatform.getConfiguration();
        if (config.getUpdaterOptions().isEnabled()) {
            scheduleVersionCheck();
        } else {
            performVersionCheck();
        }
        scheduleAlertSystem();

        registerMetrics();
        initPlayers();

        CurrentPlatform.setPrefix(config.getModulePrefix());
    }

    public static void terminate() {
        try {
            Console.send(plugin, "Finalizing console filter, please wait", Level.INFO);
            Logger coreLogger = (Logger) LogManager.getRootLogger();

            Iterator<Filter> filters = coreLogger.getFilters();
            if (filters != null) {
                while (filters.hasNext()) {
                    Filter filter = filters.next();
                    if (filter.getClass().isAnnotationPresent(PluginFilter.class))
                        filter.stop();
                }
            }
        } catch (Throwable ignored) {
        }

        int size = 10;
        String character = "*";
        try {
            size = Integer.parseInt(properties.getProperty("ascii_art_size", "10"));
            character = properties.getProperty("ascii_art_character", "*").substring(0, 1);
        } catch (Throwable ignored) {
        }

        System.out.println();
        artGen.print(ml.karmaconfigs.api.common.Console.Colors.RED_BRIGHT, "LockLogin", size, ASCIIArtGenerator.ASCIIArtFont.ART_FONT_SANS_SERIF, character);
        Console.send("&eversion:&6 {0}", versionID);
        Console.send(" ");
        Console.send("&e-----------------------");

        endPlayers();
    }

    /**
     * Register plugin commands
     */
    protected static void registerCommands() {
        Set<String> unregistered = new LinkedHashSet<>();
        Set<String> registered = new HashSet<>();

        Reflections reflections = new Reflections("ml.karmaconfigs.locklogin.plugin.velocity.command");
        Set<Class<?>> commands = reflections.getTypesAnnotatedWith(SystemCommand.class);

        for (Class<?> clazz : commands) {
            try {
                String command = SystemCommand.manager.getDeclaredCommand(clazz);

                if (command != null && !command.replaceAll("\\s", "").isEmpty()) {
                    Object instance = clazz.getDeclaredConstructor(String.class).newInstance(command);

                    if (instance instanceof BungeeLikeCommand) {
                        BungeeLikeCommand executor = (BungeeLikeCommand) instance;

                        server.getCommandManager().register(command, (SimpleCommand) invocation -> executor.execute(invocation.source(), invocation.arguments()));

                        registered.add("/" + command);
                    } else {
                        unregistered.add(command);
                    }
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        if (!unregistered.isEmpty()) {
            Console.send(plugin, properties.getProperty("command_register_problem", "Failed to register command(s): {0}"), Level.GRAVE, setToString(unregistered));
            Console.send(plugin, properties.getProperty("plugin_error_disabling", "Disabling plugin due an internal error"), Level.INFO);
        } else {
            Console.send(plugin, properties.getProperty("plugin_filter_initialize", "Initializing console filter to protect user data"), Level.INFO);

            try {
                ConsoleFilter filter = new ConsoleFilter(registered);

                Logger coreLogger = (Logger) LogManager.getRootLogger();
                coreLogger.addFilter(filter);
            } catch (Throwable ex) {
                logger.scheduleLog(Level.GRAVE, ex);
                logger.scheduleLog(Level.INFO, "Failed to register console filter");

                Console.send(plugin, properties.getProperty("plugin_filter_error", "An error occurred while initializing console filter, check logs for more info"), Level.GRAVE);
                Console.send(plugin, properties.getProperty("plugin_error_disabling", "Disabling plugin due an internal error"), Level.INFO);

            }
        }
    }

    /**
     * Setup the plugin files
     */
    protected static void setupFiles() {
        Util util = new Util(plugin);
        Set<String> failed = new LinkedHashSet<>();

        File cfg = new File(util.getDataFolder(), "config.yml");
        File proxy = new File(util.getDataFolder(), "proxy.yml");

        FileCopy config_copy = new FileCopy(plugin, "cfg/config.yml");
        FileCopy proxy_copy = new FileCopy(plugin, "cfg/proxy.yml");
        try {
            config_copy.copy(cfg);
        } catch (Throwable ex) {
            failed.add("config.yml");
        }
        try {
            proxy_copy.copy(proxy);
        } catch (Throwable ex) {
            failed.add("proxy.yml");
        }

        Config config = new Config();
        ml.karmaconfigs.locklogin.plugin.velocity.util.files.Proxy proxy_cfg = new ml.karmaconfigs.locklogin.plugin.velocity.util.files.Proxy();
        CurrentPlatform.setConfigManager(config);
        CurrentPlatform.setProxyManager(proxy_cfg);

        String country = config.getLang().country(config.getLangName());
        File msg_file = new File(util.getDataFolder() + File.separator + "lang" + File.separator + "v2", "messages_" + country + ".yml");

        InputStream internal = Main.class.getResourceAsStream("/lang/messages_" + country + ".yml");
        //Check if the file exists inside the plugin as an official language
        if (internal != null) {
            if (!msg_file.exists()) {
                FileCopy copy = new FileCopy(plugin, "lang/messages_" + country + ".yml");

                try {
                    copy.copy(msg_file);
                } catch (Throwable ex) {
                    failed.add(msg_file.getName());
                }
            }
        } else {
            if (!msg_file.exists()) {
                failed.add(msg_file.getName());
                Console.send(plugin, "Could not find community message pack named {0} in lang_v2 folder, using messages english as default", Level.GRAVE, msg_file.getName());

                msg_file = new File(util.getDataFolder() + File.separator + "lang" + File.separator + "v2", "messages_en.yml");

                if (!msg_file.exists()) {
                    FileCopy copy = new FileCopy(plugin, "lang/messages_en.yml");

                    try {
                        copy.copy(msg_file);
                    } catch (Throwable ex) {
                        failed.add(msg_file.getName());
                    }
                }
            } else {
                Console.send(plugin, "Detected community language pack, please make sure this pack is updated to avoid translation errors", Level.WARNING);
            }
        }

        if (!failed.isEmpty()) {
            Console.send(plugin, properties.getProperty("file_register_problem", "Failed to setup/check file(s): {0}. The plugin will use defaults, you can try to create files later by running /locklogin reload"), Level.WARNING, setToString(failed));
        }

        Config.manager.checkValues();
    }

    /**
     * Register plugin metrics
     */
    protected static void registerMetrics() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();
        Metrics metrics = factory.make(main, 11291);

        metrics.addCustomChart(new SimplePie("used_locale", () -> config.getLang().friendlyName(config.getLangName())));
        metrics.addCustomChart(new SimplePie("clear_chat", () -> String.valueOf(config.clearChat())
                .replace("true", "Clear chat")
                .replace("false", "Don't clear chat")));
        metrics.addCustomChart(new SimplePie("sessions_enabled", () -> String.valueOf(config.enableSessions())
                .replace("true", "Sessions enabled")
                .replace("false", "Sessions disabled")));
    }

    /**
     * Register the plugin listeners
     */
    protected static void registerListeners() {
        JoinListener onJoin = new JoinListener();
        QuitListener onQuit = new QuitListener();
        ChatListener onChat = new ChatListener();
        MessageListener onMessage = new MessageListener();

        server.getEventManager().register(plugin, onJoin);
        server.getEventManager().register(plugin, onQuit);
        server.getEventManager().register(plugin, onChat);
        server.getEventManager().register(plugin, onMessage);
    }

    /**
     * Load the plugin cache if exists
     */
    protected static void loadCache() {
        RestartCache cache = new RestartCache();
        cache.loadBungeeKey();
        cache.loadUserData();

        cache.remove();
    }

    /**
     * Perform a version check
     */
    protected static void performVersionCheck() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        VersionChecker checker = new VersionChecker(versionID);
        checker.checkVersion(config.getUpdaterOptions().getChannel());

        if (checker.isOutdated()) {
            if (changelog_requests <= 0) {
                changelog_requests = 3;

                Console.send(checker.getChangelog());

                if (!VersionDownloader.isDownloading()) {
                    VersionDownloader downloader = new VersionDownloader(versionID, config.getUpdaterOptions().getChannel());
                    downloader.download(
                            file -> Console.send(plugin, properties.getProperty("updater_downloaded", "Downloaded latest version plugin instance, to apply the updates run /locklogin applyUpdates"), Level.INFO),
                            error -> {
                                if (error != null) {
                                    logger.scheduleLog(Level.GRAVE, error);
                                    logger.scheduleLog(Level.INFO, "Failed to download latest LockLogin instance");
                                    Console.send(plugin, properties.getProperty("updater_download_fail", "Failed to download latest LockLogin update ( {0} )"), Level.INFO, error.fillInStackTrace());
                                }
                            });
                }
            } else {
                changelog_requests--;
            }
        }
    }

    /**
     * Schedule the version check process
     */
    protected static void scheduleVersionCheck() {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        AdvancedPluginTimer timer = new AdvancedPluginTimer(plugin, config.getUpdaterOptions().getInterval(), true).setAsync(true).addActionOnEnd(Manager::performVersionCheck);
        if (config.getUpdaterOptions().isEnabled())
            timer.start();

        updater_id = timer.getTimerId();

    }

    /**
     * Schedule the alert system
     */
    protected static void scheduleAlertSystem() {
        AdvancedPluginTimer timer = new AdvancedPluginTimer(plugin, 30, true).setAsync(true).addActionOnEnd(() -> {
            AlertSystem system = new AlertSystem();
            system.checkAlerts();

            if (system.available())
                Console.send(system.getMessage());
        });
        timer.start();

        alert_id = timer.getTimerId();

    }

    /**
     * Initialize already connected players
     *
     * This is util after plugin updates or
     * plugin load using third-party loaders
     */
    protected static void initPlayers() {
        server.getScheduler().buildTask(plugin, () -> {
            PluginConfiguration config = CurrentPlatform.getConfiguration();
            Message messages = new Message();

            for (Player player : server.getAllPlayers()) {
                server.getScheduler().buildTask(plugin, () -> {
                    InetSocketAddress ip = player.getRemoteAddress();
                    IpData data = new IpData(ip.getAddress());
                    int amount = data.getClonesAmount();
                    User user = new User(player);

                    if (amount + 1 == config.accountsPerIP()) {
                        user.kick(messages.maxIP());
                        return;
                    }
                    data.addClone();

                    Optional<ServerConnection> tmp_server = player.getCurrentServer();
                    if (tmp_server.isPresent()) {
                        ServerConnection connection = tmp_server.get();

                        RegisteredServer info = connection.getServer();
                        ml.karmaconfigs.locklogin.plugin.velocity.util.files.Proxy proxy = new ml.karmaconfigs.locklogin.plugin.velocity.util.files.Proxy();

                        if (ServerDataStorager.needsRegister(info.getServerInfo().getName()) || ServerDataStorager.needsProxyKnowledge(info.getServerInfo().getName())) {
                            if (ServerDataStorager.needsRegister(info.getServerInfo().getName()))
                                DataSender.send(info, DataSender.getBuilder(DataType.KEY, ACCESS_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(info.getServerInfo().getName()).addBoolData(proxy.multiBungee()).build());

                            if (ServerDataStorager.needsProxyKnowledge(info.getServerInfo().getName()))
                                DataSender.send(info, DataSender.getBuilder(DataType.REGISTER, ACCESS_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(info.getServerInfo().getName()).build());
                        }
                    }

                    DataSender.send(player, DataSender.getBuilder(DataType.MESSAGES, PLUGIN_CHANNEL, player).addTextData(Message.manager.getMessages()).build());
                    DataSender.send(player, DataSender.getBuilder(DataType.CONFIG, PLUGIN_CHANNEL, player).addTextData(Message.manager.getMessages()).build());
                    DataSender.send(player, DataSender.getBuilder(DataType.LOGGED, PLUGIN_CHANNEL, player).addIntData(SessionDataContainer.getLogged()).build());
                    DataSender.send(player, DataSender.getBuilder(DataType.REGISTERED, PLUGIN_CHANNEL, player).addIntData(SessionDataContainer.getRegistered()).build());

                    DataSender.MessageData validation = getBuilder(DataType.VALIDATION, DataSender.CHANNEL_PLAYER, player).build();
                    DataSender.send(player, validation);

                    Proxy proxy = new Proxy(ip);
                    if (proxy.isProxy()) {
                        user.kick(messages.ipProxyError());
                        return;
                    }

                    user.applySessionEffects();

                    if (config.clearChat()) {
                        for (int i = 0; i < 150; i++)
                            server.getScheduler().buildTask(plugin, () -> player.sendMessage(Component.text().content("").build()));
                    }

                    ClientSession session = user.getSession();
                    session.validate();

                    if (!config.captchaOptions().isEnabled())
                        session.setCaptchaLogged(true);

                    AdvancedPluginTimer tmp_timer = null;
                    if (!session.isCaptchaLogged()) {
                        tmp_timer = new AdvancedPluginTimer(plugin, 1, true);
                        tmp_timer.addAction(() -> player.sendActionBar(Component.text().content(StringUtils.toColor(messages.captcha(session.getCaptcha()))).build())).start();
                    }

                    MessageData join = DataSender.getBuilder(DataType.JOIN, CHANNEL_PLAYER, player)
                            .addBoolData(session.isLogged())
                            .addBoolData(session.is2FALogged())
                            .addBoolData(session.isPinLogged())
                            .addBoolData(user.isRegistered()).build();
                    DataSender.send(player, join);

                    AdvancedPluginTimer timer = tmp_timer;
                    SessionCheck check = new SessionCheck(player, target -> {
                        player.sendActionBar(Component.text().content("").build());
                        if (timer != null)
                            timer.setCancelled();
                    }, target -> {
                        player.sendActionBar(Component.text().content("").build());
                        if (timer != null)
                            timer.setCancelled();
                    });

                    server.getScheduler().buildTask(plugin, check).schedule();

                    user.checkServer();

                    UserHookEvent event = new UserHookEvent(fromPlayer(player), null);
                    JavaModuleManager.callEvent(event);
                }).delay(2, TimeUnit.SECONDS).schedule();
            }
        }).schedule();
    }

    /**
     * Finalize connected players sessions
     *
     * This is util after plugin updates or
     * plugin unload using third-party loaders
     */
    protected static void endPlayers() {
        for (Player player : server.getAllPlayers()) {
            InetSocketAddress ip = player.getRemoteAddress();
            User user = new User(player);

            SessionKeeper keeper = new SessionKeeper(fromPlayer(player));
            keeper.store();

            if (ip != null) {
                IpData data = new IpData(ip.getAddress());
                data.delClone();

                ClientSession session = user.getSession();
                session.invalidate();
                session.setLogged(false);
                session.setPinLogged(false);
                session.set2FALogged(false);

                DataSender.send(player, DataSender.getBuilder(DataType.QUIT, DataSender.CHANNEL_PLAYER, player).build());
            }

            UserUnHookEvent event = new UserUnHookEvent(fromPlayer(player), null);
            JavaModuleManager.callEvent(event);
        }
    }

    /**
     * Restart the version checker
     */
    public static void restartVersionChecker() {
        try {
            AdvancedPluginTimer timer = AdvancedPluginTimer.getManager.getTimer(updater_id);
            timer.setCancelled();
        } catch (Throwable ignored) {
        }

        scheduleVersionCheck();
    }

    /**
     * Restart the alert system timer
     */
    public static void restartAlertSystem() {
        try {
            AdvancedPluginTimer timer = AdvancedPluginTimer.getManager.getTimer(alert_id);
            timer.setCancelled();
        } catch (Throwable ignored) {
        }

        scheduleAlertSystem();
    }

    /**
     * Convert a set of strings into a single string
     *
     * @param set the set to convert
     * @return the converted set
     */
    private static String setToString(final Set<String> set) {
        StringBuilder builder = new StringBuilder();
        for (String str : set) {
            builder.append(str.replace(",", "comma")).append(", ");
        }

        return StringUtils.replaceLast(builder.toString(), ", ", "");
    }
}
