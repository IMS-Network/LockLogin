package eu.locklogin.plugin.bukkit;

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

import eu.locklogin.api.common.utils.FileInfo;
import eu.locklogin.api.common.web.ChecksumTables;
import eu.locklogin.api.util.platform.CurrentPlatform;
import eu.locklogin.api.util.platform.Platform;
import eu.locklogin.plugin.bukkit.listener.JoinListener;
import eu.locklogin.plugin.bukkit.plugin.Manager;
import eu.locklogin.plugin.bukkit.util.files.Config;
import eu.locklogin.plugin.bukkit.util.files.Message;
import ml.karmaconfigs.api.bukkit.KarmaPlugin;
import ml.karmaconfigs.api.bukkit.reflection.BarMessage;

import java.net.URL;

public final class Main extends KarmaPlugin {

    private final MainBootstrap plugin;

    private boolean unloaded = false;

    public Main() throws Throwable {
        super(false);

        CurrentPlatform.setMain(Main.class);
        CurrentPlatform.setPlatform(Platform.BUKKIT);

        ChecksumTables tables = new ChecksumTables();
        tables.checkTables();

        plugin = new MainBootstrap(this);
    }

    @Override
    public void enable() {
        plugin.enable();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!unloaded) {
                onDisable();
            }
        })); //Make sure the plugin shuts down correctly.

        CurrentPlatform.setOnline(getServer().getOnlineMode());
    }

    @Override
    public void onDisable() {
        plugin.disable();
        stopTasks();
        unloaded = true;
    }

    @Override
    public String name() {
        return getDescription().getName();
    }

    @Override
    public String version() {
        return getDescription().getVersion();
    }

    @Override
    public String description() {
        return getDescription().getDescription();
    }

    @Override
    public String[] authors() {
        return getDescription().getAuthors().toArray(new String[0]);
    }

    @Override
    public String updateURL() {
        URL url = FileInfo.versionHost(null);
        if (url != null)
            return url.toString();

        return null;
    }
}
