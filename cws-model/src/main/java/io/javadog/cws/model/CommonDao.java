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
import io.javadog.cws.model.entities.DataEntity;
import io.javadog.cws.model.entities.DataTypeEntity;
import io.javadog.cws.model.entities.Externable;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.entities.MetadataEntity;
import io.javadog.cws.model.entities.SettingEntity;
import io.javadog.cws.model.entities.SignatureEntity;
import io.javadog.cws.model.entities.TrusteeEntity;

import java.util.List;

/**
 * Common DAO Class for CWS.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public interface CommonDao {

    void persist(CWSEntity entity);

    <E extends CWSEntity> E find(Class<E> cwsEntity, Long id);

    <E extends Externable> E find(Class<E> cwsEntity, String externalId);

    <E extends CWSEntity> E getReference(Class<E> cwsEntity, Long id);

    <E extends CWSEntity> void delete(E entity);

    MemberEntity findMemberByName(String name);

    MemberEntity findMemberByNameAndCircleId(String name, String externalCircleId);

    List<TrusteeEntity> findTrustByMember(MemberEntity member);

    List<TrusteeEntity> findTrustByMemberAndCircle(MemberEntity member, String externalCircleId);

    List<SettingEntity> readSettings();

    List<TrusteeEntity> findTrusteesByCircle(CircleEntity circle);

    List<CircleEntity> findAllCircles();

    List<MemberEntity> findAllMembers();

    CircleEntity findCircleByName(String name);

    List<CircleEntity> findCirclesForMember(MemberEntity requested);

    List<CircleEntity> findCirclesBothBelongTo(MemberEntity member, MemberEntity requested);

    List<DataTypeEntity> findAllTypes();

    List<DataTypeEntity> findMatchingDataTypes(String name);

    /**
     * Finds a unique DataType in the system. If none exist or it is not
     * possible to find a unique record, then an Exception is thrown.
     *
     * @param name Name of the DataType to find
     * @return Found DataType Entity
     * @throws io.javadog.cws.common.exceptions.CWSException if no unique value could be found
     */
    DataTypeEntity findDataTypeByName(String name);

    long countObjectTypeUsage(Long id);

    DataEntity findDataByMetadata(MetadataEntity metadata);

    DataEntity findDataByMemberAndExternalId(MemberEntity member, String externalId);

    MetadataEntity findMetaDataByMemberAndExternalId(MemberEntity member, String externalId);

    MetadataEntity findRootByMemberCircle(MemberEntity member, String circleId);

    List<MetadataEntity> findMetadataByMemberAndFolder(MemberEntity member, MetadataEntity folder, int pageNumber, int pageSize);

    SignatureEntity findByChecksum(String checksum);

    List<SignatureEntity> findAllSignatures(Long id);

    TrusteeEntity findTrusteeByCircleAndMember(String externalCircleId, String externalMemberId);

    long countFolderContent(MetadataEntity entity);

    MetadataEntity findInFolder(MemberEntity member, MetadataEntity folder, String name);
}
