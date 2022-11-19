package eu.locklogin.api.util.platform;

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

/**
 * LockLogin available platforms
 */
public enum Platform {
    /**
     * Bukkit platform
     */
    BUKKIT,
    /**
     * BungeeCord platform
     */
    BUNGEE,
    /**
     * Velocity platform
     * @deprecated Support dropped
     */
    @Deprecated
    VELOCITY,
    /**
     * RedisBungeecord platform
     */
    REDIS
}
