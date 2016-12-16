package io.javadog.cws.core;

import io.javadog.cws.api.requests.FetchFolderRequest;
import io.javadog.cws.api.requests.FetchObjectRequest;
import io.javadog.cws.api.requests.FetchObjectTypeRequest;
import io.javadog.cws.api.requests.ProcessObjectRequest;
import io.javadog.cws.api.requests.ProcessObjectTypeRequest;
import io.javadog.cws.api.responses.FetchFolderResponse;
import io.javadog.cws.api.responses.FetchObjectResponse;
import io.javadog.cws.api.responses.FetchObjectTypeResponse;
import io.javadog.cws.api.responses.ProcessObjectResponse;
import io.javadog.cws.api.responses.ProcessObjectTypeResponse;
import io.javadog.cws.core.services.FetchFolderService;
import io.javadog.cws.core.services.FetchObjectService;
import io.javadog.cws.core.services.FetchObjectTypeService;
import io.javadog.cws.core.services.ProcessFolderService;
import io.javadog.cws.core.services.ProcessObjectService;
import io.javadog.cws.core.services.ProcessObjectTypeService;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ShareServiceFactory {

    public ProcessFolderService prepareProcessFolderService() {
        return new ProcessFolderService();
    }

    public Servicable<FetchFolderResponse, FetchFolderRequest> prepareFetchFolderService() {
        return new FetchFolderService();
    }

    public Servicable<ProcessObjectTypeResponse, ProcessObjectTypeRequest> prepareProcessObjectTypeService() {
        return new ProcessObjectTypeService();
    }

    public Servicable<FetchObjectTypeResponse, FetchObjectTypeRequest> prepareFetchObjectTypeService() {
        return new FetchObjectTypeService();
    }

    public Servicable<ProcessObjectResponse, ProcessObjectRequest> prepareProcessObjectService() {
        return new ProcessObjectService();
    }

    public Servicable<FetchObjectResponse, FetchObjectRequest> prepareFetchObjectService() {
        return new FetchObjectService();
    }
}
