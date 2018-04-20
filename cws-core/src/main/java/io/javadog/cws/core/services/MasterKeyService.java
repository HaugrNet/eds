/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.jce.MasterKey;
import io.javadog.cws.core.model.Settings;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class MasterKeyService extends Serviceable<MasterKeyResponse, MasterKeyRequest> {

    public MasterKeyService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MasterKeyResponse perform(final MasterKeyRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.MASTER_KEY);
        Arrays.fill(request.getCredential(), (byte) 0);

        final MasterKey masterKey = MasterKey.getInstance(settings);
        masterKey.updateKey(request.getSecret(), settings.getSalt());
        return new MasterKeyResponse();
    }
}
