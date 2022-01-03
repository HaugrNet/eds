/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
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
package net.haugr.cws.core.model;

import net.haugr.cws.core.model.entities.MemberEntity;
import net.haugr.cws.core.model.entities.SignatureEntity;
import java.util.List;
import javax.persistence.EntityManager;

/**
 * <p>Data Access Object functionality used explicitly for the fetching &amp;
 * processing of Signatures.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.1
 */
public final class SignatureDao extends CommonDao {

    public SignatureDao(final EntityManager entityManager) {
        super(entityManager);
    }

    public SignatureEntity findByChecksum(final String checksum) {
        final var query = entityManager
                .createNamedQuery("signature.findByChecksum")
                .setParameter("checksum", checksum);

        return findSingleRecord(query);
    }

    public List<SignatureEntity> findAllSignatures(final MemberEntity member) {
        final var query = entityManager
                .createNamedQuery("signature.findByMember")
                .setParameter(MEMBER, member);

        return findList(query);
    }
}
