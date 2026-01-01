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

import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import jakarta.persistence.EntityManager;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.requests.VerifyRequest;
import net.haugr.eds.api.responses.VerifyResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.exceptions.IdentificationException;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.SignatureDao;
import net.haugr.eds.core.model.entities.SignatureEntity;

/**
 * <p>Business Logic implementation for the EDS Verify request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class VerifyManager extends AbstractManager<SignatureDao, VerifyResponse, VerifyRequest> {

    public VerifyManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new SignatureDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VerifyResponse perform(final VerifyRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.VERIFY_SIGNATURE);
        Arrays.fill(request.getCredential(), (byte) 0);

        final byte[] signature = Base64.getDecoder().decode(request.getSignature());
        final String checksum = crypto.generateChecksum(signature);
        final SignatureEntity entity = dao.findByChecksum(checksum);

        if (entity == null) {
            throw new IdentificationException("It was not possible to find the Signature.");
        }

        final LocalDateTime expires = entity.getExpires();
        if ((expires != null) && expires.isBefore(Utilities.newDate())) {
            throw new EDSException(ReturnCode.SIGNATURE_WARNING, "The Signature has expired.");
        }

        final PublicKey publicKey = crypto.dearmoringPublicKey(entity.getPublicKey());
        final boolean verified = crypto.verify(publicKey, request.getData(), signature);

        if (verified) {
            entity.setVerifications(entity.getVerifications() + 1);
            dao.save(entity);
        }

        final VerifyResponse response = new VerifyResponse("The signature has successfully been verified.");
        response.setVerified(verified);

        return response;
    }
}
