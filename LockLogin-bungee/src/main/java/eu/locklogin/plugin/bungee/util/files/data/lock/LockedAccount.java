package eu.locklogin.plugin.bungee.util.files.data.lock;

/*
 * Private GSA code
 *
 * The use of this code
 * without GSA team authorization
 * will be a violation of
 * terms of use determined
 * in <a href="http://karmaconfigs.cf/license/"> here </a>
 * or (fallback domain) <a href="https://karmaconfigs.github.io/page/license"> here </a>
 */

import eu.locklogin.api.account.AccountID;
import ml.karmaconfigs.api.common.karmafile.KarmaFile;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;

import static eu.locklogin.plugin.bungee.LockLogin.plugin;

public final class LockedAccount {

    private final KarmaFile lockedFile;

    /**
     * Initialize the locked account
     *
     * @param accId the account id
     */
    public LockedAccount(final AccountID accId) {
        File file = new File(plugin.getDataFolder() + File.separator + "data" + File.separator + "accounts", accId.getId() + ".locked");
        lockedFile = new KarmaFile(file);
    }

    /**
     * Lock the account
     */
    public void lock(final String administrator) {
        lockedFile.create();

        lockedFile.set("ISSUER", administrator);
        lockedFile.set("DATE", Instant.now());
    }

    /**
     * Unlock the account
     */
    public boolean unlock() {
        try {
            return Files.deleteIfExists(lockedFile.getFile().toPath());
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * Get the locked account data
     *
     * @return the locked account data
     */
    public LockedData getData() {
        String administrator = lockedFile.getString("ISSUER", "NONE");
        Instant date = Instant.parse(lockedFile.getString("DATE", Instant.now().toString()));

        return new LockedData(administrator, date, (lockedFile.exists() && !lockedFile.getFile().isDirectory()));
    }
}
