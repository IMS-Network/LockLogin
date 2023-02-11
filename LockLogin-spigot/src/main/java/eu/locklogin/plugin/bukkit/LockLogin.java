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
import eu.locklogin.api.common.utils.other.ASCIIArtGenerator;
import eu.locklogin.api.common.utils.version.VersionID;
import eu.locklogin.api.file.pack.PluginProperties;
import eu.locklogin.api.module.plugin.javamodule.ModuleLoader;
import eu.locklogin.api.util.platform.CurrentPlatform;
import ml.karmaconfigs.api.common.console.Console;
import ml.karmaconfigs.api.common.karma.source.APISource;
import ml.karmaconfigs.api.common.logger.KarmaLogger;
import ml.karmaconfigs.api.common.utils.enums.Level;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;

public class LockLogin {

    public static Main plugin = (Main) JavaPlugin.getProvidingPlugin(Main.class);

    public static Console console = APISource.loadProvider("LockLogin").console();

    public static String name = plugin.name();
    public static String update = FileInfo.getUpdateName(null);

    public static VersionID versionID = new VersionID(plugin.version(), update).generate();

    public static String version = versionID.getVersionID();

    public static File lockloginFile = new File(Main.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath().replaceAll("%20", " "));

    public static KarmaLogger logger = CurrentPlatform.getLogger();

    public static PluginProperties properties = new PluginProperties();

    public static ASCIIArtGenerator artGen = new ASCIIArtGenerator();

    public static ModuleLoader getLoader() {
        File modulesFolder = new File(plugin.getDataFolder() + File.separator + "plugin", "modules");

        if (!modulesFolder.exists())
            try {
                Files.createDirectories(modulesFolder.getParentFile().toPath());
            } catch (Throwable ignored) {
            }

        return new ModuleLoader();
    }

    public static void trySync(final TaskTarget target, final Runnable action) {
        try {
            plugin.getServer().getScheduler().runTask(plugin, action);
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
            logger.scheduleLog(Level.INFO, "Failed to schedule async task with identifier {0}. It will run without any special thread configuration!", target.getTaskId());

            console.send("Failed to perform task {0}");
            plugin.sync().queue(target.getTaskName(), action);
        }
    }

    public static void tryAsync(final TaskTarget target, final Runnable action) {
        try {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, action);
        } catch (Throwable ex) {
            logger.scheduleLog(Level.GRAVE, ex);
            logger.scheduleLog(Level.INFO, "Failed to schedule async task with identifier {0}. It will run sync!", target.getTaskId());

            console.send("Failed to perform task {0}");
            plugin.async().queue(target.getTaskName(), action);
        }
    }


}
