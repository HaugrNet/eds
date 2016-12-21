package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.model.ProcessMemberDao;
import io.javadog.cws.model.entities.MemberEntity;

import java.util.Objects;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessMemberService extends Servicable<ProcessMemberResponse, ProcessMemberRequest> {

    private final ProcessMemberDao dao;

    public ProcessMemberService(final ProcessMemberDao dao) {
        this.dao = dao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessMemberResponse process(final ProcessMemberRequest request) {
        verify(request);

        final MemberEntity admin = checkAdminForInitialLogin(request);

        return new ProcessMemberResponse();
    }

    /**
     * <p>Checks the System or Circle Administrator Login, to see if a valid
     * Account exist, if not - then a second check is made, to see if this is a
     * new setup where the System Administrator Account has not yet been
     * initialized.</p>
     *
     * @param request Process Member Request Object with Account Credentials
     * @return Circle or System Administrator Account
     * @throws ModelException if no Account could be found
     */
    private MemberEntity checkAdminForInitialLogin(final ProcessMemberRequest request) {
        MemberEntity entity;

        try {
            entity = dao.findMemberByName(request.getName());
        } catch (ModelException e) {
            if (Objects.equals(Constants.ADMIN_ACCOUNT, request.getName()) && (e.getReturnCode() == Constants.IDENTIFICATION_WARNING)) {
                // First login
                entity = createNewAdmin(request);
            } else {
                throw e;
            }
        }

        return entity;
    }

    private MemberEntity createNewAdmin(final ProcessMemberRequest request) {
        return null;
    }
}
