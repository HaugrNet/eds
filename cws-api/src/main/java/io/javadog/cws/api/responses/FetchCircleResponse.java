package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.dtos.Trustee;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fetchCircleResponse", propOrder = "circles")
public final class FetchCircleResponse extends CWSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private final List<Circle> circles = new ArrayList<>(0);
    private final List<Trustee> trustees = new ArrayList<>(0);

    public FetchCircleResponse() {
        // Empty Constructor, required for WebServices
    }

    public FetchCircleResponse(final int returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    public void setCircles(final List<Circle> circles) {
        this.circles.addAll(circles);
    }

    public List<Circle> getCircles() {
        return Collections.unmodifiableList(circles);
    }

    public void setTrustees(final List<Trustee> trustees) {
        this.trustees.addAll(trustees);
    }

    public List<Trustee> getTrustees() {
        return Collections.unmodifiableList(trustees);
    }
}
