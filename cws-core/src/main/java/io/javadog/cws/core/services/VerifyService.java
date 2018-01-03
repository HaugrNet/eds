/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.SignatureEntity;

import javax.persistence.EntityManager;
import java.security.PublicKey;
import java.util.Date;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class VerifyService extends Serviceable<VerifyResponse, VerifyRequest> {

    public VerifyService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VerifyResponse perform(final VerifyRequest request) {
        verifyRequest(request, Permission.VERIFY_SIGNATURE);
        final String checksum = crypto.generateChecksum(request.getSignature());
        final SignatureEntity entity = dao.findByChecksum(checksum);
        final VerifyResponse response;

        if (entity != null) {
            final Date expires = entity.getExpires();
            if ((expires != null) && expires.before(new Date())) {
                response = new VerifyResponse(ReturnCode.SIGNATURE_WARNING, "The Signature has expired.");
            } else {
                final PublicKey publicKey = crypto.dearmoringPublicKey(entity.getPublicKey());
                final boolean verified = crypto.verify(publicKey, request.getData(), request.getSignature());

                if (verified) {
                    entity.setVerifications(entity.getVerifications() + 1);
                    dao.persist(entity);
                }

                response = new VerifyResponse();
                response.setVerified(verified);
            }
        } else {
            response = new VerifyResponse(ReturnCode.IDENTIFICATION_WARNING, "It was not possible to find the Signature.");
        }

        return response;
    }
}
