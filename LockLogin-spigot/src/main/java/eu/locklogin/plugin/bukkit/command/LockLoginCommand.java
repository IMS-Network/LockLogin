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

import eu.locklogin.api.common.utils.plugin.ComponentFactory;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.module.PluginModule;
import eu.locklogin.api.module.plugin.api.event.plugin.UpdateRequestEvent;
import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import eu.locklogin.api.module.plugin.javamodule.ModulePlugin;
import eu.locklogin.api.module.plugin.javamodule.updater.JavaModuleVersion;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.TaskTarget;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.plugin.FileReloader;
import eu.locklogin.plugin.bukkit.plugin.Manager;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.enums.Level;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.api.common.version.VersionUpdater;
import ml.karmaconfigs.api.common.version.util.VersionType;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

@SystemCommand(command = "locklogin", bungee_command = "slocklogin", bungeecord = true)
public final class LockLoginCommand implements CommandExecutor {

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
        PluginMessages messages = CurrentPlatform.getMessages();
        sender.sendMessage(StringUtils.toColor(messages.prefix() + properties.
                getProperty(
                        "processing_async",
                        "&dProcessing {0} command, please wait for feedback")
                .replace("{0}", label)));

        tryAsync(TaskTarget.COMMAND_EXECUTE, () -> {
            VersionUpdater updater = Manager.getUpdater();
            if (sender instanceof Player) {
                Player player = (Player) sender;
                User user = new User(player);

                switch (args.length) {
                    case 1:
                        switch (args[0].toLowerCase()) {
                            case "log":
                                if (user.hasPermission(PluginPermissions.web_log())) {
                                    user.send(messages.prefix() + "&cFeature temporally disabled");
                                } else {
                                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.web_log()));
                                }
                                break;
                            case "reload":
                                FileReloader.reload(player); //Permission check is made internally... xD
                                break;
                            case "applyupdates":
                                if (user.hasPermission(PluginPermissions.updater_apply())) {
                                    for (Player online : plugin.getServer().getOnlinePlayers()) {
                                        if (!online.equals(sender)) {
                                            User u = new User(online);
                                            if (u.hasPermission(PluginPermissions.updater_apply())) {
                                                u.send(messages.prefix() + "&7" + StringUtils.stripColor(sender.getName()) + "&d is updating LockLogin");
                                            }
                                        }
                                    }

                                    plugin.console().send("{0} is updating LockLogin", Level.INFO, StringUtils.toColor(player.getName()));

                                    Event event = new UpdateRequestEvent(sender, null);
                                    ModulePlugin.callEvent(event);
                                } else {
                                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.updater_apply()));
                                }
                                break;
                            case "modules":
                                if (user.hasPermission(PluginPermissions.module_list())) {
                                    user.send(messages.prefix() + "&dFetching modules info, please stand by");

                                    Set<PluginModule> modules = ModuleLoader.getModules();
                                    AtomicBoolean canPost = new AtomicBoolean(false);
                                    AtomicInteger id = new AtomicInteger(0);
                                    ComponentFactory main = new ComponentFactory("&3Modules &8&o( &a" + modules.size() + " &8&o)&7: ");

                                    for (PluginModule module : modules) {
                                        JavaModuleVersion version = module.getUpdater();
                                        version.fetch().whenComplete((result) -> {
                                            ComponentFactory factory = new ComponentFactory("&e" + StringUtils.stripColor(module.name()) + (id.getAndIncrement() == modules.size() ? "" : "&7, "));

                                            String hoverText = "\n&7Owner(s): &e" + module.singleAuthors() + "\n&7Version: &e" + module.version() + "\n&7Description: &e" + module.description();

                                            if (result.isUpdated()) {
                                                hoverText = hoverText + "\n&7Latest: &e" + result.getLatest() + "\n\n&7You are using the latest known module version!";
                                            } else {
                                                hoverText = hoverText + "\n&7Latest: &e" + result.getLatest() + "\n\n&7Click me to download the latest version!";
                                                factory.click(ClickEvent.Action.OPEN_URL, result.getUpdateURL());
                                            }

                                            factory.hover(hoverText);

                                            main.addExtra(factory);

                                            if (id.get() == modules.size()) {
                                                canPost.set(true);
                                            }
                                        });
                                    }

                                    SimpleScheduler timer = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true).multiThreading(true);
                                    timer.restartAction(() -> {
                                        if (canPost.get()) {
                                            timer.cancel();
                                            user.send(main.get());
                                        }
                                    }).start();
                                } else {
                                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.module_list()));
                                }
                                break;
                            case "version":
                                if (user.hasPermission(PluginPermissions.updater_version())) {
                                    user.send(messages.prefix() + "&dTrying to communicate with LockLogin website, please wait. This could take some seconds...");

                                    updater.get().whenComplete((result, error) -> {
                                        if (error == null) {
                                            user.send(messages.prefix() + "&7Current version:&e " + result.resolve(VersionType.CURRENT));
                                            user.send(messages.prefix() + "&7Latest version:&e " + result.resolve(VersionType.LATEST));
                                        } else {
                                            user.send(messages.prefix() + "&5&oFailed to fetch latest version");
                                        }
                                    });
                                } else {
                                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.updater_version()));
                                }
                                break;
                            case "changelog":
                                if (user.hasPermission(PluginPermissions.updater_changelog())) {
                                    user.send(messages.prefix() + "&dTrying to communicate with LockLogin website, please wait. This could take some seconds...");

                                    updater.get().whenComplete((result, error) -> {
                                        if (error == null) {
                                            for (String str : result.getChangelog()) {
                                                user.send(str);
                                            }
                                        } else {
                                            user.send(messages.prefix() + "&5&oFailed to fetch latest changelog");
                                        }
                                    });
                                } else {
                                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.updater_changelog()));
                                }
                                break;
                            case "synchronize":
                            case "remove":
                            case "execute":
                                if (user.hasPermission(PluginPermissions.web())) {
                                    user.send(messages.prefix() + "&cComing soon; LockLogin web panel");
                                } else {
                                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.web()));
                                }
                                break;
                            case "check":
                                if (user.hasPermission(PluginPermissions.updater_check())) {
                                    user.send(messages.prefix() + "&dTrying to communicate with LockLogin website, please wait. This could take some seconds...");

                                    updater.fetch(true).whenComplete((result, error) -> {
                                        if (error == null) {
                                            user.send(messages.prefix() + "&dChecked for updates successfully");
                                        } else {
                                            user.send(messages.prefix() + "&5&oFailed to check for updates");
                                        }
                                    });
                                } else {
                                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.updater_check()));
                                }
                                break;
                            default:
                                user.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                                break;
                        }
                        break;
                    case 3:
                        if (args[0].equalsIgnoreCase("modules")) {
                            String moduleName = args[2];
                            File moduleFile = ModuleLoader.getModuleFile(moduleName);

                            if (moduleFile != null) {
                                PluginModule module = getLoader().getModule(moduleFile);

                                if (module != null) {
                                    switch (args[1].toLowerCase()) {
                                        case "load":
                                            if (user.hasPermission(PluginPermissions.module_load())) {
                                                if (!ModuleLoader.isLoaded(module) && module.load()) {
                                                    user.send(messages.prefix() + "&dModule " + moduleName + " has been loaded successfully");
                                                } else {
                                                    user.send(messages.prefix() + "&5&oModule " + moduleName + " failed to load, maybe is already loaded?");
                                                }
                                            } else {
                                                user.send(messages.prefix() + messages.permissionError(PluginPermissions.module_load()));
                                            }
                                            break;
                                        case "unload":
                                            if (user.hasPermission(PluginPermissions.module_unload())) {
                                                if (ModuleLoader.isLoaded(module) && module.unload()) {
                                                    user.send(messages.prefix() + "&dModule " + moduleName + " has been unloaded successfully");
                                                } else {
                                                    user.send(messages.prefix() + "&5&oModule " + moduleName + " failed to unload, maybe is not loaded?");
                                                }
                                            } else {
                                                user.send(messages.prefix() + messages.permissionError(PluginPermissions.module_unload()));
                                            }
                                            break;
                                        case "reload":
                                            if (user.hasPermission(PluginPermissions.module_reload())) {
                                                module.reload();
                                                user.send(messages.prefix() + "&dModule " + moduleName + " has been reloaded, check console for more info");
                                            } else {
                                                user.send(messages.prefix() + messages.permissionError(PluginPermissions.module_reload()));
                                            }
                                            break;
                                        default:
                                            user.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin modules &e<load>&7, &e<unload>&7, &e<reload>&7 &e[module name]");
                                            break;
                                    }
                                } else {
                                    user.send(messages.prefix() + "&5&oModule " + moduleName + " does not exist!");
                                }
                            } else {
                                user.send(messages.prefix() + "&5&oModule " + moduleName + " is not loaded or does not exist!");
                            }
                        } else {
                            user.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                        }
                        break;
                    default:
                        user.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                        break;
                }
            } else {
                switch (args.length) {
                    case 1:
                        switch (args[0].toLowerCase()) {
                            case "log":
                                plugin.console().send("Feature temporally disabled", Level.INFO);
                                break;
                            case "reload":
                                FileReloader.reload(null);
                                break;
                            case "applyupdates":
                                for (Player online : plugin.getServer().getOnlinePlayers()) {
                                    if (!online.equals(sender)) {
                                        User user = new User(online);
                                        if (user.hasPermission(PluginPermissions.updater_apply())) {
                                            user.send(messages.prefix() + "&7" + StringUtils.stripColor(sender.getName()) + "&d is updating LockLogin");
                                        }
                                    }
                                }

                                Event event = new UpdateRequestEvent(sender, null);
                                ModulePlugin.callEvent(event);

                                break;
                            case "modules":
                                console.send(messages.prefix() + "&dFetching modules info, please stand by");

                                Set<PluginModule> modules = ModuleLoader.getModules();
                                AtomicBoolean canPost = new AtomicBoolean(false);
                                AtomicInteger id = new AtomicInteger(0);
                                StringBuilder builder = new StringBuilder();

                                builder.append(StringUtils.formatString("&3Modules &8&o( &a{0} &8&o)&7: ", modules.size()));

                                for (PluginModule module : modules) {
                                    JavaModuleVersion version = module.getUpdater();

                                    version.fetch().whenComplete((result) -> {
                                        builder.append(StringUtils.formatString("\n\n&7Name: &d{0}", module.name()))
                                                .append(StringUtils.formatString("\n&7Authors: &d{0}", module.singleAuthors()))
                                                .append(StringUtils.formatString("\n&7Version: &d{0}", module.version()))
                                                .append(StringUtils.formatString("\n&7Description: &d{0}", module.description()));
                                        if (result.isUpdated()) {
                                            builder.append("\n&7Version status: &dUp to date");
                                        } else {
                                            builder.append("\n&7Version status: &5&oOut of date")
                                                    .append(StringUtils.formatString("\n&7Update url: &d{0}", result.getUpdateURL()));
                                        }

                                        if (id.incrementAndGet() != modules.size()) {
                                            builder.append("\n\n&e------------------------------------");
                                        } else {
                                            canPost.set(true);
                                        }
                                    });
                                }

                                SimpleScheduler timer = new SourceScheduler(plugin, 1, SchedulerUnit.SECOND, true).multiThreading(true);
                                timer.restartAction(() -> {
                                    if (canPost.get()) {
                                        timer.cancel();
                                        console.send(builder.toString());
                                    }
                                }).start();
                                break;
                            case "version":
                                console.send(messages.prefix() + "&dTrying to communicate with LockLogin website, please wait. This could take some seconds...");

                                updater.get().whenComplete((result, error) -> {
                                    if (error == null) {
                                        console.send(messages.prefix() + "&7Current version:&e {0}", result.resolve(VersionType.CURRENT));
                                        console.send(messages.prefix() + "&7Latest version:&e {0}", result.resolve(VersionType.LATEST));
                                    } else {
                                        console.send(messages.prefix() + "&5&oFailed to fetch latest version");
                                    }
                                });
                                break;
                            case "changelog":
                                console.send(messages.prefix() + "&dTrying to communicate with LockLogin website, please wait. This could take some seconds...");

                                updater.get().whenComplete((result, error) -> {
                                    if (error == null) {
                                        for (String str : result.getChangelog()) {
                                            console.send(str);
                                        }
                                    } else {
                                        console.send(messages.prefix() + "&5&oFailed to fetch latest changelog");
                                    }
                                });
                                break;
                            case "synchronize":
                            case "remove":
                            case "execute":
                                console.send(messages.prefix() + "&cComing soon; LockLogin web panel");
                                break;
                            case "check":
                                console.send(messages.prefix() + "&dTrying to communicate with LockLogin website, please wait. This could take some seconds...");

                                updater.fetch(true).whenComplete((result, error) -> {
                                    if (error == null) {
                                        console.send(messages.prefix() + "&dChecked for updates successfully");
                                    } else {
                                        console.send(messages.prefix() + "&5&oFailed to check for updates");
                                    }
                                });

                                break;
                            default:
                                console.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                                break;
                        }
                        break;
                    case 3:
                        if (args[0].equalsIgnoreCase("modules")) {
                            String moduleName = args[2];
                            File moduleFile = ModuleLoader.getModuleFile(moduleName);

                            if (moduleFile != null) {
                                PluginModule module = getLoader().getModule(moduleFile);

                                if (module != null) {
                                    switch (args[1].toLowerCase()) {
                                        case "load":
                                            if (!ModuleLoader.isLoaded(module) && module.load()) {
                                                console.send(messages.prefix() + "&dModule " + moduleName + " has been loaded successfully");
                                            } else {
                                                console.send(messages.prefix() + "&5&oModule " + moduleName + " failed to load, maybe is already loaded?");
                                            }

                                            break;
                                        case "unload":
                                            if (ModuleLoader.isLoaded(module) && module.unload()) {
                                                console.send(messages.prefix() + "&dModule " + moduleName + " has been unloaded successfully");
                                            } else {
                                                console.send(messages.prefix() + "&5&oModule " + moduleName + " failed to unload, maybe is not loaded?");
                                            }

                                            break;
                                        case "reload":
                                            module.reload();
                                            break;
                                        default:
                                            console.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin modules &e<load>&7, &e<unload>&7, &e<reload>&7 &e[module name]");

                                            break;
                                    }
                                } else {
                                    console.send(messages.prefix() + "&5&oModule " + moduleName + " does not exist!");
                                }
                            } else {
                                console.send(messages.prefix() + "&5&oModule " + moduleName + " is not loaded or does not exist!");
                            }
                        } else {
                            console.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                        }
                        break;
                    default:
                        console.send(messages.prefix() + "&5&oAvailable sub-commands:&7 /locklogin &e<reload>&7, &e<applyupdates>&7, &e<modules>&7, &e<version>&7, &e<changelog>&7, &e<check>");
                        break;
                }
            }
        });

        return false;
    }
}
