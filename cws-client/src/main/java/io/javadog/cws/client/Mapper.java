/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.dtos.DataType;
import io.javadog.cws.api.dtos.Member;
import io.javadog.cws.api.dtos.Metadata;
import io.javadog.cws.api.dtos.Sanity;
import io.javadog.cws.api.dtos.Signature;
import io.javadog.cws.api.dtos.Trustee;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.ws.CwsResult;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Mapper {

    private static final String NAMESPACE = "";

    /**
     * Private Constructor, this is a utility Class.
     */
    private Mapper() {
    }

    // =========================================================================
    // Mapping of JAXBElement (nullable) Objects
    // =========================================================================

    public static JAXBElement<String> convert(final String field, final String value) {
        final QName qName = new QName(NAMESPACE, field);
        return new JAXBElement<>(qName, String.class, value);
    }

    public static JAXBElement<Integer> convert(final String field, final int value) {
        final QName qName = new QName(NAMESPACE, field);
        return new JAXBElement<>(qName, Integer.class, value);
    }

    public static JAXBElement<byte[]> convert(final String field, final byte[] value) {
        final QName qName = new QName(NAMESPACE, field);
        return new JAXBElement<>(qName, byte[].class, value);
    }

    // =========================================================================
    // Mapping of Standard Request & Response information
    // =========================================================================

    public static void fillAuthentication(final io.javadog.cws.ws.Authentication ws, final Authentication api) {
        ws.setAccountName(api.getAccountName());
        ws.setCredential(api.getCredential());
        ws.setCredentialType(map(api.getCredentialType()));
    }

    public static void fillResponse(final CwsResponse api, final CwsResult ws) {
        api.setReturnCode(ReturnCode.findReturnCode(ws.getReturnCode()));
        api.setReturnMessage(ws.getReturnMessage());
    }

    // =========================================================================
    // Mapping of Enum types
    // =========================================================================

    private static io.javadog.cws.ws.CredentialType map(final CredentialType api) {
        return (api != null) ? io.javadog.cws.ws.CredentialType.valueOf(api.name()) : null;
    }

    public static io.javadog.cws.ws.Action map(final Action api) {
        return (api != null) ? io.javadog.cws.ws.Action.valueOf(api.name()) : null;
    }

    private static TrustLevel map(final io.javadog.cws.ws.TrustLevel ws) {
        return (ws != null) ? TrustLevel.valueOf(ws.name()) : null;
    }

    public static JAXBElement<io.javadog.cws.ws.TrustLevel> map(final TrustLevel api) {
        JAXBElement<io.javadog.cws.ws.TrustLevel> ws = null;

        if (api != null) {
            final io.javadog.cws.ws.TrustLevel value = io.javadog.cws.ws.TrustLevel.valueOf(api.name());
            final QName qName = new QName(NAMESPACE, Constants.FIELD_TRUSTLEVEL);
            ws = new JAXBElement(qName, String.class, value);
        }

        return ws;
    }

    // =========================================================================
    // Mapping of Collections
    // =========================================================================

    public static List<Sanity> mapSanities(final List<io.javadog.cws.ws.Sanity> ws) {
        final List<Sanity> api = new ArrayList<>();

        if (ws != null) {
            for (final io.javadog.cws.ws.Sanity wsSanity : ws) {
                final Sanity sanity = new Sanity();
                sanity.setDataId(wsSanity.getDataId());
                sanity.setChanged(map(wsSanity.getChanged()));
                api.add(sanity);
            }
        }

        return api;
    }

    public static List<Circle> mapCircles(final List<io.javadog.cws.ws.Circle> ws) {
        final List<Circle> api = new ArrayList<>();

        if (ws != null) {
            for (final io.javadog.cws.ws.Circle wsCircle : ws) {
                final Circle circle = new Circle();
                circle.setCircleId(wsCircle.getCircleId());
                circle.setCircleName(wsCircle.getCircleName());
                circle.setCircleKey(wsCircle.getCircleKey().getValue());
                circle.setAdded(map(wsCircle.getAdded()));
                api.add(circle);
            }
        }

        return api;
    }

    public static List<DataType> mapDataTypes(final Iterable<io.javadog.cws.ws.DataType> ws) {
        final List<DataType> api = new ArrayList<>();

        for (final io.javadog.cws.ws.DataType wsDataType : ws) {
            api.add(map(wsDataType));
        }

        return api;
    }

    public static List<Member> mapMembers(final Iterable<io.javadog.cws.ws.Member> ws) {
        final List<Member> api = new ArrayList<>();

        for (final io.javadog.cws.ws.Member wsMember : ws) {
            final Member member = new Member();
            member.setMemberId(wsMember.getMemberId());
            member.setAccountName(wsMember.getAccountName());
            member.setPublicKey(wsMember.getPublicKey());
            member.setAdded(map(wsMember.getAdded()));
            api.add(member);
        }

        return api;
    }

    public static List<Metadata> mapMetadata(final Iterable<io.javadog.cws.ws.Metadata> ws) {
        final List<Metadata> api = new ArrayList<>();

        for (final io.javadog.cws.ws.Metadata wsMetadata : ws) {
            final Metadata metadata = new Metadata();
            metadata.setDataId(wsMetadata.getDataId());
            metadata.setCircleId(wsMetadata.getCircleId());
            metadata.setFolderId(wsMetadata.getFolderId());
            metadata.setDataName(wsMetadata.getDataName());
            metadata.setTypeName(wsMetadata.getTypeName());
            metadata.setAdded(map(wsMetadata.getAdded()));

            api.add(metadata);
        }

        return api;
    }

    public static List<Signature> mapSignatures(final Iterable<io.javadog.cws.ws.Signature> ws) {
        final List<Signature> api = new ArrayList<>();

        for (final io.javadog.cws.ws.Signature wsSignature : ws) {
            final Signature signature = new Signature();
            signature.setChecksum(wsSignature.getChecksum());
            signature.setExpires(map(wsSignature.getExpires()));
            signature.setVerifications(wsSignature.getVerifications());
            signature.setLastVerification(map(wsSignature.getLastVerification()));
            signature.setAdded(map(wsSignature.getAdded()));

            api.add(signature);
        }

        return api;
    }

    public static List<Trustee> mapTrustees(final Iterable<io.javadog.cws.ws.Trustee> ws) {
        final List<Trustee> api = new ArrayList<>();

        for (final io.javadog.cws.ws.Trustee wsTrustee : ws) {
            final Trustee trustee = new Trustee();
            trustee.setMemberId(wsTrustee.getMemberId());
            trustee.setPublicKey(wsTrustee.getPublicKey());
            trustee.setCircleId(wsTrustee.getCircleId());
            trustee.setTrustLevel(map(wsTrustee.getTrustLevel()));
            trustee.setChanged(map(wsTrustee.getChanged()));
            trustee.setAdded(map(wsTrustee.getAdded()));
            api.add(trustee);
        }

        return api;
    }

    // =========================================================================
    // Mapping of Internal Object Types
    // =========================================================================

    public static DataType map(final io.javadog.cws.ws.DataType ws) {
        DataType api = null;

        if (ws != null) {
            api = new DataType();
            api.setTypeName(ws.getTypeName());
            api.setType(ws.getType());
        }

        return api;
    }

    private static Date map(final XMLGregorianCalendar ws) {
        Date api = null;

        if (ws != null) {
            api = ws.toGregorianCalendar().getTime();
        }

        return api;
    }

    public static XMLGregorianCalendar map(final Date api) {
        XMLGregorianCalendar ws = null;

        if (api != null) {
            final GregorianCalendar calendar = new GregorianCalendar();
            // Throws a NullPointerException without the null check
            calendar.setTime(api);

            try {
                ws = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
            } catch (DatatypeConfigurationException e) {
                throw new CWSClientException(e);
            }
        }

        return ws;
    }
}
