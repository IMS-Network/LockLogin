package eu.locklogin.plugin.bukkit.command;

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

import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.common.security.Password;
import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.api.event.user.AccountCreatedEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.TaskTarget;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

@SystemCommand(command = "register", aliases = {"reg"})
public final class RegisterCommand implements CommandExecutor {

    private final static PluginConfiguration config = CurrentPlatform.getConfiguration();
    private final static PluginMessages messages = CurrentPlatform.getMessages();

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            tryAsync(TaskTarget.COMMAND_EXECUTE, () -> {
                Player player = (Player) sender;
                User user = new User(player);

                ClientSession session = user.getSession();
                if (session.isValid()) {
                    if (!session.isLogged()) {
                        AccountManager manager = user.getManager();
                        if (!manager.exists()) {
                            if (manager.create()) {
                                logger.scheduleLog(Level.INFO, "Created account of player {0}", StringUtils.stripColor(player.getDisplayName()));
                            } else {
                                logger.scheduleLog(Level.GRAVE, "Couldn't create account of player {0}", StringUtils.stripColor(player.getDisplayName()));

                                user.send(messages.prefix() + properties.getProperty("could_not_create_user", "&5&oWe're sorry, but we couldn't create your account"));
                                return;
                            }
                        }

                        if (manager.isRegistered()) {
                            user.send(messages.alreadyRegistered());
                        } else {
                            switch (args.length) {
                                case 2:
                                    if (session.isCaptchaLogged()) {
                                        String password = args[0];
                                        String confirmation = args[1];

                                        if (password.equals(confirmation)) {
                                            Password checker = new Password(password);
                                            checker.addInsecure(player.getDisplayName(), player.getName(), StringUtils.stripColor(player.getDisplayName()), StringUtils.stripColor(player.getName()));

                                            if (!checker.isSecure()) {
                                                user.send(messages.prefix() + messages.passwordInsecure());

                                                if (config.blockUnsafePasswords()) {
                                                    return;
                                                }
                                            }

                                            manager.setPassword(password);

                                            user.send(messages.prefix() + messages.registered());

                                            session.setLogged(true);

                                            if (!manager.has2FA() && config.enable2FA() && user.hasPermission(PluginPermissions.force_2fa())) {
                                                trySync(TaskTarget.COMMAND_FORCE, () -> player.performCommand("2fa setup " + password));
                                            } else {
                                                session.set2FALogged(true);
                                            }
                                            if (!manager.hasPin())
                                                session.setPinLogged(true);

                                            Event event = new AccountCreatedEvent(user.getModule(), null);
                                            ModulePlugin.callEvent(event);

                                            if (!config.useVirtualID() && player.hasPermission("locklogin.account")) {
                                                user.send("&cIMPORTANT!", "&7Virtual ID is disabled!", 0, 10, 0);
                                                user.send(messages.prefix() + "&dVirtual ID is disabled, this can be a security risk for everyone. Enable it in config (VirtualID: true) to dismiss this message. &5&lTHIS MESSAGE CAN BE ONLY SEEN BY ADMINISTRATORS");
                                            }
                                        } else {
                                            user.send(messages.prefix() + messages.registerError());
                                        }
                                    } else {
                                        if (config.captchaOptions().isEnabled()) {
                                            user.send(messages.prefix() + messages.invalidCaptcha());
                                        }
                                    }
                                    break;
                                case 3:
                                    if (session.isCaptchaLogged()) {
                                        user.send(messages.prefix() + messages.register());
                                    } else {
                                        String password = args[0];
                                        String confirmation = args[1];
                                        String captcha = args[2];

                                        if (session.getCaptcha().equals(captcha)) {
                                            session.setCaptchaLogged(true);

                                            trySync(TaskTarget.COMMAND_FORCE, () -> player.performCommand("register " + password + " " + confirmation));
                                        } else {
                                            user.send(messages.prefix() + messages.invalidCaptcha());
                                        }
                                    }
                                    break;
                                default:
                                    if (!session.isLogged()) {
                                        user.send(messages.prefix() + messages.register());
                                    } else {
                                        if (session.isTempLogged()) {
                                            user.send(messages.prefix() + messages.gAuthenticate());
                                        } else {
                                            user.send(messages.prefix() + messages.alreadyRegistered());
                                        }
                                    }
                                    break;
                            }
                        }
                    } else {
                        if (session.isTempLogged()) {
                            user.send(messages.prefix() + messages.alreadyRegistered());
                        } else {
                            user.send(messages.prefix() + messages.gAuthenticate());
                        }
                    }
                } else {
                    user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
                }
            });
        } else {
            console.send(messages.prefix() + properties.getProperty("console_is_restricted", "&5&oFor security reasons, this command is restricted to players only"));
        }

        return false;
    }
}
