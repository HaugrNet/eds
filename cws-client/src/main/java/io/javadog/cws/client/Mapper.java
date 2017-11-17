/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.ws.CwsResult;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
class Mapper {

    public static void fillAuthentication(final io.javadog.cws.ws.Authentication ws, final Authentication api) {
        ws.setAccountName(api.getAccountName());
        ws.setCredential(api.getCredential());
        ws.setCredentialType(map(api.getCredentialType()));
    }

    private static io.javadog.cws.ws.CredentialType map(final CredentialType api) {
        return (api != null) ? io.javadog.cws.ws.CredentialType.valueOf(api.name()) : null;
    }

    public static io.javadog.cws.ws.Action map(final Action api) {
        return (api != null) ? io.javadog.cws.ws.Action.valueOf(api.name()) : null;
    }

    public static void fillResponse(final CwsResponse api, final CwsResult ws) {
        api.setReturnCode(map(ws.getReturnCode()));
        api.setReturnMessage(ws.getReturnMessage());
    }

    private static ReturnCode map(final io.javadog.cws.ws.ReturnCode ws) {
        return (ws != null) ? ReturnCode.valueOf(ws.name()) : null;
    }

    public static TrustLevel map(final io.javadog.cws.ws.TrustLevel ws) {
        return (ws != null) ? TrustLevel.valueOf(ws.name()) : null;
    }

    public static io.javadog.cws.ws.TrustLevel map(final TrustLevel api) {
        return (api != null) ? io.javadog.cws.ws.TrustLevel.valueOf(api.name()) : null;
    }

    public static Date map(final XMLGregorianCalendar ws) {
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
                throw new RuntimeException(e);
            }
        }

        return ws;
    }
}
