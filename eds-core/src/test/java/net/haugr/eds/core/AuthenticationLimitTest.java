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
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.FetchDataTypeRequest;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.requests.SettingRequest;
import net.haugr.eds.api.responses.AuthenticateResponse;
import net.haugr.eds.api.responses.MasterKeyResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.managers.AbstractManager;
import net.haugr.eds.core.managers.AuthAttempts;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.jce.MasterKey;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>Tests the authentication attempt limits (#68): an account is locked after
 * too many wrong passphrases within a window, the lock heals itself, and an
 * accepted passphrase clears the count. Only a wrong passphrase counts, and a
 * locked account gets the same answer as a wrong passphrase. The feature is
 * off by default.</p>
 *
 * <p>The tests have no JTA container, so the count is mostly written in the
 * transaction of the test; testAttemptsAreCountedBeforeTheyAreTried uses
 * transactions of their own, at the same moment. That the count survives the
 * rollback of a request is shown by EdsAuthLimitTest.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class AuthenticationLimitTest extends DatabaseSetup {

    private static final String WRONG = "a wrong passphrase";

    private Settings enabledSettings(final int maxFailed, final int windowMinutes, final int lockoutMinutes) {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.AUTH_MAX_FAILED_ATTEMPTS.getKey(), Integer.toString(maxFailed));
        mySettings.set(StandardSetting.AUTH_FAILED_WINDOW_MINUTES.getKey(), Integer.toString(windowMinutes));
        mySettings.set(StandardSetting.AUTH_LOCKOUT_MINUTES.getKey(), Integer.toString(lockoutMinutes));
        return mySettings;
    }

    private static AuthenticateResponse authenticate(final ManagementBean bean, final String account, final String passphrase) {
        final Authentication request = prepareRequest(Authentication.class, account);
        if (passphrase != null) {
            request.setCredential(credentialOf(passphrase));
        }
        return bean.authenticated(request);
    }

    private static AuthenticateResponse failOnce(final ManagementBean bean) {
        return authenticate(bean, MEMBER_1, WRONG);
    }

    private int failedLogins(final String account) {
        return ((Number) entityManager.createNativeQuery("select failed_logins from eds_members where name = :name")
                .setParameter("name", account).getSingleResult()).intValue();
    }

    private Object lockedUntil(final String account) {
        return entityManager.createNativeQuery("select locked_until from eds_members where name = :name")
                .setParameter("name", account).getSingleResult();
    }

    private boolean isLocked(final String account) {
        return ((Number) entityManager.createNativeQuery("select count(*) from eds_members where name = :name and locked_until > :now")
                .setParameter("name", account)
                .setParameter("now", Utilities.newDate())
                .getSingleResult()).longValue() > 0;
    }

    private void setCount(final String account, final int failed, final LocalDateTime firstFailedAt, final LocalDateTime until) {
        entityManager.createNativeQuery("update eds_members set failed_logins = :failed, first_failed_at = :first, locked_until = :until where name = :name")
                .setParameter("failed", failed)
                .setParameter("first", firstFailedAt)
                .setParameter("until", until)
                .setParameter("name", account)
                .executeUpdate();
    }

    // =========================================================================
    // Counting and locking
    // =========================================================================

    @Test
    void testFeatureDisabledByDefault() {
        final ManagementBean bean = prepareManagementBean(newSettings());

        for (int i = 0; i < 10; i++) {
            assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), failOnce(bean).getReturnCode());
        }

        assertEquals(0, failedLogins(MEMBER_1));
        assertNull(lockedUntil(MEMBER_1));
        assertTrue(authenticate(bean, MEMBER_1, null).isOk());
    }

    @Test
    void testAccountLocksAfterMaxFailures() {
        final ManagementBean bean = prepareManagementBean(enabledSettings(3, 15, 15));

        for (int i = 0; i < 3; i++) {
            final AuthenticateResponse response = failOnce(bean);
            assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
            assertEquals(AbstractManager.WRONG_CREDENTIALS, response.getReturnMessage());
        }

        assertEquals(3, failedLogins(MEMBER_1));
        assertTrue(isLocked(MEMBER_1));

        // Even the right passphrase is refused now, with the same answer as a
        // wrong one, so the lock tells nothing more.
        final AuthenticateResponse response = authenticate(bean, MEMBER_1, null);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals(AbstractManager.WRONG_CREDENTIALS, response.getReturnMessage());
        // Others are not touched.
        assertTrue(authenticate(bean, MEMBER_2, null).isOk());
    }

    @Test
    void testAttemptsWhileLockedDoNotExtendTheLock() {
        final ManagementBean bean = prepareManagementBean(enabledSettings(2, 15, 30));
        failOnce(bean);
        failOnce(bean);
        final Object firstLock = lockedUntil(MEMBER_1);
        assertNotNull(firstLock);

        assertEquals(AbstractManager.WRONG_CREDENTIALS, failOnce(bean).getReturnMessage());
        assertEquals(firstLock, lockedUntil(MEMBER_1));
        assertEquals(2, failedLogins(MEMBER_1));
    }

    @Test
    void testAcceptedPassphraseResetsTheCount() {
        final ManagementBean bean = prepareManagementBean(enabledSettings(5, 15, 15));
        failOnce(bean);
        failOnce(bean);
        assertEquals(2, failedLogins(MEMBER_1));

        assertTrue(authenticate(bean, MEMBER_1, null).isOk());

        assertEquals(0, failedLogins(MEMBER_1));
        assertNull(lockedUntil(MEMBER_1));
    }

    @Test
    void testLockHealsItself() {
        final ManagementBean bean = prepareManagementBean(enabledSettings(3, 15, 15));
        setCount(MEMBER_1, 3, Utilities.newDate().minusMinutes(20), Utilities.newDate().minusMinutes(1));
        assertFalse(isLocked(MEMBER_1));

        assertTrue(authenticate(bean, MEMBER_1, null).isOk());
        assertEquals(0, failedLogins(MEMBER_1));
        assertNull(lockedUntil(MEMBER_1));
    }

    @Test
    void testCountRestartsWhenTheWindowHasPassed() {
        final ManagementBean bean = prepareManagementBean(enabledSettings(5, 15, 15));
        setCount(MEMBER_1, 4, Utilities.newDate().minusMinutes(30), null);

        failOnce(bean);

        assertEquals(1, failedLogins(MEMBER_1));
        assertFalse(isLocked(MEMBER_1));
    }

    @Test
    void testCountRestartsAfterALockHasEnded() {
        // The lock (5 minutes) is shorter than the window (60): once it has
        // ended, one more wrong passphrase must not lock the account again.
        final ManagementBean bean = prepareManagementBean(enabledSettings(3, 60, 5));
        setCount(MEMBER_1, 3, Utilities.newDate().minusMinutes(6), Utilities.newDate().minusMinutes(1));

        failOnce(bean);

        assertEquals(1, failedLogins(MEMBER_1));
        assertFalse(isLocked(MEMBER_1));
    }

    @Test
    void testDisabledLimitCountsNothing() {
        final AuthLimitBean authLimit = new AuthLimitBean(entityManager, newSettings());
        final long memberId = dao.findMemberByName(MEMBER_1).getId();
        for (int i = 0; i < 3; i++) {
            assertTrue(authLimit.begin(memberId));
        }
        assertEquals(0, failedLogins(MEMBER_1));
        assertNull(lockedUntil(MEMBER_1));
    }

    @Test
    void testAttemptsAreCountedBeforeTheyAreTried() throws InterruptedException, ExecutionException {
        // Ten attempts at the same moment, each in a transaction of its own,
        // as the AuthLimitBean has them: with a limit of three, the count lets
        // exactly three through, and the account is then locked. (That the
        // count comes before the passphrase is checked is the order of
        // AbstractManager.checkPassphrase.)
        final long memberId = dao.findMemberByName(MEMBER_1).getId();
        final Settings mySettings = enabledSettings(3, 15, 15);
        final int attempts = 10;
        final List<Future<Boolean>> results = new ArrayList<>(attempts);
        try (ExecutorService pool = Executors.newFixedThreadPool(attempts)) {
            final CountDownLatch start = new CountDownLatch(1);
            for (int i = 0; i < attempts; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return inOwnTransaction(em -> AuthAttempts.begin(new CommonDao(em), mySettings, memberId));
                }));
            }
            start.countDown();
            int permitted = 0;
            for (final Future<Boolean> result : results) {
                permitted += result.get() ? 1 : 0;
            }

            assertEquals(3, permitted);
            assertEquals(3, failedLogins(MEMBER_1));
            assertTrue(isLocked(MEMBER_1));
        } finally {
            inOwnTransaction(em -> {
                new CommonDao(em).clearFailedLogins(memberId);
                return null;
            });
        }
    }

    private <T> T inOwnTransaction(final Function<EntityManager, T> work) {
        try (EntityManager em = entityManager.getEntityManagerFactory().createEntityManager()) {
            em.getTransaction().begin();
            final T result = work.apply(em);
            em.getTransaction().commit();
            return result;
        }
    }

    /** The count in transactions of their own, as the AuthLimitBean has it in the container. */
    private final class OwnTransactions extends AuthLimitBean {

        private final Settings limits;

        private OwnTransactions(final Settings limits) {
            super(entityManager, limits);
            this.limits = limits;
        }

        @Override
        public boolean begin(final long memberId) {
            return inOwnTransaction(em -> AuthAttempts.begin(new CommonDao(em), limits, memberId));
        }

        @Override
        public void succeeded(final long memberId) {
            inOwnTransaction(em -> {
                new CommonDao(em).clearFailedLogins(memberId);
                return null;
            });
        }
    }

    // =========================================================================
    // Only a wrong passphrase counts, and only an accepted one clears
    // =========================================================================

    @Test
    void testSessionWithAnotherAccountsNameNeitherClearsNorCounts() {
        final ManagementBean bean = prepareManagementBean(enabledSettings(5, 15, 15));
        failOnce(bean);
        failOnce(bean);

        // A valid Session of member2, which names member1: it succeeds as
        // member2, and must not clear member1's count.
        final ProcessMemberRequest login = prepareRequest(ProcessMemberRequest.class, MEMBER_2);
        login.setAction(Action.LOGIN);
        final ProcessMemberResponse session = bean.processMember(login);
        assertTrue(session.isOk(), session.getReturnMessage());
        final FetchMemberRequest withName = prepareSessionRequest(FetchMemberRequest.class, session.getSession());
        withName.setAccountName(MEMBER_1);
        assertTrue(bean.fetchMembers(withName).isOk());
        assertEquals(2, failedLogins(MEMBER_1));

        // A Session which isn't valid, naming member1, doesn't count either.
        final FetchMemberRequest bogus = prepareSessionRequest(FetchMemberRequest.class, new byte[32]);
        bogus.setAccountName(MEMBER_1);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), bean.fetchMembers(bogus).getReturnCode());
        assertEquals(2, failedLogins(MEMBER_1));
    }

    @Test
    void testOtherRefusalsDoNotCount() {
        final ManagementBean bean = prepareManagementBean(enabledSettings(2, 15, 15));

        // The right passphrase, but the MasterKey request isn't permitted to a member.
        for (int i = 0; i < 3; i++) {
            final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, MEMBER_1);
            request.setSecret(credentialOf("MasterKey"));
            assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), bean.masterKey(request).getReturnCode());
        }
        assertEquals(0, failedLogins(MEMBER_1));

        // An unknown name has no account to count against.
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), authenticate(bean, "no-such-account", WRONG).getReturnCode());
        assertNull(dao.findMemberByName("no-such-account"));

        // A request which isn't valid is refused before the passphrase.
        final Authentication invalid = prepareRequest(Authentication.class, MEMBER_1);
        invalid.setCredential(new byte[0]);
        for (int i = 0; i < 3; i++) {
            assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), bean.authenticated(invalid).getReturnCode());
        }
        assertEquals(0, failedLogins(MEMBER_1));
        assertTrue(authenticate(bean, MEMBER_1, null).isOk());

        // A wrong MasterKey secret, while EDS is locked, is refused before
        // the passphrase of the System Administrator is looked at.
        MasterKey.getInstance().lock();
        for (int i = 0; i < 3; i++) {
            final MasterKeyRequest wrongSecret = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
            wrongSecret.setSecret(credentialOf("not the MasterKey"));
            assertEquals("Invalid credentials.", bean.masterKey(wrongSecret).getReturnMessage());
        }
        assertEquals(0, failedLogins(Constants.ADMIN_ACCOUNT));
    }

    @Test
    void testAcceptedInvitationEndsALock() {
        // Wrong passphrases at the name of an invitation, before it is
        // accepted, lock it; accepting it gives a new passphrase, which ends
        // the lock, as the attempts were at the old one.
        final ManagementBean bean = prepareManagementBean(enabledSettings(2, 15, 15));
        final ProcessMemberRequest invite = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        invite.setAction(Action.INVITE);
        invite.setNewAccountName("locked guest");
        invite.setCircleId(CIRCLE_1_ID);
        invite.setTrustLevel(TrustLevel.READ);
        final ProcessMemberResponse invited = bean.processMember(invite);
        assertTrue(invited.isOk(), invited.getReturnMessage());
        authenticate(bean, "locked guest", WRONG);
        authenticate(bean, "locked guest", WRONG);
        assertTrue(isLocked("locked guest"));

        final ProcessMemberRequest accept = new ProcessMemberRequest();
        accept.setAccountName("locked guest");
        accept.setAction(Action.UPDATE);
        accept.setCredentialType(CredentialType.INVITATION);
        accept.setCredential(invited.getInvitation().clone());
        accept.setNewCredential(credentialOf("locked guest"));
        assertTrue(bean.processMember(accept).isOk());

        assertEquals(0, failedLogins("locked guest"));
        assertTrue(authenticate(bean, "locked guest", "locked guest").isOk());
    }

    @Test
    void testFirstAccountOfAStoneIsNotCounted() {
        // A stone without accounts: who comes first becomes its System
        // Administrator, an account this request makes and doesn't commit
        // until it ends, so it has nothing to count.
        // The count runs in transactions of its own, as in the container,
        // which can't see the new account: counted, it would be refused.
        for (final MemberEntity member : dao.findAllAscending(MemberEntity.class, "id")) {
            dao.delete(member);
        }
        final Settings mySettings = enabledSettings(1, 15, 15);
        final ManagementBean bean = new ManagementBean(entityManager, schemaManager, mySettings, null, null, new OwnTransactions(mySettings));

        assertTrue(authenticate(bean, "sysop", "sysop").isOk());
        assertEquals(0, failedLogins("sysop"));
    }

    @Test
    void testFailedCountKeepsEdsLocked() {
        // While EDS is locked, the right MasterKey secret installs the
        // MasterKey before the System Administrator's passphrase is checked.
        // If the count fails, with anything, EDS must be locked again.
        final Settings mySettings = enabledSettings(3, 15, 15);
        final AuthLimitBean failing = new AuthLimitBean(entityManager, mySettings) {
            @Override
            public boolean begin(final long memberId) {
                throw new IllegalStateException("The database is gone.");
            }
        };
        final ManagementBean bean = new ManagementBean(entityManager, schemaManager, mySettings, null, null, failing);
        MasterKey.getInstance().lock();
        final MasterKeyRequest unlock = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        unlock.setSecret(TEST_MASTERKEY_SECRET.getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class, () -> bean.masterKey(unlock));
        assertFalse(MasterKey.getInstance().isUnlocked());
    }

    @Test
    void testPassphraseIsNeverCheckedWithoutTheCount() {
        // A manager which checks a passphrase without the count refuses to:
        // counted in the request's transaction, a rollback would undo it.
        final ManagementBean bean = new ManagementBean(entityManager, schemaManager, newSettings(), null, null, null);
        final Authentication request = prepareRequest(Authentication.class, MEMBER_1);
        assertThrows(IllegalStateException.class, () -> bean.authenticated(request));
    }

    @Test
    void testShareRequestsCountAndClear() {
        final Settings mySettings = enabledSettings(5, 15, 15);
        final ShareBean share = prepareShareBean(mySettings);

        final FetchDataTypeRequest wrong = prepareRequest(FetchDataTypeRequest.class, MEMBER_1);
        wrong.setCredential(credentialOf(WRONG));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), share.fetchDataTypes(wrong).getReturnCode());
        assertEquals(1, failedLogins(MEMBER_1));

        assertTrue(share.fetchDataTypes(prepareRequest(FetchDataTypeRequest.class, MEMBER_1)).isOk());
        assertEquals(0, failedLogins(MEMBER_1));
    }

    // =========================================================================
    // The MasterKey request checks the lock too
    // =========================================================================

    @Test
    void testMasterKeyRequestCountsAndHonoursTheLock() {
        final ManagementBean bean = prepareManagementBean(enabledSettings(2, 15, 15));

        for (int i = 0; i < 2; i++) {
            final MasterKeyRequest wrong = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
            wrong.setCredential(credentialOf(WRONG));
            wrong.setSecret(credentialOf("Test MasterKey"));
            assertEquals("Invalid credentials.", bean.masterKey(wrong).getReturnMessage());
        }
        assertTrue(isLocked(Constants.ADMIN_ACCOUNT));

        // The right passphrase and the right secret, but the account is locked.
        final MasterKeyRequest right = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        right.setSecret(TEST_MASTERKEY_SECRET.getBytes(StandardCharsets.UTF_8));
        final MasterKeyResponse refused = bean.masterKey(right);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), refused.getReturnCode());
        assertEquals("Invalid credentials.", refused.getReturnMessage());

        // While EDS is locked, a locked System Administrator can't unlock it,
        // and EDS stays locked.
        MasterKey.getInstance().lock();
        final MasterKeyRequest unlock = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        unlock.setSecret(TEST_MASTERKEY_SECRET.getBytes(StandardCharsets.UTF_8));
        assertEquals("Invalid credentials.", bean.masterKey(unlock).getReturnMessage());
        assertFalse(MasterKey.getInstance().isUnlocked());
    }

    // =========================================================================
    // The settings
    // =========================================================================

    @Test
    void testSettingsAreCheckedWhenSet() {
        final ManagementBean bean = prepareManagementBean(newSettings());
        final String max = StandardSetting.AUTH_MAX_FAILED_ATTEMPTS.getKey();
        final String window = StandardSetting.AUTH_FAILED_WINDOW_MINUTES.getKey();
        final String lockout = StandardSetting.AUTH_LOCKOUT_MINUTES.getKey();

        final String places = StandardSetting.AUTH_CONCURRENT_PASSPHRASES.getKey();
        final String wait = StandardSetting.AUTH_PASSPHRASE_WAIT_MILLIS.getKey();

        for (final String[] refused : new String[][] { { max, "five" }, { max, "99999999999" }, { max, "1001" }, { max, "-1" },
                { window, "0" }, { window, "1441" }, { lockout, "0" }, { lockout, "999999999" },
                { places, "-1" }, { places, "1025" }, { places, "half" }, { wait, "-1" }, { wait, "60001" } }) {
            assertEquals(ReturnCode.SETTING_WARNING.getCode(), change(bean, refused[0], refused[1]), refused[0] + '=' + refused[1]);
        }
        for (final String[] accepted : new String[][] { { max, "0" }, { max, "1000" }, { window, "1" }, { window, "1440" },
                { lockout, "1" }, { lockout, "1440" }, { places, "0" }, { places, "1024" }, { wait, "0" }, { wait, "60000" } }) {
            assertEquals(ReturnCode.SUCCESS.getCode(), change(bean, accepted[0], accepted[1]), accepted[0] + '=' + accepted[1]);
        }
    }

    @Test
    void testStoredValuesNeverStopTheLogin() {
        // Values stored before they were checked: not numbers, or out of range.
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.AUTH_MAX_FAILED_ATTEMPTS.getKey(), "five");
        mySettings.set(StandardSetting.AUTH_FAILED_WINDOW_MINUTES.getKey(), "99999999999");
        mySettings.set(StandardSetting.AUTH_LOCKOUT_MINUTES.getKey(), "-3");
        assertEquals(0, mySettings.getAuthMaxFailedAttempts());
        assertEquals(15, mySettings.getAuthFailedWindowMinutes());
        assertEquals(1, mySettings.getAuthLockoutMinutes());
        assertTrue(authenticate(prepareManagementBean(mySettings), MEMBER_1, null).isOk());

        mySettings.set(StandardSetting.AUTH_MAX_FAILED_ATTEMPTS.getKey(), "5000");
        mySettings.set(StandardSetting.AUTH_LOCKOUT_MINUTES.getKey(), "999999999");
        assertEquals(Settings.MAX_AUTH_FAILED_ATTEMPTS, mySettings.getAuthMaxFailedAttempts());
        assertEquals(Settings.MAX_AUTH_MINUTES, mySettings.getAuthLockoutMinutes());

        mySettings.remove(StandardSetting.AUTH_LOCKOUT_MINUTES.getKey());
        assertEquals(15, mySettings.getAuthLockoutMinutes());
    }

    private static int change(final ManagementBean bean, final String key, final String value) {
        final SettingRequest request = prepareRequest(SettingRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSettings(new HashMap<>(Map.of(key, value)));
        return bean.settings(request).getReturnCode();
    }
}
