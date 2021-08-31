package eu.locklogin.plugin.bungee.listener;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.BruteForce;
import eu.locklogin.api.common.security.TokenGen;
import eu.locklogin.api.common.security.client.AccountData;
import eu.locklogin.api.common.security.client.ClientData;
import eu.locklogin.api.common.security.client.Name;
import eu.locklogin.api.common.security.client.ProxyCheck;
import eu.locklogin.api.common.session.SessionCheck;
import eu.locklogin.api.common.utils.DataType;
import eu.locklogin.api.common.utils.InstantParser;
import eu.locklogin.api.common.utils.other.UUIDGen;
import eu.locklogin.api.common.utils.plugin.FloodGateUtil;
import eu.locklogin.api.common.utils.plugin.ServerDataStorage;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.ProxyConfiguration;
import eu.locklogin.api.module.plugin.api.event.user.UserJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPostJoinEvent;
import eu.locklogin.api.module.plugin.api.event.user.UserPreJoinEvent;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bungee.permissibles.PluginPermission;
import eu.locklogin.plugin.bungee.plugin.sender.DataSender;
import eu.locklogin.plugin.bungee.util.files.Config;
import eu.locklogin.plugin.bungee.util.files.Proxy;
import eu.locklogin.plugin.bungee.util.files.client.OfflineClient;
import eu.locklogin.plugin.bungee.util.files.data.lock.LockedAccount;
import eu.locklogin.plugin.bungee.util.files.data.lock.LockedData;
import eu.locklogin.plugin.bungee.util.player.PlayerPool;
import eu.locklogin.plugin.bungee.util.player.User;
import ml.karmaconfigs.api.common.timer.SourceSecondsTimer;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.common.utils.enums.Level;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.locklogin.plugin.bungee.LockLogin.*;
import static eu.locklogin.plugin.bungee.plugin.sender.DataSender.*;

public final class JoinListener implements Listener {

    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    /**
     * Initialize the join listener
     */
    public JoinListener() {
        PlayerPool.whenValid((player) -> {
            PluginConfiguration config = CurrentPlatform.getConfiguration();
            InetSocketAddress ip = getSocketIp(player.getSocketAddress());
            User user = new User(player);

            Server server = player.getServer();
            if (server != null) {
                ServerInfo info = server.getInfo();
                ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

                if (ServerDataStorage.needsRegister(info.getName()) || ServerDataStorage.needsProxyKnowledge(info.getName())) {
                    if (ServerDataStorage.needsRegister(info.getName()))
                        DataSender.send(info, DataSender.getBuilder(DataType.KEY, ACCESS_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(info.getName()).build());

                    if (ServerDataStorage.needsProxyKnowledge(info.getName())) {
                        DataSender.send(info, DataSender.getBuilder(DataType.REGISTER, ACCESS_CHANNEL, player)
                                .addTextData(proxy.proxyKey()).addTextData(info.getName())
                                .addTextData(TokenGen.expiration("LOCAL_TOKEN").toString())
                                .build());
                    }
                }

                DataSender.send(player, DataSender.getBuilder(DataType.MESSAGES, PLUGIN_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(info.getName()).addTextData(CurrentPlatform.getMessages().toString()).build());
                DataSender.send(player, DataSender.getBuilder(DataType.CONFIG, PLUGIN_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(info.getName()).addTextData(Config.manager.getConfiguration()).build());
            }

            CurrentPlatform.requestDataContainerUpdate();

            MessageData validation = getBuilder(DataType.VALIDATION, DataSender.CHANNEL_PLAYER, player).build();
            DataSender.send(player, validation);

            PluginMessages messages = CurrentPlatform.getMessages();

            ProxyCheck proxy = new ProxyCheck(ip);
            if (proxy.isProxy()) {
                user.kick(messages.ipProxyError());
                return;
            }

            user.applySessionEffects();

            if (config.clearChat()) {
                for (int i = 0; i < 150; i++)
                    plugin.getProxy().getScheduler().runAsync(plugin, () -> player.sendMessage(TextComponent.fromLegacyText("")));
            }

            ClientSession session = user.getSession();
            session.validate();

            if (!config.captchaOptions().isEnabled())
                session.setCaptchaLogged(true);

            SimpleScheduler tmp_timer = null;
            if (!session.isCaptchaLogged()) {
                tmp_timer = new SourceSecondsTimer(plugin, 1, true);
                tmp_timer.secondChangeAction((second) -> player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(StringUtils.toColor(messages.captcha(session.getCaptcha()))))).start();
            }

            MessageData join = DataSender.getBuilder(DataType.JOIN, CHANNEL_PLAYER, player)
                    .addBoolData(session.isLogged())
                    .addBoolData(session.is2FALogged())
                    .addBoolData(session.isPinLogged())
                    .addBoolData(user.isRegistered()).build();
            DataSender.send(player, join);

            SimpleScheduler timer = tmp_timer;
            SessionCheck<ProxiedPlayer> check = user.getChecker().whenComplete(() -> {
                user.restorePotionEffects();
                player.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));

                if (timer != null)
                    timer.cancel();
            });

            plugin.getProxy().getScheduler().runAsync(plugin, check);

            DataSender.send(player, DataSender.getBuilder(DataType.CAPTCHA, DataSender.CHANNEL_PLAYER, player).build());

            if (!Proxy.isAuth(player.getServer().getInfo())) {
                user.checkServer(0);
            }

            UserPostJoinEvent event = new UserPostJoinEvent(fromPlayer(player), null);
            ModulePlugin.callEvent(event);

            if (event.isHandled()) {
                user.kick(event.getHandleReason());
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onServerPing(ProxyPingEvent e) {
        ClientData client = new ClientData(getIp(e.getConnection().getSocketAddress()));
        if (!client.isVerified())
            client.setVerified(true);
    }

    @SuppressWarnings("all")
    @EventHandler(priority = EventPriority.LOWEST)
    public final void onPreLogin(PreLoginEvent e) {
        PluginMessages messages = CurrentPlatform.getMessages();
        InetAddress ip = getIp(e.getConnection().getSocketAddress());

        String conn_name = e.getConnection().getName();
        UUID tar_uuid = UUIDGen.getUUID(conn_name);

        String address = "null";
        try {
            address = ip.getHostAddress();
        } catch (Throwable ignored) {
        }
        if (!e.isCancelled()) {
            try {
                if (validateIP(ip)) {
                    PluginConfiguration config = CurrentPlatform.getConfiguration();

                    if (config.registerOptions().maxAccounts() > 0) {
                        AccountData data = new AccountData(ip, AccountID.fromUUID(tar_uuid));

                        if (data.allow(config.registerOptions().maxAccounts())) {
                            data.save();

                            int amount = data.getAlts().size();
                            if (amount > 2) {
                                for (ProxiedPlayer online : plugin.getProxy().getPlayers()) {
                                    User user = new User(online);

                                    if (user.hasPermission(PluginPermission.altInfo())) {
                                        user.send(messages.prefix() + messages.altFound(conn_name, amount - 1));
                                    }
                                }

                                if (!messages.altFound(conn_name, amount).replaceAll("\\s", "").isEmpty())
                                    console.send(messages.prefix() + messages.altFound(conn_name, amount - 1));
                            }
                        } else {
                            e.setCancelled(true);
                            e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.maxRegisters())));
                            return;
                        }
                    }

                    if (config.bruteForceOptions().getMaxTries() > 0) {
                        BruteForce protection = new BruteForce(ip);
                        if (protection.isBlocked()) {
                            e.setCancelled(true);
                            e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.ipBlocked(protection.getBlockLeft()))));
                            return;
                        }
                    }

                    if (config.antiBot()) {
                        ClientData client = new ClientData(ip);
                        if (client.isVerified()) {
                            String name = e.getConnection().getName();

                            if (client.canAssign(config.accountsPerIP(), name, tar_uuid)) {
                                logger.scheduleLog(Level.INFO, "Assigned IP address {0} to client {1}", ip.getHostAddress(), name);
                            } else {
                                e.setCancelled(true);
                                e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.maxIP())));
                                return;
                            }
                        } else {
                            e.setCancelled(true);
                            e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.antiBot())));
                            return;
                        }
                    }

                    if (config.checkNames()) {
                        Name name = new Name(conn_name);
                        name.check();

                        if (name.notValid()) {
                            e.setCancelled(true);
                            e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.illegalName(name.getInvalidChars()))));
                            return;
                        }
                    }

                    OfflineClient offline = new OfflineClient(conn_name);
                    AccountManager manager = offline.getAccount();

                    if (manager != null) {
                        if (config.enforceNameCheck()) {
                            if (!manager.getName().equals(conn_name)) {
                                e.setCancelled(true);
                                e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.similarName(manager.getName()))));
                                return;
                            }
                        }

                        LockedAccount account = new LockedAccount(manager.getUUID());
                        LockedData data = account.getData();

                        if (data.isLocked()) {
                            String administrator = data.getAdministrator();
                            Instant date = data.getLockDate();
                            InstantParser parser = new InstantParser(date);
                            String dateString = parser.getDay() + " " + parser.getMonth() + " " + parser.getYear();

                            e.setCancelled(true);
                            e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.forcedAccountRemoval(administrator + " [ " + dateString + " ]"))));
                            logger.scheduleLog(Level.WARNING, "Client {0} tried to join, but his account was blocked by {1} on {2}", conn_name, administrator, dateString);
                            return;
                        }
                    }

                    UserPreJoinEvent event = new UserPreJoinEvent(getIp(e.getConnection().getSocketAddress()), tar_uuid, e.getConnection().getName(), e);
                    ModulePlugin.callEvent(event);

                    if (event.isHandled()) {
                        e.setCancelled(true);
                        e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(event.getHandleReason())));
                        return;
                    }

                    PlayerPool.addPlayer(tar_uuid);
                } else {
                    e.setCancelled(true);
                    e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.ipProxyError())));
                    logger.scheduleLog(Level.GRAVE, "Player {0}[{2}] tried to join with an invalid IP address ( {1} ), his connection got rejected with ip is proxy message", conn_name, address, e.getConnection().getUniqueId());
                }
            } catch (Throwable ex) {
                logger.scheduleLog(Level.GRAVE, ex);
                e.setCancelled(true);
                e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.ipProxyError())));
                logger.scheduleLog(Level.GRAVE, "Player {0}[{2}] tried to join with an invalid IP address ( {1} ), his connection got rejected with ip is proxy message", conn_name, address, e.getConnection().getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onLogin(LoginEvent e) {
        if (!e.isCancelled()) {
            PluginConfiguration config = CurrentPlatform.getConfiguration();
            PluginMessages messages = CurrentPlatform.getMessages();

            if (config.uuidValidator()) {
                UUID tar_uuid = e.getConnection().getUniqueId();
                UUID gen_uuid = UUIDGen.getUUID(e.getConnection().getName());

                if (!gen_uuid.equals(tar_uuid)) {
                    FloodGateUtil util = new FloodGateUtil(tar_uuid);
                    if (!util.isFloodClient()) {
                        e.setCancelled(true);
                        e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(messages.uuidFetchError())));
                        return;
                    }
                }
            }

            UserJoinEvent event = new UserJoinEvent(getIp(e.getConnection().getSocketAddress()), e.getConnection().getUniqueId(), e.getConnection().getName(), e);
            ModulePlugin.callEvent(event);

            if (event.isHandled()) {
                e.setCancelled(true);
                e.setCancelReason(TextComponent.fromLegacyText(StringUtils.toColor(event.getHandleReason())));
                PlayerPool.delPlayer(e.getConnection().getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public final void onSwitch(ServerSwitchEvent e) {
        ProxiedPlayer player = e.getPlayer();
        User user = new User(player);

        Server server = player.getServer();
        if (server != null) {
            ServerInfo info = server.getInfo();
            ProxyConfiguration proxy = CurrentPlatform.getProxyConfiguration();

            if (ServerDataStorage.needsRegister(info.getName()) || ServerDataStorage.needsProxyKnowledge(info.getName())) {
                if (ServerDataStorage.needsRegister(info.getName()))
                    DataSender.send(info, DataSender.getBuilder(DataType.KEY, ACCESS_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(info.getName()).build());

                if (ServerDataStorage.needsProxyKnowledge(info.getName())) {
                    DataSender.send(info, DataSender.getBuilder(DataType.REGISTER, ACCESS_CHANNEL, player)
                            .addTextData(proxy.proxyKey()).addTextData(info.getName())
                            .addTextData(TokenGen.expiration("LOCAL_TOKEN").toString())
                            .build());
                }
            }

            DataSender.send(player, DataSender.getBuilder(DataType.MESSAGES, PLUGIN_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(info.getName()).addTextData(CurrentPlatform.getMessages().toString()).build());
            DataSender.send(player, DataSender.getBuilder(DataType.CONFIG, PLUGIN_CHANNEL, player).addTextData(proxy.proxyKey()).addTextData(info.getName()).addTextData(Config.manager.getConfiguration()).build());
        }

        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            MessageData validation = getBuilder(DataType.VALIDATION, DataSender.CHANNEL_PLAYER, player).build();
            DataSender.send(player, validation);

            CurrentPlatform.requestDataContainerUpdate();

            ClientSession session = user.getSession();
            session.validate();

            MessageData join = DataSender.getBuilder(DataType.JOIN, CHANNEL_PLAYER, player)
                    .addBoolData(session.isLogged())
                    .addBoolData(session.is2FALogged())
                    .addBoolData(session.isPinLogged())
                    .addBoolData(user.isRegistered()).build();
            DataSender.send(player, join);

            DataSender.send(player, DataSender.getBuilder(DataType.CAPTCHA, DataSender.CHANNEL_PLAYER, player).build());

            if (!user.hasPermission(PluginPermission.limbo()))
                user.checkServer(0);
        }, (long) 1.5, TimeUnit.SECONDS);
    }

    /**
     * Check if the ip is valid
     *
     * @param ip the ip address
     * @return if the ip is valid
     */
    private boolean validateIP(final InetAddress ip) {
        PluginConfiguration config = CurrentPlatform.getConfiguration();

        if (config.ipHealthCheck()) {
            if (StringUtils.isNullOrEmpty(ip.getHostAddress())) {
                return false;
            }

            Matcher matcher = IPv4_PATTERN.matcher(ip.getHostAddress());
            return matcher.matches();
        }

        return true;
    }
}
