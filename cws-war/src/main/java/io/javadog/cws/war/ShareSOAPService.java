package io.javadog.cws.war;

import io.javadog.cws.api.Share;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.FetchFolderRequest;
import io.javadog.cws.api.requests.FetchObjectRequest;
import io.javadog.cws.api.requests.FetchObjectTypeRequest;
import io.javadog.cws.api.requests.ProcessFolderRequest;
import io.javadog.cws.api.requests.ProcessObjectRequest;
import io.javadog.cws.api.requests.ProcessObjectTypeRequest;
import io.javadog.cws.api.responses.FetchFolderResponse;
import io.javadog.cws.api.responses.FetchObjectResponse;
import io.javadog.cws.api.responses.FetchObjectTypeResponse;
import io.javadog.cws.api.responses.ProcessFolderResponse;
import io.javadog.cws.api.responses.ProcessObjectResponse;
import io.javadog.cws.api.responses.ProcessObjectTypeResponse;
import io.javadog.cws.core.ShareServiceFactory;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Servicable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.jws.WebService;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@WebService
public final class ShareSOAPService implements Share {

    private static final Logger log = LoggerFactory.getLogger(ShareSOAPService.class);

    private static final String GENERAL_RETURN_MESSAGE = "An unknown error occurred. Please consult the CWS System Log.";

    private ShareServiceFactory factory = null;

    @PostConstruct
    public void init() {
        factory = new ShareServiceFactory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessFolderResponse processFolder(final ProcessFolderRequest request) {
        ProcessFolderResponse response;

        try {
            final Servicable<ProcessFolderResponse, ProcessFolderRequest> service = factory.prepareProcessFolderService();
            response = service.process(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new ProcessFolderResponse(e.getReturnCode(), e.getMessage());
        } catch (RuntimeException e) {
            // If an error occurs at this level, which has not been caught
            // earlier, then it is either a programming error or a problem with
            // the container itself. Please consult the log file for more
            // information.
            log.error(e.getMessage(), e);
            response = new ProcessFolderResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchFolderResponse fetchFolder(final FetchFolderRequest request) {
        FetchFolderResponse response;

        try {
            final Servicable<FetchFolderResponse, FetchFolderRequest> service = factory.prepareFetchFolderService();
            response = service.process(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new FetchFolderResponse(e.getReturnCode(), e.getMessage());
        } catch (RuntimeException e) {
            // If an error occurs at this level, which has not been caught
            // earlier, then it is either a programming error or a problem with
            // the container itself. Please consult the log file for more
            // information.
            log.error(e.getMessage(), e);
            response = new FetchFolderResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessObjectTypeResponse processObjectType(final ProcessObjectTypeRequest request) {
        ProcessObjectTypeResponse response;

        try {
            final Servicable<ProcessObjectTypeResponse, ProcessObjectTypeRequest> service = factory.prepareProcessObjectTypeService();
            response = service.process(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new ProcessObjectTypeResponse(e.getReturnCode(), e.getMessage());
        } catch (RuntimeException e) {
            // If an error occurs at this level, which has not been caught
            // earlier, then it is either a programming error or a problem with
            // the container itself. Please consult the log file for more
            // information.
            log.error(e.getMessage(), e);
            response = new ProcessObjectTypeResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchObjectTypeResponse fetchObjectTypes(final FetchObjectTypeRequest request) {
        FetchObjectTypeResponse response;

        try {
            final Servicable<FetchObjectTypeResponse, FetchObjectTypeRequest> service = factory.prepareFetchObjectTypeService();
            response = service.process(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new FetchObjectTypeResponse(e.getReturnCode(), e.getMessage());
        } catch (RuntimeException e) {
            // If an error occurs at this level, which has not been caught
            // earlier, then it is either a programming error or a problem with
            // the container itself. Please consult the log file for more
            // information.
            log.error(e.getMessage(), e);
            response = new FetchObjectTypeResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessObjectResponse processObject(final ProcessObjectRequest request) {
        ProcessObjectResponse response;

        try {
            final Servicable<ProcessObjectResponse, ProcessObjectRequest> service = factory.prepareProcessObjectService();
            response = service.process(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new ProcessObjectResponse(e.getReturnCode(), e.getMessage());
        } catch (RuntimeException e) {
            // If an error occurs at this level, which has not been caught
            // earlier, then it is either a programming error or a problem with
            // the container itself. Please consult the log file for more
            // information.
            log.error(e.getMessage(), e);
            response = new ProcessObjectResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchObjectResponse fetchObject(final FetchObjectRequest request) {
        FetchObjectResponse response;

        try {
            final Servicable<FetchObjectResponse, FetchObjectRequest> service = factory.prepareFetchObjectService();
            response = service.process(request);
        } catch (CWSException e) {
            // Any Warning or Error thrown by the CWS contain enough information
            // so it can be dealt with by the requesting System. Logging the
            // error is thus not needed, as all information is provided in the
            // response.
            log.trace(e.getMessage(), e);
            response = new FetchObjectResponse(e.getReturnCode(), e.getMessage());
        } catch (RuntimeException e) {
            // If an error occurs at this level, which has not been caught
            // earlier, then it is either a programming error or a problem with
            // the container itself. Please consult the log file for more
            // information.
            log.error(e.getMessage(), e);
            response = new FetchObjectResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }
}
