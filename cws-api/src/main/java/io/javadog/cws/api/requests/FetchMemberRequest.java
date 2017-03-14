package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Authentication;

import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "fetchMemberRequest", propOrder = "memberId")
public final class FetchMemberRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private static final String FIELD_MEMBER_ID = "memberId";

    @XmlElement(name = FIELD_MEMBER_ID, required = true, nillable = true)
    private String memberId = null;

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    @Pattern(regexp = Constants.ID_PATTERN_REGEX)
    public void setMemberId(final String memberId) {
        ensureValidId(FIELD_MEMBER_ID, memberId);
        this.memberId = memberId;
    }

    public String getMemberId() {
        return memberId;
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        final Map<String, String> errors = super.validate();

        if (memberId != null) {
            checkPattern(errors, FIELD_MEMBER_ID, memberId, Constants.ID_PATTERN_REGEX, "The Member Id is invalid.");
        }

        return errors;
    }
}
