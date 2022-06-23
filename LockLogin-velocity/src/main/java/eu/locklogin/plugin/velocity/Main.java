package eu.locklogin.plugin.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.JarManager;
import eu.locklogin.api.common.security.AllowedCommand;
import eu.locklogin.api.common.session.SessionDataContainer;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.utils.dependencies.Dependency;
import eu.locklogin.api.common.utils.dependencies.PluginDependency;
import eu.locklogin.api.common.web.ChecksumTables;
import eu.locklogin.api.common.web.STFetcher;
import eu.locklogin.api.module.LoadRule;
import eu.locklogin.api.module.plugin.api.event.plugin.PluginStatusChangeEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserAuthenticateEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.ActionBarSender;
import eu.locklogin.api.module.plugin.client.MessageSender;
import eu.locklogin.api.module.plugin.client.OpContainer;
import eu.locklogin.api.module.plugin.client.TitleSender;
import eu.locklogin.api.module.plugin.client.permission.PermissionContainer;
import eu.locklogin.api.module.plugin.client.permission.PermissionObject;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.Platform;
import eu.locklogin.plugin.velocity.plugin.Manager;
import eu.locklogin.plugin.velocity.plugin.sender.DataSender;
import eu.locklogin.plugin.velocity.util.player.User;
import ml.karmaconfigs.api.common.karma.KarmaAPI;
import ml.karmaconfigs.api.common.karma.loader.BruteLoader;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.api.velocity.KarmaPlugin;
import net.kyori.adventure.text.Component;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

//TODO: If you see this you can also buy me a coffee ;) [ https://www.buymeacoffee.com/karmadev ]
public class Main extends KarmaPlugin {

    public Main(final ProxyServer server, final PluginContainer plugin) {
        super(server, plugin, false);
        CurrentPlatform.setPlatform(Platform.VELOCITY);
        CurrentPlatform.setMain(VelocityPlugin.class);

        BruteLoader appender = new BruteLoader((URLClassLoader) VelocityPlugin.class.getClassLoader());

        ChecksumTables tables = new ChecksumTables();
        tables.checkTables();

        try {
            JarManager.changeField(CurrentPlatform.class, "current_appender", appender);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void enable() {
        console().send("&aInjected plugin KarmaAPI version {0}, compiled at {1} for jdk {2}", KarmaAPI.getVersion(), KarmaAPI.getBuildDate(), KarmaAPI.getCompilerVersion());

        for (Dependency pluginDependency : Dependency.values()) {
            PluginDependency dependency = pluginDependency.getAsDependency();

            if (FileInfo.showChecksums(null)) {
                console().send("&7----------------------");
                console().send("");
                console().send("&bDependency: &3{0}", dependency.getName());
                console().send("&bType&8/&eCurrent&8/&aFetched");
                console().send("&bAdler32 &8- {0} &8- &a{1}", dependency.getAdlerCheck(), ChecksumTables.getAdler(dependency));
                console().send("&bCRC32 &8- {0} &8- &a{1}", dependency.getCRCCheck(), ChecksumTables.getCRC(dependency));
                console().send("");
                console().send("&7----------------------");
            }

            JarManager manager = new JarManager(dependency);
            manager.process(false);
        }
        JarManager.downloadAll();

        STFetcher fetcher = new STFetcher();
        fetcher.check();

        CurrentPlatform.setOnDataContainerUpdate(() -> {
            for (RegisteredServer server : getServer().getAllServers()) {
                DataSender.send(server, DataSender.getBuilder(DataType.LOGGED, DataSender.PLUGIN_CHANNEL, null).addIntData(SessionDataContainer.getLogged()).build());
                DataSender.send(server, DataSender.getBuilder(DataType.REGISTERED, DataSender.PLUGIN_CHANNEL, null).addIntData(SessionDataContainer.getRegistered()).build());
            }
        });

        console().getData().setOkPrefix("&aOk &e>> &7");
        console().getData().setInfoPrefix("&7Info &e>> &7");
        console().getData().setWarnPrefix("&6Warning &e>> &7");
        console().getData().setGravePrefix("&4Grave &e>> &7");

        Consumer<MessageSender> onMessage = messageSender -> {
            if (messageSender.getSender() instanceof ModulePlayer) {
                ModulePlayer mp = (ModulePlayer) messageSender.getSender();
                Player player = mp.getPlayer();

                if (player != null) {
                    User user = new User(player);
                    user.send(messageSender.getMessage());
                }
            }
        };
        Consumer<ActionBarSender> onActionBar = messageSender -> {
            Player player = messageSender.getPlayer().getPlayer();

            if (player != null) {
                User user = new User(player);

                if (!StringUtils.isNullOrEmpty(messageSender.getMessage())) {
                    Component component = Component.text(messageSender.getMessage());
                    user.send(component);
                } else {
                    Component component = Component.text("");
                    user.send(component);
                }
            }
        };
        Consumer<TitleSender> onTitle = messageSender -> {
            Player player = messageSender.getPlayer().getPlayer();

            if (player != null) {
                User user = new User(player);

                if (StringUtils.isNullOrEmpty(messageSender.getTitle()) && StringUtils.isNullOrEmpty(messageSender.getSubtitle()))
                    user.send("", "", 0, 0, 0);

                user.send(messageSender.getTitle(), messageSender.getSubtitle(), messageSender.getFadeOut(), messageSender.getKeepIn(), messageSender.getHideIn());
            }
        };
        Consumer<MessageSender> onKick = messageSender -> {
            if (messageSender.getSender() instanceof ModulePlayer) {
                ModulePlayer mp = (ModulePlayer) messageSender.getSender();
                Player player = mp.getPlayer();

                if (player != null) {
                    User user = new User(player);
                    user.kick(messageSender.getMessage());
                }
            }
        };
        Consumer<ModulePlayer> onLogin = modulePlayer -> {
            Player player = modulePlayer.getPlayer();

            if (player != null) {
                User user = new User(player);
                ClientSession session = user.getSession();

                if (!session.isLogged() || !session.isTempLogged()) {
                    session.setCaptchaLogged(true);
                    session.setLogged(true);
                    session.setPinLogged(true);
                    session.set2FALogged(true);

                    DataSender.MessageData login = DataSender.getBuilder(DataType.SESSION, "ll:account", player).build();
                    DataSender.MessageData pin = DataSender.getBuilder(DataType.PIN, "ll:account", player).addTextData("close").build();
                    DataSender.MessageData gauth = DataSender.getBuilder(DataType.GAUTH, "ll:account", player).build();

                    DataSender.send(player, login);
                    DataSender.send(player, pin);
                    DataSender.send(player, gauth);

                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.API,
                            UserAuthenticateEvent.Result.SUCCESS,
                            user.getModule(),
                            "",
                            null);
                    ModulePlugin.callEvent(event);

                    user.checkServer(0);
                    user.send(event.getAuthMessage());
                }
            }
        };
        Consumer<ModulePlayer> onClose = modulePlayer -> {
            Player player = modulePlayer.getPlayer();

            if (player != null) {
                User user = new User(player);
                user.performCommand("account close");
            }
        };
        Consumer<PermissionContainer> hasPermission = permContainer -> {
            UUID id = permContainer.getAttachment().getUUID();

            getServer().getPlayer(id).ifPresent((player -> {
                PermissionObject permission = permContainer.getPermission();

                switch (permission.getCriteria()) {
                    case TRUE:
                        permContainer.setResult(!player.hasPermission("!" + permission.getPermission()));
                        break;
                    case FALSE:
                    case OP:
                    default:
                        permContainer.setResult(player.hasPermission(permission.getPermission()));
                }
            }));
        };
        Consumer<OpContainer> opContainer = pContainer -> {
            UUID id = pContainer.getAttachment().getUUID();

            getServer().getPlayer(id).ifPresent((player) -> pContainer.setResult(player.hasPermission("*") || player.hasPermission("'*'")));
        };
        BiConsumer<String, byte[]> onDataSend = DataSender::sendModule;

        try {
            JarManager.changeField(ModulePlayer.class, "onChat", onMessage);
            JarManager.changeField(ModulePlayer.class, "onBar", onActionBar);
            JarManager.changeField(ModulePlayer.class, "onTitle", onTitle);
            JarManager.changeField(ModulePlayer.class, "onKick", onKick);
            JarManager.changeField(ModulePlayer.class, "onLogin", onLogin);
            JarManager.changeField(ModulePlayer.class, "onClose", onClose);
            JarManager.changeField(ModulePlayer.class, "hasPermission", hasPermission);
            JarManager.changeField(ModulePlayer.class, "opContainer", opContainer);

            //JarManager.changeField(ModuleMessageService.class, "onDataSent", onDataSend);
        } catch (Throwable ignored) {
        }

        LockLogin.logger.scheduleLog(Level.OK, "LockLogin initialized and all its dependencies has been loaded");

        File[] moduleFiles = LockLogin.getLoader().getDataFolder().listFiles();
        if (moduleFiles != null) {
            List<File> files = Arrays.asList(moduleFiles);
            Iterator<File> iterator = files.iterator();
            do {
                File file = iterator.next();
                LockLogin.getLoader().loadModule(file, LoadRule.PREPLUGIN);
            } while (iterator.hasNext());
        }

        Event event = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.LOAD, null);
        ModulePlugin.callEvent(event);

        AllowedCommand.scan();

        Manager.initialize();

        if (moduleFiles != null) {
            List<File> files = Arrays.asList(moduleFiles);
            Iterator<File> iterator = files.iterator();
            do {
                File file = iterator.next();
                LockLogin.getLoader().loadModule(file, LoadRule.POSTPLUGIN);
            } while (iterator.hasNext());
        }
    }

    @Override
    public void disable() {
        Event event = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.UNLOAD, null);
        ModulePlugin.callEvent(event);
        File[] moduleFiles = LockLogin.getLoader().getDataFolder().listFiles();
        if (moduleFiles != null)
            for (File module : moduleFiles)
                LockLogin.getLoader().unloadModule(module);

        Manager.terminate();
        stopTasks();
    }

    public String name() {
        Optional<String> name = getContainer().getDescription().getName();
        return name.orElseGet(() -> FileInfo.getJarName(null));
    }

    public String version() {
        Optional<String> version = getContainer().getDescription().getVersion();
        return version.orElseGet(() -> FileInfo.getJarVersion(null));
    }

    public String description() {
        Optional<String> description = getContainer().getDescription().getDescription();
        return description.orElseGet(() -> FileInfo.getJarDescription(null));
    }

    public String[] authors() {
        List<String> authors = getContainer().getDescription().getAuthors();
        return authors.toArray(new String[0]);
    }

    @Override
    public String updateURL() {
        URL url = FileInfo.versionHost(null);
        if (url != null)
            return url.toString();

        return null;
    }
}
