package io.javadog.cws.api.common;

import javax.xml.bind.annotation.XmlType;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlType(name = "trustLevel")
public enum TrustLevel {

    NONE,
    READ,
    WRITE,
    ADMIN
}
