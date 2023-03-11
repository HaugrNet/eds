/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
package net.haugr.eds.core.model;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import net.haugr.eds.core.model.entities.TrusteeEntity;

/**
 * <p>Data Access Object functionality used explicitly for the fetching &amp;
 * processing of Trustees.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.1
 */
public final class TrusteeDao extends CommonDao {

    public TrusteeDao(final EntityManager entityManager) {
        super(entityManager);
    }

    public List<TrusteeEntity> findTrusteesByMemberAndCircle(final String externalMemberId, final String externalCircleId) {
        final Query query = entityManager
                .createNamedQuery("trustee.findByCircleAndMember")
                .setParameter("emid", externalMemberId)
                .setParameter("ecid", externalCircleId);

        return findList(query);
    }

    public List<TrusteeEntity> findTrusteesByMember(final String externalMemberId) {
        final Query query = entityManager
                .createNamedQuery("trustee.findByExternalMemberId")
                .setParameter("externalMemberId", externalMemberId);

        return findList(query);
    }

    public List<TrusteeEntity> findTrusteesByCircle(final String externalCircleId) {
        final Query query = entityManager
                .createNamedQuery("trustee.findByExternalCircleId")
                .setParameter("externalCircleId", externalCircleId);

        return findList(query);
    }
}
