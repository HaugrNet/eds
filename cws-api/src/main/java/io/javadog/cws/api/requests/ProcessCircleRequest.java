/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Authentication;
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.dtos.Trustee;

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
@XmlType(name = "processCircleRequest", propOrder = { "action", "circle", "trustee" })
public final class ProcessCircleRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final Set<Action> ALLOWED = EnumSet.of(Action.PROCESS, Action.ADD, Action.ALTER, Action.REMOVE, Action.DELETE);

    private static final String FIELD_ACTION = "action";
    private static final String FIELD_CIRCLE = "circle";
    private static final String FIELD_TRUSTEE = "trustee";

    @XmlElement(required = true)
    private Action action = null;

    @XmlElement(required = true)
    private Circle circle = null;

    @XmlElement(name = "FIELD_TRUSTEE", required = true, nillable = true)
    private Trustee trustee = null;

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    /**
     * <p>Sets the Action for the processing Request. Allowed values from the
     * Actions enumerated Object includes:</p>
     * <ul>
     *   <li>PROCESS</li>
     *   <li>DELETE</li>
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

    /**
     * <p>Sets the Circle to Process, i.e. either Create if it doesn't exist or
     * update if possible, depending on the Action.</p>
     *
     * @param circle Circle to Process
     */
    @NotNull
    public void setCircle(final Circle circle) {
        ensureNotNull(FIELD_CIRCLE, circle);
        ensureVerifiable(FIELD_CIRCLE, circle);
        this.circle = circle;
    }

    public Circle getCircle() {
        return circle;
    }

    public void setTrustee(final Trustee trustee) {
        ensureVerifiable(FIELD_TRUSTEE, trustee);
        this.trustee = trustee;
    }

    public Trustee getTrustee() {
        return trustee;
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

        if (circle == null) {
            errors.put(FIELD_CIRCLE, "Value is missing, null or invalid.");
        } else {
            errors.putAll(circle.validate());
        }

        if ((action != null) && ((action == Action.ADD) || (action == Action.ALTER) || (action == Action.REMOVE))) {
            if (trustee == null) {
                errors.put(FIELD_TRUSTEE, "Value is missing, null or invalid.");
            } else {
                errors.putAll(trustee.validate());
            }
        }

        return errors;
    }
}
