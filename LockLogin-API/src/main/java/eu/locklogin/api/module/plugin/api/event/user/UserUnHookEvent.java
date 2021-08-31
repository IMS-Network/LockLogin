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

import eu.locklogin.api.module.plugin.api.event.util.Event;
import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import org.jetbrains.annotations.Nullable;

/**
 * This event is fired when the plugin
 * unhooks a player, this event can happen
 * when the server is stopped, or when the plugin
 * is about to be updated.
 */
public final class UserUnHookEvent extends Event {

    private final ModulePlayer modulePlayer;
    private final Object eventObj;

    private boolean handled = false;
    private String handleReason = "";

    /**
     * Initialize event
     *
     * @param modulePlayerObject the player
     * @param event        the event in where this event is fired
     */
    public UserUnHookEvent(final ModulePlayer modulePlayerObject, final Object event) {
        modulePlayer = modulePlayerObject;
        eventObj = event;
    }

    /**
     * Get if the event is handleable or not
     *
     * @return if the event is handleable
     */
    @Override
    public boolean isHandleable() {
        return false;
    }

    /**
     * Check if the event has been handled
     *
     * @return if the event has been handled
     */
    @Override
    public boolean isHandled() {
        return isHandleable() && handled;
    }

    /**
     * Get the reason of why the event has been
     * marked as handled
     *
     * @return the event handle reason
     */
    @Override
    public String getHandleReason() {
        return handleReason;
    }

    /**
     * Set the event handle status
     *
     * @param status the handle status
     * @param reason the handle reason
     */
    public void setHandled(final boolean status, final String reason) {
        handled = status;
        handleReason = reason;
    }

    /**
     * Get the player
     *
     * @return the player
     */
    public ModulePlayer getPlayer() {
        return modulePlayer;
    }

    /**
     * Get the event instance
     *
     * @return the event instance
     */
    @Override
    public @Nullable Object getEvent() {
        return eventObj;
    }
}
