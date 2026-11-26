/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core.managers;

import java.time.LocalDateTime;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.MemberEntity;

/**
 * <p>The attempts at the passphrase of an account, for its lock (#68). Each
 * attempt is counted before the passphrase is checked, so no more attempts
 * are tried than the limit allows, however many come at the same moment; an
 * accepted passphrase clears the count again.</p>
 *
 * <p>The Beans give their managers the {@code AuthLimitBean}, which keeps the
 * count in transactions of its own, so a request which fails afterwards
 * can't roll it back, and the request never holds the row of its account.
 * A manager which checks a passphrase without it refuses to.</p>
 *
 * <p>An attempt counts from its start until it is cleared, also a right one:
 * so when as many attempts as the limit are in progress at once, a further
 * one is refused until they end, even with the right passphrase. An
 * accepted passphrase clears the whole count, also wrong attempts made at
 * the same moment.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public interface AuthAttempts {

    /**
     * Counts an attempt at the passphrase of the account, before it is
     * checked.
     *
     * @param memberId The internal Id of the account
     * @return True if the passphrase may be tried, false if the account is
     * locked
     */
    boolean begin(long memberId);

    /**
     * Clears the count after the passphrase was accepted.
     *
     * @param memberId The internal Id of the account
     */
    void succeeded(long memberId);

    /**
     * The account a request with a passphrase names, read in a transaction
     * of its own: so the request's transaction has read nothing yet, and
     * holds no connection of the database, while the passphrase is checked
     * (ticket 28).
     *
     * @param name     The name of the account
     * @param circleId The Circle the request names, or null
     * @return What was found
     */
    Account findAccount(String name, String circleId);

    /**
     * What {@link #findAccount(String, String)} found.
     *
     * @param byName           The account of the name, or null
     * @param member           The account as the request may use it; with a
     *                         Circle named, it is a Trustee of it or an
     *                         administrator. If it is neither, findAccount
     *                         throws IDENTIFICATION_WARNING before the
     *                         passphrase, as before (ticket 26)
     * @param stoneHasAccounts Whether the stone has any account
     */
    record Account(MemberEntity byName, MemberEntity member, boolean stoneHasAccounts) {
    }

    /**
     * Finds the account with the given DAO, see {@link #findAccount(String, String)}.
     *
     * @param dao      The DAO
     * @param name     The name of the account
     * @param circleId The Circle the request names, or null
     * @return What was found
     */
    static Account findAccount(final CommonDao dao, final String name, final String circleId) {
        final MemberEntity byName = dao.findMemberByName(name);
        if (byName == null) {
            return new Account(null, null, dao.countMembers() != 0);
        }
        final MemberEntity member = ((circleId == null) || (byName.getMemberRole() == MemberRole.ADMIN))
                ? byName
                : dao.findMemberByNameAndCircleId(byName.getName(), circleId);
        return new Account(byName, member, true);
    }

    /**
     * Counts an attempt with the given DAO, see {@link #begin(long)}.
     *
     * @param dao      The DAO
     * @param settings The settings of the lock
     * @param memberId The internal Id of the account
     * @return True if the passphrase may be tried, always when the lock is off
     */
    static boolean begin(final CommonDao dao, final Settings settings, final long memberId) {
        final int max = settings.getAuthMaxFailedAttempts();
        if (max <= 0) {
            // The lock is off: nothing is counted, and every attempt is tried.
            return true;
        }
        final LocalDateTime now = Utilities.newDate();
        return dao.beginAttempt(memberId, now, now.minusMinutes(settings.getAuthFailedWindowMinutes()),
                max, now.plusMinutes(settings.getAuthLockoutMinutes()));
    }
}
