package eu.locklogin.api.module.plugin.api.event.user;

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

import java.net.InetAddress;
import java.util.UUID;

/**
 * This event is fired when a player joins
 * the server at the eyes of the plugin.
 */
public final class UserPreJoinEvent extends GenericJoinEvent {

    private final InetAddress address;
    private final UUID id;
    private final String player;
    private final Object eventObject;

    /**
     * Initialize event
     *
     * @param ip    the player ip
     * @param uuid  the player uuid
     * @param name  the player name
     * @param event the event instance
     */
    public UserPreJoinEvent(final InetAddress ip, final UUID uuid, final String name, final Object event) {
        address = ip;
        id = uuid;
        player = name;
        eventObject = event;
    }

    /**
     * Get the player ip
     *
     * @return the player ip
     */
    public InetAddress getIp() {
        return address;
    }

    /**
     * Get the player account id
     *
     * @return the player account id
     */
    public AccountID getAccountId() {
        return AccountID.fromUUID(id);
    }

    /**
     * Get the event player
     *
     * @return the event player
     */
    public String getName() {
        return player;
    }

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public Object getEvent() {
        return eventObject;
    }
}

