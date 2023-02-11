package eu.locklogin.api.common.session.online;

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
 * LockLogin logged/registered data
 */
public final class SessionChangeData {

    private final DataType type;
    private final DataChange change;
    private final int amount;

    /**
     * Initialize the session change data
     *
     * @param _type      the change type
     * @param _change    the data change type
     * @param changeSize the number change amount
     */
    public SessionChangeData(final DataType _type, final DataChange _change, final int changeSize) {
        type = _type;
        change = _change;
        amount = changeSize;
    }

    /**
     * Get the data type
     *
     * @return the data type
     */
    public DataType getDataType() {
        return type;
    }

    /**
     * Get the data change type
     *
     * @return the change type
     */
    public DataChange getChangeType() {
        return change;
    }

    /**
     * Get the change size
     *
     * @return the changed size
     */
    public int getSize() {
        return amount;
    }

    /**
     * Available session data type
     */
    public enum DataType {
        /**
         * Login data type
         */
        LOGIN,

        /**
         * Register data type
         */
        REGISTER;
    }

    /**
     * Available data change type
     */
    public enum DataChange {
        /**
         * Data decreased by 1
         */
        DECREASE,

        /**
         * Data decreased by 1
         */
        INCREASE,

        /**
         * Data stills the same, nothing
         * has changed
         */
        SAME
    }
}
