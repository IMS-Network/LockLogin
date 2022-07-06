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

import eu.locklogin.api.account.AccountID;
import eu.locklogin.api.account.AccountManager;
import eu.locklogin.api.account.ClientSession;
import eu.locklogin.api.file.PluginMessages;
import eu.locklogin.api.file.plugin.Alias;
import eu.locklogin.api.module.plugin.client.permission.plugin.PluginPermissions;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.plugin.bukkit.command.util.SystemCommand;
import eu.locklogin.plugin.bukkit.util.files.client.OfflineClient;
import eu.locklogin.plugin.bukkit.util.player.User;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static eu.locklogin.plugin.bukkit.LockLogin.*;

@SystemCommand(command = "alias")
public final class AliasCommand implements CommandExecutor {

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

        if (sender instanceof Player) {
            Player player = (Player) sender;
            User user = new User(player);
            ClientSession session = user.getSession();

            if (session.isValid()) {
                if (user.hasPermission(PluginPermissions.alias())) {
                    if (args.length >= 2) {
                        String sub = args[0];

                        String name = args[1];
                        Alias alias = new Alias(name);
                        switch (sub.toLowerCase()) {
                            case "create":
                                if (!alias.exists()) {
                                    alias.create();
                                    user.send(messages.prefix() + messages.aliasCreated(alias));
                                } else {
                                    user.send(messages.prefix() + messages.aliasExists(alias));
                                }
                                break;
                            case "destroy":
                                if (alias.exists()) {
                                    alias.destroy();
                                    user.send(messages.prefix() + messages.aliasDestroyed(alias));
                                } else {
                                    user.send(messages.prefix() + messages.aliasNotFound(name));
                                }
                                break;
                            case "add":
                                if (alias.exists()) {
                                    if (args.length >= 3) {
                                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                                            String[] names;
                                            if (args.length == 3) {
                                                names = new String[]{args[2]};
                                            } else {
                                                Set<String> set = new LinkedHashSet<>(Arrays.asList(args).subList(2, args.length));
                                                names = set.toArray(new String[]{});
                                            }

                                            String invalid = extract(names);
                                            if (!invalid.replaceAll("\\s", "").isEmpty())
                                                user.send(messages.prefix() + messages.neverPlayer(invalid));

                                            Map<AccountID, String> accounts = parse(names);

                                            Set<String> added = new LinkedHashSet<>(accounts.values());
                                            Set<String> not_added = alias.addUsers(accounts);

                                            if (!not_added.isEmpty()) {
                                                added.removeAll(not_added);
                                                user.send(messages.prefix() + messages.playerAlreadyIn(alias, not_added.toArray(new String[]{})));
                                            }
                                            if (!added.isEmpty()) {
                                                user.send(messages.prefix() + messages.addedPlayer(alias, added.toArray(new String[]{})));
                                            } else {
                                                if (not_added.isEmpty())
                                                    user.send(messages.prefix() + messages.addedPlayer(alias, "@nobody"));
                                            }
                                        });
                                    } else {
                                        user.send(messages.prefix() + messages.alias());
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.aliasNotFound(name));
                                }
                                break;
                            case "remove":
                                if (alias.exists()) {
                                    if (args.length >= 3) {
                                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                                            String[] names;
                                            if (args.length == 3) {
                                                names = new String[]{args[2]};
                                            } else {
                                                Set<String> set = new LinkedHashSet<>(Arrays.asList(args).subList(2, args.length));
                                                names = set.toArray(new String[]{});
                                            }

                                            String invalid = extract(names);
                                            if (!invalid.replaceAll("\\s", "").isEmpty())
                                                user.send(messages.prefix() + messages.neverPlayer(invalid));

                                            Map<AccountID, String> accounts = parse(names);

                                            Set<String> removed = new LinkedHashSet<>(accounts.values());
                                            Set<String> not_removed = alias.delUsers(accounts);

                                            if (!not_removed.isEmpty()) {
                                                removed.removeAll(not_removed);
                                                user.send(messages.prefix() + messages.playerNotIn(alias, not_removed.toArray(new String[]{})));
                                            }
                                            if (!removed.isEmpty()) {
                                                user.send(messages.prefix() + messages.removedPlayer(alias, removed.toArray(new String[]{})));
                                            } else {
                                                if (not_removed.isEmpty())
                                                    user.send(messages.prefix() + messages.removedPlayer(alias, "@nobody"));
                                            }
                                        });
                                    } else {
                                        user.send(messages.prefix() + messages.alias());
                                    }
                                } else {
                                    user.send(messages.prefix() + messages.aliasNotFound(name));
                                }
                                break;
                            default:
                                user.send(messages.prefix() + messages.alias());
                                break;
                        }
                    } else {
                        user.send(messages.prefix() + messages.alias());
                    }
                } else {
                    user.send(messages.prefix() + messages.permissionError(PluginPermissions.alias()));
                }
            } else {
                user.send(messages.prefix() + properties.getProperty("session_not_valid", "&5&oYour session is invalid, try leaving and joining the server again"));
            }
        } else {
            if (args.length >= 2) {
                String sub = args[0];

                String name = args[1];
                Alias alias = new Alias(name);
                switch (sub.toLowerCase()) {
                    case "create":
                        if (!alias.exists()) {
                            alias.create();
                            console.send(messages.prefix() + messages.aliasCreated(alias));
                        } else {
                            console.send(messages.prefix() + messages.aliasExists(alias));
                        }
                        break;
                    case "destroy":
                        if (alias.exists()) {
                            alias.destroy();
                            console.send(messages.prefix() + messages.aliasDestroyed(alias));
                        } else {
                            console.send(messages.prefix() + messages.aliasNotFound(name));
                        }
                        break;
                    case "add":
                        if (alias.exists()) {
                            if (args.length >= 3) {
                                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                                    String[] names;
                                    if (args.length == 3) {
                                        names = new String[]{args[2]};
                                    } else {
                                        Set<String> set = new LinkedHashSet<>(Arrays.asList(args).subList(2, args.length));
                                        names = set.toArray(new String[]{});
                                    }

                                    String invalid = extract(names);
                                    if (!invalid.replaceAll("\\s", "").isEmpty())
                                        console.send(messages.prefix() + messages.neverPlayer(invalid));

                                    Map<AccountID, String> accounts = parse(names);

                                    Set<String> added = new LinkedHashSet<>(accounts.values());
                                    Set<String> not_added = alias.addUsers(accounts);

                                    if (!not_added.isEmpty()) {
                                        added.removeAll(not_added);
                                        console.send(messages.prefix() + messages.playerAlreadyIn(alias, not_added.toArray(new String[]{})));
                                    }
                                    if (!added.isEmpty()) {
                                        console.send(messages.prefix() + messages.addedPlayer(alias, added.toArray(new String[]{})));
                                    } else {
                                        if (not_added.isEmpty())
                                            console.send(messages.prefix() + messages.addedPlayer(alias, "@nobody"));
                                    }
                                });
                            } else {
                                console.send(messages.prefix() + messages.alias());
                            }
                        } else {
                            console.send(messages.prefix() + messages.aliasNotFound(name));
                        }
                        break;
                    case "remove":
                        if (alias.exists()) {
                            if (args.length >= 3) {
                                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                                    String[] names;
                                    if (args.length == 3) {
                                        names = new String[]{args[2]};
                                    } else {
                                        Set<String> set = new LinkedHashSet<>(Arrays.asList(args).subList(2, args.length));
                                        names = set.toArray(new String[]{});
                                    }

                                    String invalid = extract(names);
                                    if (!invalid.replaceAll("\\s", "").isEmpty())
                                        console.send(messages.prefix() + messages.neverPlayer(invalid));

                                    Map<AccountID, String> accounts = parse(names);

                                    Set<String> removed = new LinkedHashSet<>(accounts.values());
                                    Set<String> not_removed = alias.delUsers(accounts);

                                    if (!not_removed.isEmpty()) {
                                        removed.removeAll(not_removed);
                                        console.send(messages.prefix() + messages.playerNotIn(alias, not_removed.toArray(new String[]{})));
                                    }
                                    if (!removed.isEmpty()) {
                                        console.send(messages.prefix() + messages.removedPlayer(alias, removed.toArray(new String[]{})));
                                    } else {
                                        if (not_removed.isEmpty())
                                            console.send(messages.prefix() + messages.removedPlayer(alias, "@nobody"));
                                    }
                                });
                            } else {
                                console.send(messages.prefix() + messages.alias());
                            }
                        } else {
                            console.send(messages.prefix() + messages.aliasNotFound(name));
                        }
                        break;
                    default:
                        console.send(messages.prefix() + messages.alias());
                        break;
                }
            } else {
                console.send(messages.prefix() + messages.alias());
            }
        }

        return false;
    }

    private String extract(final String[] data) {
        StringBuilder builder = new StringBuilder();

        for (String str : data) {
            OfflineClient client = new OfflineClient(str);
            if (client.getAccount() == null)
                builder.append(str).append(", ");
        }

        return StringUtils.replaceLast(builder.toString(), ", ", "");
    }

    private Map<AccountID, String> parse(final String[] data) {
        Map<AccountID, String> accounts = new LinkedHashMap<>();

        for (String str : data) {
            OfflineClient client = new OfflineClient(str);
            AccountManager manager = client.getAccount();

            if (manager != null)
                accounts.put(manager.getUUID(), str);
        }

        return accounts;
    }
}
