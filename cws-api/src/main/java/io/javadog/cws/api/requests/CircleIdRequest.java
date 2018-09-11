/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
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
