package io.javadog.cws.model;

import io.javadog.cws.model.entities.CWSEntity;
import io.javadog.cws.model.entities.MemberEntity;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public interface ProcessMemberDao {

    <E extends CWSEntity> E persist(E entity);
    MemberEntity findMemberByName(String name);
}
