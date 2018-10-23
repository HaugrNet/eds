/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2018, JavaDog.io
 * mailto: cws AT JavaDog DOT io
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
package io.javadog.cws.core.model;

import io.javadog.cws.core.model.entities.CircleEntity;
import io.javadog.cws.core.model.entities.MemberEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

/**
 * <p>Data Access Object functionality used explicitly for the fetching &amp;
 * processing of Members.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.1
 */
public final class MemberDao extends CommonDao {

    public MemberDao(final EntityManager entityManager) {
        super(entityManager);
    }

    public List<CircleEntity> findCirclesForMember(final MemberEntity member) {
        final Query query = entityManager.createNamedQuery("trustee.findCirclesByMember");
        query.setParameter(MEMBER, member);

        return findList(query);
    }

    public List<TrusteeEntity> findCirclesBothBelongTo(final MemberEntity member,
                                                       final MemberEntity requested) {
        final Query query = entityManager.createNamedQuery("trustee.findSharedCircles");
        query.setParameter(MEMBER, member);
        query.setParameter("requested", requested);

        return findList(query);
    }
}
