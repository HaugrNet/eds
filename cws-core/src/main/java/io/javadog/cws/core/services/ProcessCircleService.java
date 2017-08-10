/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.entities.CircleEntity;

import javax.persistence.EntityManager;
import java.util.Objects;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessCircleService extends Serviceable<ProcessCircleResponse, ProcessCircleRequest> {

    public ProcessCircleService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessCircleResponse perform(final ProcessCircleRequest request) {
        verifyRequest(request, Permission.PROCESS_CIRCLE, readExternalCircleId(request));
        final ProcessCircleResponse response;

        switch (request.getAction()) {
            case CREATE:
                response = createCircle(request);
                break;
            case UPDATE:
                response = updateCircle(request);
                break;
            case DELETE:
                response = deleteCircle(request);
                break;
            case ADD:
                response = addTrustee(request);
                break;
            case ALTER:
                response = alterTrustee(request);
                break;
            case REMOVE:
                response = removeTrustee(request);
                break;
            default:
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "Unsupported Action.");
        }

        return response;
    }

    private static String readExternalCircleId(final ProcessCircleRequest request) {
        String circleId = null;

        if (request != null) {
            circleId = request.getCircleId();
        }

        return circleId;
    }

    private ProcessCircleResponse createCircle(ProcessCircleRequest request) {
        // TODO when creating a new Circle, there should also be added a Folder with name "/" for the data as root

        return null;
    }

    /**
     * <p>Updating a Circle, means changing the name of it, as this is the only
     * valid information it has. Both the System Administrator and Circle
     * Administrator is allowed to perform this action. And as the initial
     * checks already have verified that it is either of the Administrators,
     * there are no other permission checks required.</p>
     *
     * <p>Only checks needed, is to verify that the name is not already taken
     * by any other circle.</p>
     *
     * @param request Request Object with Id and new Name for the Circle
     * @return Response Object with the changed information
     */
    private ProcessCircleResponse updateCircle(final ProcessCircleRequest request) {
        final String externalId = request.getCircleId();
        final String name = request.getCircleName();
        final ProcessCircleResponse response;

        final CircleEntity existing = dao.findCircleByName(name);

        if (existing == null) {
            final CircleEntity entity = dao.find(CircleEntity.class, externalId);
            entity.setName(name);
            dao.persist(entity);

            response = new ProcessCircleResponse();
        } else {
            response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "A Circle with the requested name already exists.");
        }

        return response;
    }

    /**
     * <p>Deleting an existing Circle, is an irreversible process, by which all
     * the data, keys & trustees will also be deleted alongside the Circle.</p>
     *
     * <p>The operation can only be performed by the System Administrator, due
     * to the nature of it.</p>
     *
     * @param request Request Object with the Id of the Circle to delete
     * @return Response Object with error information
     */
    private ProcessCircleResponse deleteCircle(final ProcessCircleRequest request) {
        final ProcessCircleResponse response;

        if (Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
            final String externalId = request.getCircleId();
            final CircleEntity entity = dao.find(CircleEntity.class, externalId);
            dao.delete(entity);

            response = new ProcessCircleResponse();
        } else {
            response = new ProcessCircleResponse(ReturnCode.AUTHORIZATION_WARNING, "Only the System Administrator may delete a Circle.");
        }

        return response;
    }

    private ProcessCircleResponse addTrustee(ProcessCircleRequest request) {
        return null;
    }

    private ProcessCircleResponse alterTrustee(ProcessCircleRequest request) {
        return null;
    }

    private ProcessCircleResponse removeTrustee(ProcessCircleRequest request) {
        return null;
    }
}
