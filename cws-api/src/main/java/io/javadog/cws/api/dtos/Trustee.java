package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.TrustLevel;

import javax.xml.bind.annotation.XmlElement;
import java.util.Date;

/**
 * <p>A Trustee, is a Member of a Circle, with a granted Trust Level.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Trustee {

    @XmlElement                  private String id = null;
    @XmlElement(required = true) private Circle circle = null;
    @XmlElement(required = true) private Member member = null;
    @XmlElement(required = true) private TrustLevel trustLevel = null;
    @XmlElement                  private Date modified = null;
    @XmlElement                  private Date since = null;
}
