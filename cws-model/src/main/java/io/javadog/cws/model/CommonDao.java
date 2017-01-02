package io.javadog.cws.model;

import io.javadog.cws.model.entities.CWSEntity;
import io.javadog.cws.model.entities.MemberEntity;

/**
 * Common DAO Class for CWS.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public interface CommonDao {

    <E extends CWSEntity> E persist(E entity);
    MemberEntity findMemberByNameCredential(String name);
}
