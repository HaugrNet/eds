package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Authentication;
import io.javadog.cws.api.dtos.Member;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "processMemberRequest", propOrder = { "action", "member" })
public final class ProcessMemberRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final Set<Action> ALLOWED = EnumSet.of(Action.PROCESS, Action.DELETE, Action.REKEY);

    private static final String FIELD_ACTION = "action";
    private static final String FIELD_MEMBER = "member";

    @XmlElement(required = true) private Action action = null;
    @XmlElement(required = true) private Member member = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    /**
     * <p>Sets the Action for the processing Request. Allowed values from the
     * Actions enumerated Object includes:</p>
     * <ul>
     *   <li>PROCESS</li>
     *   <li>DELETE</li>
     *   <li>REKEY</li>
     * </ul>
     *
     * @param action Current Action
     * @throws IllegalArgumentException if the value is null or not allowed
     */
    @NotNull
    public void setAction(final Action action) {
        ensureNotNull(FIELD_ACTION, action);
        ensureValidEntry(FIELD_ACTION, action, ALLOWED);
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

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

        if (action == null) {
            errors.put(FIELD_ACTION, "No action has been provided.");
        }

        if (member == null) {
            errors.put(FIELD_MEMBER, "Value is missing, null or invalid.");
        } else {
            errors.putAll(member.validate());
        }

        return errors;
    }
}
