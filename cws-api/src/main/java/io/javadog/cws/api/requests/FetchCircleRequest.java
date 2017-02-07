package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Authentication;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fetchCircleRequest", propOrder = "circleId")
public final class FetchCircleRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = "circleId", required = true, nillable = true)
    private String circleId = null;

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    public void setCircleId(final String circleId) {
        this.circleId = circleId;
    }

    public String getCircleId() {
        return circleId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        return new HashMap<>();
    }
}
