package io.javadog.cws.api.responses;

import io.javadog.cws.api.common.Constants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Collections;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "settingResponse", propOrder = "settings")
public final class SettingResponse extends CWSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = 8868831828030258226L;

    @XmlElement(name = "settings", required = true)
    private Map<String, String> settings = null;

    public SettingResponse() {
        // Empty Constructor, required for WebServices
    }

    public SettingResponse(final int returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    public void setSettings(final Map<String, String> settings) {
        this.settings = Collections.unmodifiableMap(settings);
    }

    public Map<String, String> getSettings() {
        return Collections.unmodifiableMap(settings);
    }
}
