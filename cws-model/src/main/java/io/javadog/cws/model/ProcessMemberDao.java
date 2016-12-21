package io.javadog.cws.model;

import io.javadog.cws.model.entities.MemberEntity;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public interface ProcessMemberDao {

    MemberEntity findMemberByName(String name);
}
