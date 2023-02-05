package eu.locklogin.module.manager;

import eu.locklogin.api.common.session.SessionDataContainer;
import eu.locklogin.api.common.web.alert.Notification;
import eu.locklogin.api.common.web.alert.RemoteNotification;
import eu.locklogin.api.module.plugin.api.command.Command;
import eu.locklogin.api.module.plugin.javamodule.sender.ModuleConsole;
import eu.locklogin.api.module.plugin.javamodule.sender.ModuleSender;
import eu.locklogin.api.util.platform.CurrentPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ModuleInfoCommand extends Command implements LockLoginManager {

    /**
     * Initialize the module info command
     */
    public ModuleInfoCommand() {
        super("Shows information about the plugin", "info");
    }

    /**
     * Process the command when
     * its fired
     *
     * @param arg        the used argument
     * @param sender     the command sender
     * @param parameters the command parameters
     */
    @Override
    public void processCommand(final String arg, final ModuleSender sender, final String... parameters) {
        if (sender instanceof ModuleConsole) {
            module.async().queue("info_command", () -> {
                RemoteNotification rm = new RemoteNotification();
                if (parameters.length == 1) {
                    String param = parameters[0];
                    if (param.equalsIgnoreCase("--force-alert")) {
                        sender.sendMessage("&dFetching notifications, please wait...");

                        synchronized (Thread.currentThread()) {
                            AtomicBoolean completed = new AtomicBoolean(false);
                            rm.checkAlerts().whenComplete(() -> completed.set(true));

                            while (!completed.get()) {
                                try {
                                    Thread.currentThread().wait();
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                }
                Notification notification = rm.getNotification();

                sender.sendMessage("&d------------------------------");
                sender.sendMessage("");
                sender.sendMessage("&7LockLogin plugin information");
                sender.sendMessage("");
                sender.sendMessage("&bServer hash: &eu.c" + CurrentPlatform.getServerHash());
                sender.sendMessage("&bPanel status: &cMaintenance");
                sender.sendMessage("&bRegistered users: &eu.c" + SessionDataContainer.getRegistered());
                sender.sendMessage("&bLogged users: &eu.c" + SessionDataContainer.getLogged());
                sender.sendMessage("&bLast notification level: &eu.c" + notification.getLevel());
                sender.sendMessage("&bLast notification: &eu.c" + notification.getNotification());
                sender.sendMessage("&bForce configuration: &eu.c" + notification.forceConfig());
                sender.sendMessage("&bForce proxy configuration: &eu.c" + notification.forceProxy());
                sender.sendMessage("");
                sender.sendMessage("&d------------------------------");
            });
        } else {
            sender.sendMessage(CurrentPlatform.getMessages().prefix() + "&cThis command can be only run from console!");
        }
    }
}

