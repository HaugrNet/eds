package io.javadog.cws.api;

import io.javadog.cws.api.dtos.Member;
import io.javadog.cws.api.requests.FetchFolderRequest;
import io.javadog.cws.api.requests.FetchObjectRequest;
import io.javadog.cws.api.requests.FetchObjectTypesRequest;
import io.javadog.cws.api.requests.ProcessFolderRequest;
import io.javadog.cws.api.requests.ProcessObjectRequest;
import io.javadog.cws.api.requests.ProcessObjectTypeRequest;
import io.javadog.cws.api.responses.FetchFolderResponse;
import io.javadog.cws.api.responses.FetchObjectResponse;
import io.javadog.cws.api.responses.FetchObjectTypesResponse;
import io.javadog.cws.api.responses.ProcessFolderResponse;
import io.javadog.cws.api.responses.ProcessObjectResponse;
import io.javadog.cws.api.responses.ProcessObjectTypeResponse;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public interface Share {

    ProcessFolderResponse processFolder(Member member, ProcessFolderRequest request);
    FetchFolderResponse fetchFolder(Member member, FetchFolderRequest request);

    ProcessObjectTypeResponse processObjectType(Member member, ProcessObjectTypeRequest request);
    FetchObjectTypesResponse fetchObjectTypes(Member member, FetchObjectTypesRequest request);

    ProcessObjectResponse processObject(Member member, ProcessObjectRequest request);
    FetchObjectResponse fetchObject(Member member, FetchObjectRequest request);
}
