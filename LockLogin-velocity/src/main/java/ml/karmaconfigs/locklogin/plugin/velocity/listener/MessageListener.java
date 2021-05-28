package ml.karmaconfigs.locklogin.plugin.velocity.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import ml.karmaconfigs.api.common.Level;
import ml.karmaconfigs.api.common.utils.StringUtils;
import ml.karmaconfigs.api.velocity.Console;
import ml.karmaconfigs.locklogin.api.account.AccountManager;
import ml.karmaconfigs.locklogin.api.account.ClientSession;
import ml.karmaconfigs.locklogin.api.encryption.CryptoUtil;
import ml.karmaconfigs.locklogin.api.modules.api.channel.ModuleMessageService;
import ml.karmaconfigs.locklogin.api.modules.util.client.ModulePlayer;
import ml.karmaconfigs.locklogin.api.modules.util.javamodule.JavaModuleManager;
import ml.karmaconfigs.locklogin.api.modules.api.event.user.UserAuthenticateEvent;
import ml.karmaconfigs.locklogin.plugin.common.session.SessionDataContainer;
import ml.karmaconfigs.locklogin.plugin.common.utils.DataType;
import ml.karmaconfigs.locklogin.plugin.common.utils.plugin.ServerDataStorager;
import ml.karmaconfigs.locklogin.plugin.velocity.plugin.sender.DataSender;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.Proxy;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.client.PlayerFile;
import ml.karmaconfigs.locklogin.plugin.velocity.util.files.messages.Message;
import ml.karmaconfigs.locklogin.plugin.velocity.util.player.User;

import java.util.Optional;
import java.util.UUID;

import static ml.karmaconfigs.locklogin.plugin.velocity.LockLogin.*;

@SuppressWarnings("UnstableApiUsage")
public final class MessageListener {

    @Subscribe(order = PostOrder.FIRST)
    public final void onMessageReceive(PluginMessageEvent e) {
        if (e.getResult().isAllowed()) {
            Message messages = new Message();
            Proxy proxy = new Proxy();

            ByteArrayDataInput input = ByteStreams.newDataInput(e.getData());

            DataType sub = DataType.valueOf(input.readUTF().toUpperCase());
            String id = input.readUTF();

            switch (e.getIdentifier().getId().toLowerCase()) {
                case "ll:account":
                    if (sub == DataType.PIN) {

                        UUID uuid = UUID.fromString(id);
                        String pin = input.readUTF();

                        Optional<Player> tmp_player = server.getPlayer(uuid);
                        if (tmp_player.isPresent() && tmp_player.get().isActive()) {
                            Player player = tmp_player.get();
                            User user = new User(player);
                            ClientSession session = user.getSession();
                            AccountManager manager = user.getManager();

                            if (session.isValid()) {
                                if (manager.getPin().replaceAll("\\s", "").isEmpty()) {
                                    DataSender.send(player, DataSender.getBuilder(DataType.PIN, DataSender.CHANNEL_PLAYER, player).addTextData("close").build());

                                    UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                            (manager.has2FA() ? UserAuthenticateEvent.Result.SUCCESS_TEMP : UserAuthenticateEvent.Result.SUCCESS), fromPlayer(player),
                                            (manager.has2FA() ? messages.gAuthInstructions() : messages.logged()), null);
                                    JavaModuleManager.callEvent(event);

                                    user.send(messages.prefix() + event.getAuthMessage());
                                    session.setPinLogged(true);
                                } else {
                                    if (CryptoUtil.getBuilder().withPassword(pin).withToken(manager.getPin()).build().validate() || pin.equalsIgnoreCase("error")) {
                                        DataSender.send(player, DataSender.getBuilder(DataType.PIN, DataSender.CHANNEL_PLAYER, player).addTextData("close").build());

                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN,
                                                (manager.has2FA() ? UserAuthenticateEvent.Result.SUCCESS_TEMP : UserAuthenticateEvent.Result.SUCCESS), fromPlayer(player),
                                                (manager.has2FA() ? messages.gAuthInstructions() : messages.logged()), null);
                                        JavaModuleManager.callEvent(event);

                                        user.send(messages.prefix() + event.getAuthMessage());
                                        session.setPinLogged(true);

                                        if (!manager.has2FA())
                                            SessionDataContainer.setLogged(SessionDataContainer.getLogged() + 1);
                                    } else {
                                        DataSender.send(player, DataSender.getBuilder(DataType.PIN, DataSender.CHANNEL_PLAYER, player).addTextData("open").build());

                                        UserAuthenticateEvent event = new UserAuthenticateEvent(UserAuthenticateEvent.AuthType.PIN, UserAuthenticateEvent.Result.FAILED, fromPlayer(player), "", null);
                                        JavaModuleManager.callEvent(event);

                                        user.send(messages.prefix() + event.getAuthMessage());
                                    }
                                }
                            }
                        }
                    }
                    break;
                case "ll:plugin":
                    switch (sub) {
                        case PLAYER:
                            if (!id.equalsIgnoreCase(proxy.getProxyID().toString())) {
                                ModulePlayer modulePlayer = StringUtils.loadUnsafe(input.readUTF());
                                if (modulePlayer != null) {
                                    AccountManager manager = modulePlayer.getAccount();

                                    if (manager != null) {
                                        AccountManager newManager = new PlayerFile(manager.getUUID());

                                        if (!newManager.exists())
                                            newManager.create();

                                        newManager.setName(manager.getName());
                                        newManager.setPassword(manager.getPassword());
                                        newManager.setPin(manager.getPin());
                                        newManager.set2FA(manager.has2FA());
                                        newManager.setGAuth(manager.getGAuth());
                                    }
                                }
                            }
                            break;
                        case MODULE:
                            int byteLength = input.readInt();
                            byte[] bytes = new byte[byteLength];

                            int i = 0;
                            while (i < byteLength) {
                                bytes[i] = input.readByte();
                                i++;
                            }

                            ModuleMessageService.listenMessage(id, bytes);
                            break;
                    }
                case "ll:access":
                    String name = input.readUTF();

                    switch (sub) {
                        case KEY:
                            if (!id.equalsIgnoreCase("invalid")) {
                                //As the key should be global, when a proxy registers a key, all the proxies should know that
                                Console.send(plugin, "Registered proxy key into server {0}", Level.INFO, name);
                                ServerDataStorager.setKeyRegistered(name);
                            } else {
                                Console.send(plugin, "Failed to set proxy key in {0}", Level.GRAVE, name);
                                e.setResult(PluginMessageEvent.ForwardResult.handled());
                            }
                            break;
                        case REGISTER:
                            if (!id.equalsIgnoreCase("invalid")) {
                                //Only listen if the proxy id is this one
                                if (proxy.getProxyID().toString().equalsIgnoreCase(id)) {
                                    Console.send(plugin, "Registered this proxy into server {0}", Level.INFO, name);
                                    ServerDataStorager.setProxyRegistered(name);
                                } else {
                                    e.setResult(PluginMessageEvent.ForwardResult.handled());
                                }
                            } else {
                                Console.send(plugin, "Failed to register this proxy in {0}", Level.GRAVE, name);
                                e.setResult(PluginMessageEvent.ForwardResult.handled());
                            }
                            break;
                        case REMOVE:
                            if (!id.equalsIgnoreCase("invalid")) {
                                //Only listen if the proxy id is this one
                                if (proxy.getProxyID().toString().equalsIgnoreCase(id)) {
                                    Console.send(plugin, "Removed ths proxy from server {0}", Level.INFO, name);
                                    ServerDataStorager.removeProxyRegistered(name);
                                } else {
                                    ServerDataStorager.removeKeyRegistered(name);
                                    e.setResult(PluginMessageEvent.ForwardResult.handled());
                                }
                            } else {
                                Console.send(plugin, "Failed to remove this proxy from server {0}", Level.GRAVE, name);
                                e.setResult(PluginMessageEvent.ForwardResult.handled());
                            }
                            break;
                    }
                    break;
            }
        }
    }
}
