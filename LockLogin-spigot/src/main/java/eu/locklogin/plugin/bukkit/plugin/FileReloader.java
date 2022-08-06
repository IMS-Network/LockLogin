package eu.locklogin.plugin.bukkit.plugin;

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

import eu.locklogin.api.file.PluginConfiguration;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.plugin.api.command.help.HelpPage;
import eu.locklogin.api.module.plugin.api.event.plugin.PluginStatusChangeEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.util.files.Config;
import eu.locklogin.plugin.bukkit.util.inventory.object.Button;
import eu.locklogin.plugin.bukkit.util.player.User;
import org.bukkit.entity.Player;

import static eu.locklogin.plugin.bukkit.LockLogin.console;
import static eu.locklogin.plugin.bukkit.LockLogin.properties;

public class FileReloader {

    /**
     * Reload the plugin files and other
     *
     * @param player the player that called the action
     */
    public static void reload(final Player player) {
        Event reload_start = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.RELOAD_START, null);
        ModulePlugin.callEvent(reload_start);

        PluginMessages messages = CurrentPlatform.getMessages();
        if (player != null) {
            User user = new User(player);

            if (user.hasPermission(PluginPermissions.reload_config()) || user.hasPermission(PluginPermissions.reload_messages())) {
                Button.reload();

                if (user.hasPermission(PluginPermissions.reload_config())) {
                    if (Config.manager.reload()) {
                        Config.manager.checkValues();

                        user.send(messages.prefix() + properties.getProperty("reload_config", "&dReloaded config file"));

                        PluginConfiguration config = CurrentPlatform.getConfiguration();
                        CurrentPlatform.setConfigManager(new Config());
                        CurrentPlatform.setPrefix(config.getModulePrefix());
                    }
                }
                if (user.hasPermission(PluginPermissions.reload_messages())) {
                    if (CurrentPlatform.getMessages().reload()) {
                        user.send(messages.prefix() + properties.getProperty("reload_messages", "&dReloaded messages file"));
                    }
                }

                if (user.hasPermission(PluginPermissions.reload())) {
                    user.send(messages.prefix() + properties.getProperty("restart_systems", "&7Restarting version checker and plugin alert systems"));

                    Manager.restartVersionChecker();
                    Manager.restartAlertSystem();
                }
            } else {
                user.send(messages.prefix() + messages.permissionError(PluginPermissions.reload()));
            }
        } else {
            Button.reload();

            if (Config.manager.reload()) {
                Config.manager.checkValues();

                console.send(messages.prefix() + properties.getProperty("reload_config", "&dReloaded config file"));

                PluginConfiguration config = CurrentPlatform.getConfiguration();
                CurrentPlatform.setPrefix(config.getModulePrefix());
            }
            if (CurrentPlatform.getMessages().reload()) {
                console.send(messages.prefix() + properties.getProperty("reload_messages", "&adReloaded messages file"));
            }

            console.send(messages.prefix() + properties.getProperty("restart_systems", "&7Restarting version checker and plugin alert systems"));

            Manager.restartVersionChecker();
            Manager.restartAlertSystem();

            HelpPage.updatePagesPrefix();
        }

        Event reload_finish = new PluginStatusChangeEvent(PluginStatusChangeEvent.Status.RELOAD_END, null);
        ModulePlugin.callEvent(reload_finish);
    }
}