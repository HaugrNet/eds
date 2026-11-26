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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Arrays;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.api.responses.FetchMemberResponse;
import net.haugr.eds.api.responses.MasterKeyResponse;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.jce.MasterKey;
import net.haugr.eds.core.managers.MasterKeyManager;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.MasterKeyEntity;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.SettingEntity;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>Tests for the MasterKey lifecycle (PLAN.md, Crypto §1 and phase C2): loading
 * at startup, unlocking, first setup and changing it. The test MasterKey is
 * installed again before each test, and the database changes are rolled
 * back.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class ManagementBeanMasterKeyTest extends DatabaseSetup {

    private static final String LOCKED_MESSAGE = "EDS is locked, the MasterKey is not loaded.";

    // =========================================================================
    // Request validation
    // =========================================================================

    @Test
    void testUpdateMasterKeyWithNullRequest() {
        final ManagementBean bean = prepareManagementBean();

        // Null is passed on purpose, to check how it is refused.
        @SuppressWarnings("findbugs:NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
        final MasterKeyResponse response = bean.masterKey(null);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Cannot Process a NULL Object.", response.getReturnMessage());
    }

    @Test
    void testUpdateMasterKeyWithEmptyRequest() {
        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = new MasterKeyRequest();

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("""
                Request Object contained errors:
                Key: credential, Error: The Session (Credential) is missing.
                Key: secret, Error: Either the secret or the URL must be given to alter the MasterKey.
                Key: url, Error: Either the secret or the URL must be given to alter the MasterKey.""", response.getReturnMessage());
    }

    @Test
    void testUpdateMasterKeyAsMember() {
        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, MEMBER_1);
        request.setSecret(bytes(NEW_MASTERKEY_SECRET));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Given Account is not permitted to perform this request.", response.getReturnMessage());
    }

    // =========================================================================
    // Unlocked EDS
    // =========================================================================

    @Test
    void testUpdateMasterKeyAsAdminWithWrongCredentials() {
        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCredential(bytes("root"));
        request.setSecret(bytes(NEW_MASTERKEY_SECRET));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Invalid credentials.", response.getReturnMessage());
    }

    @Test
    void testUpdateMasterKeyToCurrent() {
        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(bytes(TEST_MASTERKEY_SECRET));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("MasterKey unlocked.", response.getReturnMessage());
        assertEquals(1, MasterKey.getInstance().getVersion());
    }

    @Test
    void testChangeMasterKeyAdminOnly() {
        deleteNonAdminMembers();
        final String kcvBefore = MasterKey.getInstance().getKcv();

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(bytes(NEW_MASTERKEY_SECRET));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("MasterKey updated.", response.getReturnMessage());

        // A new version and KCV are stored, and the System Administrator
        // account was re-protected with the new MasterKey.
        assertEquals(2, MasterKey.getInstance().getVersion());
        final MasterKeyEntity stored = dao.findCurrentMasterKey();
        assertEquals(2, stored.getVersion());
        assertNotEquals(kcvBefore, stored.getKcv());
        assertEquals(MasterKey.getInstance().getKcv(), stored.getKcv());

        final FetchMemberResponse fetched = bean.fetchMembers(prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT));
        assertTrue(fetched.isOk(), fetched.getReturnMessage());
    }

    @Test
    void testARotatedMasterKeyIsInstalledOnlyWhenCommitted() {
        assertTrue(rotateWith(Status.STATUS_COMMITTED));
    }

    @Test
    void testARotationRolledBackKeepsTheMasterKey() {
        assertFalse(rotateWith(Status.STATUS_ROLLEDBACK));
    }

    /**
     * Rotates the MasterKey in a transaction which ends with the given status
     * (ticket 13): until it has ended, the old MasterKey is in use. Whether
     * the new one was installed then.
     */
    private boolean rotateWith(final int status) {
        deleteNonAdminMembers();
        final String kcvBefore = MasterKey.getInstance().getKcv();
        final List<Synchronization> registered = new java.util.ArrayList<>();
        final ManagementBean bean = new ManagementBean(entityManager, settings, registry(registered));
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(bytes(NEW_MASTERKEY_SECRET));

        final MasterKeyResponse response = bean.masterKey(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        assertEquals(kcvBefore, MasterKey.getInstance().getKcv(), "The new MasterKey was installed before the commit");
        assertEquals(1, registered.size());

        registered.getFirst().afterCompletion(status);
        return !kcvBefore.equals(MasterKey.getInstance().getKcv());
    }

    /** A registry of a transaction which is active, and keeps what is registered. */
    private static TransactionSynchronizationRegistry registry(final List<Synchronization> registered) {
        return (TransactionSynchronizationRegistry) java.lang.reflect.Proxy.newProxyInstance(ManagementBeanMasterKeyTest.class.getClassLoader(),
                new Class<?>[] { TransactionSynchronizationRegistry.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "getTransactionStatus" -> Status.STATUS_ACTIVE;
                    case "registerInterposedSynchronization" -> registered.add((Synchronization) args[0]);
                    case "setRollbackOnly" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    // The paths are the test's own resources and temporary files.
    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    @Test
    void testChangeMasterKeyViaUrl() throws IOException {
        deleteNonAdminMembers();
        final String path = tempDir() + "secret_master_key.bin";
        Files.write(Paths.get(path), generateData(8192));

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setUrl("file://" + path);

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("MasterKey updated.", response.getReturnMessage());
        assertEquals("file://" + path, currentMasterKeyUrl());
    }

    @Test
    void testUpdateMasterKeyWithWeakSecret() {
        deleteNonAdminMembers();
        final String kcvBefore = MasterKey.getInstance().getKcv();

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(bytes("New MasterKey"));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("The MasterKey secret is too weak. Use at least 20 random characters (letters, digits and symbols), or a passphrase of several random words.", response.getReturnMessage());
        assertEquals(kcvBefore, MasterKey.getInstance().getKcv());
        assertEquals(1, MasterKey.getInstance().getVersion());
    }

    @Test
    void testUpdateMasterKeyWithUnreachableURL() {
        final String path = tempDir() + "not_existing_file.bin";
        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setUrl("file://" + path);

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.NETWORK_ERROR.getCode(), response.getReturnCode());
        // A fixed message, without the URL (ticket 5).
        assertEquals("The MasterKey source could not be read (not a regular file).", response.getReturnMessage());
    }

    /**
     * Without the passphrase, nothing is fetched (ticket 6): before, the URL
     * was read first, so anyone who knew the name of the System
     * Administrator could make the stone fetch any URL.
     */
    @Test
    void testNothingIsFetchedWithoutThePassphrase() throws IOException {
        try (LocalListener listener = LocalListener.answering()) {
            final ManagementBean bean = prepareManagementBean();
            final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
            request.setCredential(bytes("root"));
            request.setUrl("https://127.0.0.1:" + listener.port() + "/secret");

            final MasterKeyResponse response = bean.masterKey(request);
            assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
            assertEquals("Invalid credentials.", response.getReturnMessage());
            assertEquals(0, listener.accepted(), "The URL may not be fetched before the passphrase is checked.");
        }
    }

    /**
     * While EDS is locked, the secret must be read before the passphrase can
     * be checked. A source which can't be read is answered as a wrong
     * secret, so the answer tells nothing about files or hosts (ticket 6),
     * and the log has the kind of failure, without the URL (ticket 5).
     */
    @Test
    void testAnUnreadableSourceWhileLockedIsAnsweredAsAWrongSecret() throws IOException {
        MasterKey.getInstance().lock();
        final ManagementBean bean = prepareManagementBean();

        try (LocalListener silent = LocalListener.silent()) {
            // The first fails without the fix, before /dev/zero is tried,
            // which was read until the memory ran out: a missing file, a
            // query (which may hold a secret), a device, another scheme, and
            // a server which never answers.
            for (final String url : List.of("file://" + tempDir() + "no-such-file-x7", "file://" + tempDir() + "no-such-file-x7?key=s3cr3t",
                    "file:///dev/zero", "http://127.0.0.1:1/secret", "https://127.0.0.1:" + silent.port() + "/secret")) {
                assertAnsweredAsAWrongSecret(bean, url);
            }
        }
    }

    /**
     * While others hold both slots for reading a source, the System
     * Administrator is told so, not that the secret is wrong; the refusal
     * depends on no URL, so it tells an attacker nothing.
     */
    @Test
    void testBusySourcesWhileLockedAreSaidAsSuch() throws Exception {
        MasterKey.getInstance().lock();
        final ManagementBean bean = prepareManagementBean();

        try (LocalListener silent = LocalListener.silent()) {
            final String url = "https://127.0.0.1:" + silent.port() + "/secret";
            final List<Thread> readers = new java.util.ArrayList<>();
            for (int i = 0; i < MasterKey.MAX_FETCHES; i++) {
                readers.add(Thread.ofVirtual().start(() -> assertThrows(EDSException.class, () -> MasterKey.readMasterKeySecretFromUrl(url))));
            }
            final long until = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            while (silent.accepted() < MasterKey.MAX_FETCHES) {
                assertTrue(System.nanoTime() < until, "The readers didn't connect.");
                java.util.concurrent.locks.LockSupport.parkNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(10));
            }

            final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
            request.setUrl(url);
            final MasterKeyResponse response = bean.masterKey(request);
            assertEquals(ReturnCode.NETWORK_ERROR.getCode(), response.getReturnCode());
            assertEquals(MasterKey.SOURCES_BUSY, response.getReturnMessage());
            assertFalse(MasterKey.getInstance().isUnlocked());

            for (final Thread reader : readers) {
                reader.join(java.time.Duration.ofSeconds(20));
            }
        }
    }

    private void assertAnsweredAsAWrongSecret(final ManagementBean bean, final String url) {
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setUrl(url);

        try (LogCapture log = LogCapture.of(MasterKeyManager.class)) {
            final MasterKeyResponse response = bean.masterKey(request);
            assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode(), url);
            assertEquals("Invalid credentials.", response.getReturnMessage(), url);
            assertThat(log.warnings()).singleElement().asString()
                    .startsWith("The MasterKey request's source could not be read while EDS is locked: The MasterKey source ")
                    .doesNotContain("no-such-file-x7").doesNotContain("s3cr3t").doesNotContain("127.0.0.1");
        }
        assertFalse(MasterKey.getInstance().isUnlocked(), url);
    }

    // The paths are the test's own resources and temporary files.
    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    @Test
    void testUpdateMasterKeyUrlWithWrongCredentialsKeepsSetting() throws IOException {
        final String before = currentMasterKeyUrl();
        final String path = tempDir() + "unauthenticated_master_key.bin";
        Files.write(Paths.get(path), generateData(8192));

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCredential(bytes("root"));
        request.setUrl("file://" + path);

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Invalid credentials.", response.getReturnMessage());
        assertEquals(before, currentMasterKeyUrl());
    }

    /**
     * Rotating the MasterKey with Members, Circles and Data (PLAN.md, Crypto §1):
     * every outer layer is re-wrapped, the Data Objects are not touched, and
     * everything can still be read afterwards.
     */
    @Test
    void testRotateMasterKeyWithMembersAndData() {
        final ShareBean shareBean = prepareShareBean();
        final ProcessDataRequest addRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "rotation data", 200_000);
        final byte[] content = Arrays.copyOf(addRequest.getData(), addRequest.getData().length);
        final ProcessDataResponse added = shareBean.processData(addRequest);
        assertTrue(added.isOk(), added.getReturnMessage());
        final byte[] storedBefore = storedData(added.getDataId());
        final String trusteeBefore = firstTrusteeKey();

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(bytes(NEW_MASTERKEY_SECRET));
        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode(), response.getReturnMessage());
        assertEquals("MasterKey updated.", response.getReturnMessage());
        assertEquals(2, MasterKey.getInstance().getVersion());
        assertEquals(2, dao.findCurrentMasterKey().getVersion());

        // The outer layers changed, the Data Object didn't.
        assertNotEquals(trusteeBefore, firstTrusteeKey());
        assertArrayEquals(storedBefore, storedData(added.getDataId()));

        // Members still log in, and the data reads as before, also by
        // another Trustee of the Circle.
        final FetchDataRequest fetch1 = prepareRequest(FetchDataRequest.class, MEMBER_1);
        fetch1.setDataId(added.getDataId());
        final FetchDataResponse fetched1 = shareBean.fetchData(fetch1);
        assertTrue(fetched1.isOk(), fetched1.getReturnMessage());
        assertArrayEquals(content, fetched1.getData());
        final FetchDataRequest fetch2 = prepareRequest(FetchDataRequest.class, MEMBER_2);
        fetch2.setDataId(added.getDataId());
        assertArrayEquals(content, shareBean.fetchData(fetch2).getData());

        // The old MasterKey is no longer accepted when unlocking.
        MasterKey.getInstance().lock();
        final MasterKeyRequest oldKey = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        oldKey.setSecret(bytes(TEST_MASTERKEY_SECRET));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), bean.masterKey(oldKey).getReturnCode());
    }

    @Test
    void testRotationEndsSessionsAndVoidsInvitations() {
        final ManagementBean bean = prepareManagementBean();
        final ProcessMemberRequest login = prepareRequest(ProcessMemberRequest.class, MEMBER_1);
        login.setAction(Action.LOGIN);
        final ProcessMemberResponse loginResponse = bean.processMember(login);
        assertTrue(loginResponse.isOk());
        final ProcessMemberRequest invite = prepareRequest(ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        invite.setAction(Action.INVITE);
        invite.setNewAccountName("rotation invitee");
        assertTrue(bean.processMember(invite).isOk());

        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(bytes(NEW_MASTERKEY_SECRET));
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.masterKey(request).getReturnCode());

        assertNull(dao.findMemberByName("rotation invitee"));
        final FetchMemberResponse viaSession = bean.fetchMembers(prepareSessionRequest(FetchMemberRequest.class, loginResponse.getSession()));
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), viaSession.getReturnCode());
    }

    // The paths are the test's own resources and temporary files.
    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    @Test
    void testRotationWithWrongCredentialsKeepsSetting() throws IOException {
        final String before = currentMasterKeyUrl();
        final String path = tempDir() + "rejected_master_key.bin";
        Files.write(Paths.get(path), generateData(8192));

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCredential(bytes("wrong credential"));
        request.setUrl("file://" + path);

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals(before, currentMasterKeyUrl());
        assertEquals(1, MasterKey.getInstance().getVersion());
    }

    // =========================================================================
    // Locked EDS
    // =========================================================================

    @Test
    void testRequestsFailWhileLocked() {
        MasterKey.getInstance().lock();

        final ManagementBean bean = prepareManagementBean();
        final FetchMemberResponse response = bean.fetchMembers(prepareRequest(FetchMemberRequest.class, Constants.ADMIN_ACCOUNT));
        assertEquals(ReturnCode.CRYPTO_ERROR.getCode(), response.getReturnCode());
        assertEquals(LOCKED_MESSAGE, response.getReturnMessage());
    }

    @Test
    void testUnlockWithCorrectMasterKey() {
        MasterKey.getInstance().lock();

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(bytes(TEST_MASTERKEY_SECRET));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("MasterKey unlocked.", response.getReturnMessage());
        assertTrue(MasterKey.getInstance().isUnlocked());
        assertTrue(bean.fetchMembers(prepareRequest(FetchMemberRequest.class, MEMBER_1)).isOk());
    }

    /**
     * A wrong MasterKey is rejected by the key check value, before anything
     * is decrypted, and EDS stays locked.
     */
    @Test
    void testUnlockWithWrongMasterKeyIsRejectedByKcv() {
        MasterKey.getInstance().lock();

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(bytes(NEW_MASTERKEY_SECRET));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Invalid credentials.", response.getReturnMessage());
        assertFalse(MasterKey.getInstance().isUnlocked());
    }

    @Test
    void testUnlockWithWrongAdminCredentialsStaysLocked() {
        MasterKey.getInstance().lock();

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setCredential(bytes("root"));
        request.setSecret(bytes(TEST_MASTERKEY_SECRET));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertFalse(MasterKey.getInstance().isUnlocked());
    }

    @Test
    void testFirstSetup() {
        deleteAllMembers();
        deleteMasterKeyRecords();
        MasterKey.getInstance().lock();

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(bytes(NEW_MASTERKEY_SECRET));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("MasterKey unlocked.", response.getReturnMessage());

        final MasterKeyEntity stored = dao.findCurrentMasterKey();
        assertNotNull(stored);
        assertEquals(1, stored.getVersion());
        assertEquals(MasterKey.derive(bytes(NEW_MASTERKEY_SECRET)).kcv(), stored.getKcv());
        assertNotNull(dao.findMemberByName(Constants.ADMIN_ACCOUNT));
    }

    /**
     * A stone without accounts: the first MasterKey comes with the first
     * account's passphrase, and if that can't be the passphrase of an
     * account, as it's too weak, EDS must stay locked, although the MasterKey
     * itself was right.
     */
    @Test
    void testFirstSetupWithWeakPassphraseStaysLocked() {
        deleteAllMembers();
        deleteMasterKeyRecords();
        MasterKey.getInstance().lock();

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, "sysop");
        request.setCredential(bytes("weak"));
        request.setSecret(bytes(NEW_MASTERKEY_SECRET));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode(), response.getReturnMessage());
        assertFalse(MasterKey.getInstance().isUnlocked());
    }

    /**
     * The name of the System Administrator is not given by the stone: who
     * gives a stone without accounts its first MasterKey becomes it, under
     * the name given. After that, the name "admin" is a name like any other.
     */
    @Test
    void testFirstSetupUnderAnotherName() {
        deleteAllMembers();
        deleteMasterKeyRecords();
        MasterKey.getInstance().lock();

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, "sysop");
        request.setSecret(bytes(NEW_MASTERKEY_SECRET));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode(), response.getReturnMessage());
        assertEquals(net.haugr.eds.api.common.MemberRole.ADMIN, dao.findMemberByName("sysop").getMemberRole());
        assertNull(dao.findMemberByName(Constants.ADMIN_ACCOUNT));
        assertEquals(1L, dao.countMembers());

        // Nobody else may give the MasterKey, also not under the name "admin".
        final MasterKeyRequest other = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        other.setSecret(bytes(NEW_MASTERKEY_SECRET));
        final MasterKeyResponse refused = bean.masterKey(other);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), refused.getReturnCode());
        assertEquals("Given Account is not permitted to perform this request.", refused.getReturnMessage());
        assertNull(dao.findMemberByName(Constants.ADMIN_ACCOUNT));

        // The one who became it may, again.
        final MasterKeyRequest again = prepareRequest(MasterKeyRequest.class, "sysop");
        again.setSecret(bytes(NEW_MASTERKEY_SECRET));
        assertEquals(ReturnCode.SUCCESS.getCode(), bean.masterKey(again).getReturnCode());
    }

    /**
     * Another account with the role of an administrator is not the System
     * Administrator, and may not give the MasterKey.
     */
    @Test
    void testUpdateMasterKeyAsOtherAdministrator() {
        final ManagementBean bean = prepareManagementBean();
        final net.haugr.eds.api.requests.ProcessMemberRequest create = prepareRequest(net.haugr.eds.api.requests.ProcessMemberRequest.class, Constants.ADMIN_ACCOUNT);
        create.setAction(net.haugr.eds.api.common.Action.CREATE);
        create.setNewAccountName("second admin");
        create.setNewCredential(credentialOf("second admin"));
        create.setMemberRole(net.haugr.eds.api.common.MemberRole.ADMIN);
        assertTrue(bean.processMember(create).isOk());

        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, "second admin");
        request.setSecret(bytes(NEW_MASTERKEY_SECRET));
        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.AUTHENTICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Given Account is not permitted to perform this request.", response.getReturnMessage());
    }

    @Test
    void testFirstSetupWithWeakSecret() {
        deleteAllMembers();
        deleteMasterKeyRecords();
        MasterKey.getInstance().lock();

        final ManagementBean bean = prepareManagementBean();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(bytes(Constants.ADMIN_ACCOUNT));

        final MasterKeyResponse response = bean.masterKey(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertFalse(MasterKey.getInstance().isUnlocked());
    }

    // =========================================================================
    // Startup
    // =========================================================================

    /**
     * There is no fallback MasterKey: without a source, EDS stays locked.
     */
    @Test
    void testStartupWithoutSourceStaysLocked() {
        MasterKey.getInstance().lock();
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.MASTERKEY_URL.getKey(), "");

        assertFalse(new MasterKeyManager(mySettings, entityManager).loadAtStartup());
        assertFalse(MasterKey.getInstance().isUnlocked());
    }

    @Test
    void testStartupWithUnreadableSourceStaysLocked() {
        MasterKey.getInstance().lock();
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.MASTERKEY_URL.getKey(), "file://" + tempDir() + "not_existing_file.bin");

        assertFalse(new MasterKeyManager(mySettings, entityManager).loadAtStartup());
        assertFalse(MasterKey.getInstance().isUnlocked());
    }

    @Test
    void testStartupWithWrongSourceStaysLocked() throws IOException {
        MasterKey.getInstance().lock();
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.MASTERKEY_URL.getKey(), writeMasterKeyFile("wrong_master_key.bin", NEW_MASTERKEY_SECRET));

        assertFalse(new MasterKeyManager(mySettings, entityManager).loadAtStartup());
        assertFalse(MasterKey.getInstance().isUnlocked());
    }

    @Test
    void testStartupWithCorrectSource() throws IOException {
        MasterKey.getInstance().lock();
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.MASTERKEY_URL.getKey(), writeMasterKeyFile("test_master_key.bin", TEST_MASTERKEY_SECRET));

        assertTrue(new MasterKeyManager(mySettings, entityManager).loadAtStartup());
        assertTrue(MasterKey.getInstance().isUnlocked());
        assertEquals(1, MasterKey.getInstance().getVersion());
    }

    @Test
    void testStartupFirstSetup() throws IOException {
        deleteAllMembers();
        deleteMasterKeyRecords();
        MasterKey.getInstance().lock();
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.MASTERKEY_URL.getKey(), writeMasterKeyFile("new_master_key.bin", NEW_MASTERKEY_SECRET));

        assertTrue(new MasterKeyManager(mySettings, entityManager).loadAtStartup());
        assertEquals(1, dao.findCurrentMasterKey().getVersion());
    }

    // =========================================================================
    // Internal helper methods
    // =========================================================================

    private static byte[] bytes(final String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String tempDir() {
        return System.getProperty("java.io.tmpdir") + File.separator;
    }

    // The paths are the test's own resources and temporary files.
    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    private static String writeMasterKeyFile(final String name, final String secret) throws IOException {
        final String path = tempDir() + name;
        Files.writeString(Paths.get(path), secret, StandardCharsets.UTF_8);
        return "file://" + path;
    }

    private String currentMasterKeyUrl() {
        final SettingEntity entity = dao.findSettingByKey(StandardSetting.MASTERKEY_URL);
        return (entity != null) ? entity.getSetting() : null;
    }

    private void deleteNonAdminMembers() {
        final List<MemberEntity> members = dao.findAllAscending(MemberEntity.class, "id");
        for (final MemberEntity member : members) {
            if (!Constants.ADMIN_ACCOUNT.equals(member.getName())) {
                dao.delete(member);
            }
        }
    }

    private void deleteAllMembers() {
        for (final MemberEntity member : dao.findAllAscending(MemberEntity.class, "id")) {
            dao.delete(member);
        }
    }

    private byte[] storedData(final String dataId) {
        // What is in the database, part by part, whatever the EntityManager holds.
        final java.io.ByteArrayOutputStream stored = new java.io.ByteArrayOutputStream();
        final java.util.List<?> parts = entityManager.createNativeQuery("SELECT p.part FROM eds_data_parts p JOIN eds_data d ON d.id = p.data_id " +
                        "JOIN eds_metadata m ON m.id = d.metadata_id WHERE m.external_id = :id ORDER BY p.part_offset")
                .setParameter("id", dataId)
                .getResultList();
        for (final Object part : parts) {
            stored.writeBytes((byte[]) part);
        }
        return stored.toByteArray();
    }

    private String firstTrusteeKey() {
        return (String) entityManager.createQuery("select t.circleKey from TrusteeEntity t order by t.id")
                .setMaxResults(1)
                .getSingleResult();
    }

    private void deleteMasterKeyRecords() {
        entityManager.createQuery("delete from MasterKeyEntity").executeUpdate();
    }
}
