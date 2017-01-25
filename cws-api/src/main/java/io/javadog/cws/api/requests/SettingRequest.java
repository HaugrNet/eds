package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Authentication;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "settingRequest", propOrder = "settings")
public final class SettingRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @XmlElement(name = "settings", required = true)
    private Map<String, String> settings = null;

    public void setSettings(final Map<String, String> settings) {
        this.settings = Collections.unmodifiableMap(settings);
    }

    public Map<String, String> getSettings() {
        return (settings != null) ? Collections.unmodifiableMap(settings) : new HashMap<>();
    }
}
