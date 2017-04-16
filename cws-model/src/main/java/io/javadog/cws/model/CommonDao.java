/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-model)
 * =============================================================================
 */
package io.javadog.cws.model;

import io.javadog.cws.model.entities.CWSEntity;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.entities.SettingEntity;
import io.javadog.cws.model.entities.TrusteeEntity;

import java.util.List;

/**
 * Common DAO Class for CWS.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public interface CommonDao {

    <E extends CWSEntity> E persist(E entity);

    <E extends CWSEntity> E find(Class<E> cwsEntity, Long id);

    MemberEntity findMemberByName(String name);

    List<TrusteeEntity> findTrustByMember(MemberEntity member);

    List<SettingEntity> readSettings();

    List<TrusteeEntity> findTrusteesByCircle(String externalCircleId);

    List<TrusteeEntity> findTrusteesByCircle(Long circleId);

    List<CircleEntity> findAllCircles();

    List<MemberEntity> findAllMembers();

    MemberEntity findMemberByExternalId(String externalId);

    List<CircleEntity> findCirclesForMember(MemberEntity requested);

    List<CircleEntity> findCirclesBothBelongTo(MemberEntity member, MemberEntity requested);
}
