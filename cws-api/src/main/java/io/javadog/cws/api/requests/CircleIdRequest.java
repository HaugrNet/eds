/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

/**
 * Common Interface, used by all Objects which has a CircleId.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public interface CircleIdRequest {

    void setCircleId(String circleId);

    String getCircleId();
}
