package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Authenticate;
import io.javadog.cws.api.dtos.Member;

import javax.validation.constraints.NotNull;
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
@XmlType(name = "member", propOrder = "member")
public final class ProcessMemberRequest extends Authenticate {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final String FIELD_MEMBER = "member";

    @XmlElement(required = true) private Member member = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    @NotNull
    public void setMember(final Member member) {
        ensureNotNull(FIELD_MEMBER, member);
        this.member = member;
    }

    public Member getMember() {
        return member;
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

        if (member == null) {
            errors.put(FIELD_MEMBER, "Value is missing, null or invalid.");
        } else {
            errors.putAll(member.validate());
        }

        return errors;
    }
}
