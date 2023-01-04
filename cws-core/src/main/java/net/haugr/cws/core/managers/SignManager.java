/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.core.managers;

import java.util.Arrays;
import java.util.Base64;
import javax.persistence.EntityManager;
import net.haugr.cws.api.requests.SignRequest;
import net.haugr.cws.api.responses.SignResponse;
import net.haugr.cws.core.enums.Permission;
import net.haugr.cws.core.model.Settings;
import net.haugr.cws.core.model.SignatureDao;
import net.haugr.cws.core.model.entities.SignatureEntity;

/**
 * <p>Business Logic implementation for the CWS Sign request.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class SignManager extends AbstractManager<SignatureDao, SignResponse, SignRequest> {

    public SignManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new SignatureDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SignResponse perform(final SignRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.CREATE_SIGNATURE);
        Arrays.fill(request.getCredential(), (byte) 0);

        final SignResponse response = new SignResponse();

        final byte[] rawSignature = crypto.sign(keyPair.getPrivate().getKey(), request.getData());
        final String signature = Base64.getEncoder().encodeToString(rawSignature);
        final String checksum = crypto.generateChecksum(rawSignature);
        final SignatureEntity existing = dao.findByChecksum(checksum);

        if (existing == null) {
            final SignatureEntity entity = new SignatureEntity();
            entity.setPublicKey(member.getPublicKey());
            entity.setMember(member);
            entity.setChecksum(checksum);
            entity.setExpires(request.getExpires());
            dao.save(entity);
            response.setReturnMessage("The document was successfully signed.");
        } else {
            response.setReturnMessage("This document has already been signed.");
        }

        response.setSignature(signature);
        return response;
    }
}
