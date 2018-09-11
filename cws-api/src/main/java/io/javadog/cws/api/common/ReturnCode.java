/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.api.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Common Return Codes, used for all requests, to help specify the nature of
 * the problem which may have occurred, if processing wasn't successful.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = Constants.FIELD_RETURN_CODE)
public enum ReturnCode {

    // =========================================================================
    // From https://en.wikipedia.org/wiki/List_of_HTTP_status_codes
    // =========================================================================
    // 1xx Informational responses
    //     An informational response indicates that the request was received and
    //     understood. It is issued on a provisional basis while request
    //     processing continues. It alerts the client to wait for a final
    //     response. The message consists only of the status line and optional
    //     header fields, and is terminated by an empty line. As the HTTP/1.0
    //     standard did not define any 1xx status codes, servers must not send a
    //     1xx response to an HTTP/1.0 compliant client except under
    //     experimental conditions.
    // -------------------------------------------------------------------------
    // 100 Continue
    //     The server has received the request headers and the client should
    //     proceed to send the request body (in the case of a request for which
    //     a body needs to be sent; for example, a POST request). Sending a
    //     large request body to a server after a request has been rejected for
    //     inappropriate headers would be inefficient. To have a server check
    //     the request's headers, a client must send Expect: 100-continue as a
    //     header in its initial request and receive a 100 Continue status code
    //     in response before sending the body. If the client receives an error
    //     code such as 403 (Forbidden) or 405 (Method Not Allowed) then it
    //     shouldn't send the request's body. The response 417 Expectation
    //     Failed indicates that the request should be repeated without the
    //     Expect header as it indicates that the server doesn't support
    //     expectations (this is the case, for example, of HTTP/1.0 servers).
    // 101 Switching Protocols
    //     The requester has asked the server to switch protocols and the server
    //     has agreed to do so.
    // 102 Processing (WebDAV; RFC 2518)
    //     A WebDAV request may contain many sub-requests involving file
    //     operations, requiring a long time to complete the request. This code
    //     indicates that the server has received and is processing the request,
    //     but no response is available yet.[7] This prevents the client from
    //     timing out and assuming the request was lost.
    // =========================================================================
    // 2xx Success
    //     This class of status codes indicates the action requested by the
    //     client was received, understood, accepted, and processed
    //     successfully.
    // -------------------------------------------------------------------------
    // 200 OK
    //     Standard response for successful HTTP requests. The actual response
    //     will depend on the request method used. In a GET request, the
    //     response will contain an entity corresponding to the requested
    //     resource. In a POST request, the response will contain an entity
    //     describing or containing the result of the action.
    // 201 Created
    //     The request has been fulfilled, resulting in the creation of a new
    //     resource.
    // 202 Accepted
    //     The request has been accepted for processing, but the processing has
    //     not been completed. The request might or might not be eventually
    //     acted upon, and may be disallowed when processing occurs.
    // 203 Non-Authoritative Information (since HTTP/1.1)
    //     The server is a transforming proxy (e.g. a Web accelerator) that
    //     received a 200 OK from its origin, but is returning a modified
    //     version of the origin's response.
    // 204 No Content
    //     The server successfully processed the request and is not returning
    //     any content.
    // 205 Reset Content
    //     The server successfully processed the request, but is not returning
    //     any content. Unlike a 204 response, this response requires that the
    //     requester reset the document view.
    // 206 Partial Content (RFC 7233)
    //     The server is delivering only part of the resource (byte serving) due
    //     to a range header sent by the client. The range header is used by
    //     HTTP clients to enable resuming of interrupted downloads, or split a
    //     download into multiple simultaneous streams.
    // 207 Multi-Status (WebDAV; RFC 4918)
    //     The message body that follows is an XML message and can contain a
    //     number of separate response codes, depending on how many sub-requests
    //     were made.
    // 208 Already Reported (WebDAV; RFC 5842)
    //     The members of a DAV binding have already been enumerated in a
    //     preceding part of the (multistatus) response, and are not being
    //     included again.
    // 226 IM Used (RFC 3229)
    //     The server has fulfilled a request for the resource, and the response
    //     is a representation of the result of one or more
    //     instance-manipulations applied to the current instance.
    // =========================================================================
    // 3xx Redirection
    //     This class of status code indicates the client must take additional
    //     action to complete the request. Many of these status codes are used
    //     in URL redirection.
    //       A user agent may carry out the additional action with no user
    //     interaction only if the method used in the second request is GET or
    //     HEAD. A user agent may automatically redirect a request. A user agent
    //     should detect and intervene to prevent cyclical redirects.[18]
    // -------------------------------------------------------------------------
    // 300 Multiple Choices
    //     Indicates multiple options for the resource from which the client may
    //     choose (via agent-driven content negotiation). For example, this code
    //     could be used to present multiple video format options, to list files
    //     with different filename extensions, or to suggest word-sense
    //     disambiguation.
    // 301 Moved Permanently
    //     This and all future requests should be directed to the given URI.
    // 302 Found
    //     This is an example of industry practice contradicting the standard.
    //     The HTTP/1.0 specification (RFC 1945) required the client to perform
    //     a temporary redirect (the original describing phrase was "Moved
    //     Temporarily"), but popular browsers implemented 302 with the
    //     functionality of a 303 See Other. Therefore, HTTP/1.1 added status
    //     codes 303 and 307 to distinguish between the two behaviours. However,
    //     some Web applications and frameworks use the 302 status code as if it
    //     were the 303.
    // 303 See Other (since HTTP/1.1)
    //     The response to the request can be found under another URI using the
    //     GET method. When received in response to a POST (or PUT/DELETE), the
    //     client should presume that the server has received the data and
    //     should issue a new GET request to the given URI.
    // 304 Not Modified (RFC 7232)
    //     Indicates that the resource has not been modified since the version
    //     specified by the request headers If-Modified-Since or If-None-Match.
    //     In such case, there is no need to retransmit the resource since the
    //     client still has a previously-downloaded copy.
    // 305 Use Proxy (since HTTP/1.1)
    //     The requested resource is available only through a proxy, the address
    //     for which is provided in the response. Many HTTP clients (such as
    //     Mozilla and Internet Explorer) do not correctly handle responses with
    //     this status code, primarily for security reasons.
    // 306 Switch Proxy
    //     No longer used. Originally meant "Subsequent requests should use the
    //     specified proxy."
    // 307 Temporary Redirect (since HTTP/1.1)
    //     In this case, the request should be repeated with another URI:
    //     however, future requests should still use the original URI. In
    //     contrast to how 302 was historically implemented, the request method
    //     is not allowed to be changed when reissuing the original request. For
    //     example, a POST request should be repeated using another POST
    //     request.
    // 308 Permanent Redirect (RFC 7538)
    //     The request and all future requests should be repeated using another
    //     URI. 307 and 308 parallel the behaviors of 302 and 301, but do not
    //     allow the HTTP method to change. So, for example, submitting a form
    //     to a permanently redirected resource may continue smoothly.
    // =========================================================================
    //4xx Client errors
    //    This class of status code is intended for situations in which the
    //    error seems to have been caused by the client. Except when responding
    //    to a HEAD request, the server should include an entity containing an
    //    explanation of the error situation, and whether it is a temporary or
    //    permanent condition. These status codes are applicable to any request
    //    method. User agents should display any included entity to the user.
    // -------------------------------------------------------------------------
    // 400 Bad Request
    //     The server cannot or will not process the request due to an apparent
    //     client error (e.g., malformed request syntax, size too large, invalid
    //     request message framing, or deceptive request routing).
    // 401 Unauthorized (RFC 7235)
    //     Similar to 403 Forbidden, but specifically for use when
    //     authentication is required and has failed or has not yet been
    //     provided. The response must include a WWW-Authenticate header field
    //     containing a challenge applicable to the requested resource. See
    //     Basic access authentication and Digest access authentication. 401
    //     semantically means "unauthenticated", i.e. the user does not have the
    //     necessary credentials.
    //       Note: Some sites issue HTTP 401 when an IP address is banned from
    //     the website (usually the website domain) and that specific address is
    //     refused permission to access a website.
    // 402 Payment Required
    //     Reserved for future use. The original intention was that this code
    //     might be used as part of some form of digital cash or micropayment
    //     scheme, as proposed for example by GNU Taler, but that has not yet
    //     happened, and this code is not usually used. Google Developers API
    //     uses this status if a particular developer has exceeded the daily
    //     limit on requests.[36] Stripe API uses this code for errors with
    //     processing credit cards.
    // 403 Forbidden
    //     The request was valid, but the server is refusing action. The user
    //     might not have the necessary permissions for a resource, or may need
    //     an account of some sort.
    // 404 Not Found
    //     The requested resource could not be found but may be available in the
    //     future. Subsequent requests by the client are permissible.
    // 405 Method Not Allowed
    //     A request method is not supported for the requested resource; for
    //     example, a GET request on a form that requires data to be presented
    //     via POST, or a PUT request on a read-only resource.
    // 406 Not Acceptable
    //     The requested resource is capable of generating only content not
    //     acceptable according to the Accept headers sent in the request.
    // 407 Proxy Authentication Required (RFC 7235)
    //     The client must first authenticate itself with the proxy.
    // 408 Request Timeout
    //     The server timed out waiting for the request. According to HTTP
    //     specifications: "The client did not produce a request within the time
    //     that the server was prepared to wait. The client MAY repeat the
    //     request without modifications at any later time."
    // 409 Conflict
    //     Indicates that the request could not be processed because of conflict
    //     in the request, such as an edit conflict between multiple
    //     simultaneous updates.
    // 410 Gone
    //     Indicates that the resource requested is no longer available and will
    //     not be available again. This should be used when a resource has been
    //     intentionally removed and the resource should be purged. Upon
    //     receiving a 410 status code, the client should not request the
    //     resource in the future. Clients such as search engines should remove
    //     the resource from their indices.[41] Most use cases do not require
    //     clients and search engines to purge the resource, and a "404 Not
    //     Found" may be used instead.
    // 411 Length Required
    //     The request did not specify the length of its content, which is
    //     required by the requested resource.
    // 412 Precondition Failed (RFC 7232)
    //     The server does not meet one of the preconditions that the requester
    //     put on the request.
    // 413 Payload Too Large (RFC 7231)
    //     The request is larger than the server is willing or able to process.
    //     Previously called "Request Entity Too Large".
    // 414 URI Too Long (RFC 7231)
    //     The URI provided was too long for the server to process. Often the
    //     result of too much data being encoded as a query-string of a GET
    //     request, in which case it should be converted to a POST request.
    //     Called "Request-URI Too Long" previously.
    // 415 Unsupported Media Type
    //     The request entity has a media type which the server or resource does
    //     not support. For example, the client uploads an image as
    //     image/svg+xml, but the server requires that images use a different
    //     format.
    // 416 Range Not Satisfiable (RFC 7233)
    //     The client has asked for a portion of the file (byte serving), but
    //     the server cannot supply that portion. For example, if the client
    //     asked for a part of the file that lies beyond the end of the file.
    //     Called "Requested Range Not Satisfiable" previously.
    // 417 Expectation Failed
    //     The server cannot meet the requirements of the Expect request-header
    //     field.
    // 418 I'm a teapot (RFC 2324)
    //     This code was defined in 1998 as one of the traditional IETF April
    //     Fools' jokes, in RFC 2324, Hyper Text Coffee Pot Control Protocol,
    //     and is not expected to be implemented by actual HTTP servers. The RFC
    //     specifies this code should be returned by teapots requested to brew
    //     coffee. This HTTP status is used as an Easter egg in some websites,
    //     including Google.com.
    // 421 Misdirected Request (RFC 7540)
    //     The request was directed at a server that is not able to produce a
    //     response. (for example because of a connection reuse)
    // 422 Unprocessable Entity (WebDAV; RFC 4918)
    //     The request was well-formed but was unable to be followed due to
    //     semantic errors.
    // 423 Locked (WebDAV; RFC 4918)
    //     The resource that is being accessed is locked.
    // 424 Failed Dependency (WebDAV; RFC 4918)
    //     The request failed due to failure of a previous request
    //     (e.g., a PROPPATCH).
    // 426 Upgrade Required
    //     The client should switch to a different protocol such as TLS/1.0,
    //     given in the Upgrade header field.
    // 428 Precondition Required (RFC 6585)
    //     The origin server requires the request to be conditional. Intended to
    //     prevent the 'lost update' problem, where a client GETs a resource's
    //     state, modifies it, and PUTs it back to the server, when meanwhile a
    //     third party has modified the state on the server, leading to a
    //     conflict."
    // 429 Too Many Requests (RFC 6585)
    //     The user has sent too many requests in a given amount of time.
    //     Intended for use with rate-limiting schemes.
    // 431 Request Header Fields Too Large (RFC 6585)
    //     The server is unwilling to process the request because either an
    //     individual header field, or all the header fields collectively, are
    //     too large.
    // 451 Unavailable For Legal Reasons (RFC 7725)
    //     A server operator has received a legal demand to deny access to a
    //     resource or to a set of resources that includes the requested
    //     resource. The code 451 was chosen as a reference to the novel
    //     Fahrenheit 451 (see the Acknowledgements in the RFC).
    // =========================================================================
    // 5xx Server errors
    //     The server failed to fulfill a request.
    //       Response status codes beginning with the digit "5" indicate cases
    //     in which the server is aware that it has encountered an error or is
    //     otherwise incapable of performing the request. Except when responding
    //     to a HEAD request, the server should include an entity containing an
    //     explanation of the error situation, and indicate whether it is a
    //     temporary or permanent condition. Likewise, user agents should
    //     display any included entity to the user. These response codes are
    //     applicable to any request method.
    // -------------------------------------------------------------------------
    // 500 Internal Server Error
    //     A generic error message, given when an unexpected condition was
    //     encountered and no more specific message is suitable.
    // 501 Not Implemented
    //     The server either does not recognize the request method, or it lacks
    //     the ability to fulfill the request. Usually this implies future
    //     availability (e.g., a new feature of a web-service API).
    // 502 Bad Gateway
    //     The server was acting as a gateway or proxy and received an invalid
    //     response from the upstream server.
    // 503 Service Unavailable
    //     The server is currently unavailable (because it is overloaded or down
    //     for maintenance). Generally, this is a temporary state.
    // 504 Gateway Timeout
    //     The server was acting as a gateway or proxy and did not receive a
    //     timely response from the upstream server.
    // 505 HTTP Version Not Supported
    //     The server does not support the HTTP protocol version used in the
    //     request.
    // 506 Variant Also Negotiates (RFC 2295)
    //     Transparent content negotiation for the request results in a circular
    //     reference.
    // 507 Insufficient Storage (WebDAV; RFC 4918)
    //     The server is unable to store the representation needed to complete
    //     the request.
    // 508 Loop Detected (WebDAV; RFC 5842)
    //     The server detected an infinite loop while processing the request
    //     (sent in lieu of 208 Already Reported).
    // 510 Not Extended (RFC 2774)
    //     Further extensions to the request are required for the server to
    //     fulfil it.
    // 511 Network Authentication Required (RFC 6585)
    //     The client needs to authenticate to gain network access. Intended for
    //     use by intercepting proxies used to control access to the network
    //     (e.g., "captive portals" used to require agreement to Terms of
    //     Service before granting full Internet access via a Wi-Fi hotspot).
    // =========================================================================

    /**
     * <b>HTTP Status Code 200 - OK</b>.<br>
     * <i>Standard response for successful HTTP requests. The actual response
     * will depend on the request method used. In a GET request, the response
     * will contain an entity corresponding to the requested resource. In a POST
     * request, the response will contain an entity describing or containing the
     * result of the action.</i>
     */
    SUCCESS(Classification.CLASS_INFO, 0, "Request completed normally."),

    /**
     * <b>HTTP Status Code 400 - Bad Request</b>.<br>
     * <i>The server cannot or will not process the request due to an apparent
     * client error (e.g., malformed request syntax, size too large, invalid
     * request message framing, or deceptive request routing).</i>
     */
    WARNING(Classification.CLASS_WARNING, 0, "General Warning occurred while handling the request."),

    /**
     * <b>HTTP Status Code 401 - Unauthorized (RFC 7235)</b>.<br>
     * <i>Similar to 403 Forbidden, but specifically for use when authentication
     * is required and has failed or has not yet been provided. The response
     * must include a WWW-Authenticate header field containing a challenge
     * applicable to the requested resource. See Basic access authentication and
     * Digest access authentication. 401 semantically means "unauthenticated",
     * i.e. the user does not have the necessary credentials.<br>
     *   Note: Some sites issue HTTP 401 when an IP address is banned from the
     * website (usually the website domain) and that specific address is refused
     * permission to access a website.</i>
     */
    AUTHORIZATION_WARNING(Classification.CLASS_WARNING, 1, "The Account is not permitted to perform requested Action."),

    /**
     * <b>HTTP Status Code 403 - Forbidden</b>.<br>
     * <i>The request was valid, but the server is refusing action. The user
     * might not have the necessary permissions for a resource, or may need an
     * account of some sort.</i>
     */
    AUTHENTICATION_WARNING(Classification.CLASS_WARNING, 3, "Authentication of the Account failed."),

    /**
     * <b>HTTP Status Code 404 - Not Found</b>.<br>
     * <i>The requested resource could not be found but may be available in the
     * future. Subsequent requests by the client are permissible.</i>
     */
    IDENTIFICATION_WARNING(Classification.CLASS_WARNING, 4, "Not possible to positively identify the requested Data."),

    /**
     * <b>HTTP Status Code 405 - Method Not Allowed</b>.<br>
     * <i>A request method is not supported for the requested resource; for
     * example, a GET request on a form that requires data to be presented via
     * POST, or a PUT request on a read-only resource.</i>
     */
    ILLEGAL_ACTION(Classification.CLASS_WARNING, 5, "The Account tried to invoke an Action not allowed."),

    /**
     * <b>HTTP Status Code 406 - Not Acceptable</b>.<br>
     * <i>The requested resource is capable of generating only content not
     * acceptable according to the Accept headers sent in the request.</i>
     */
    VERIFICATION_WARNING(Classification.CLASS_WARNING, 6, "The provided Request information is insufficient or invalid."),

    /**
     * <b>HTTP Status Code 409 - Conflict</b>.<br>
     * <i>Indicates that the request could not be processed because of conflict
     * in the request, such as an edit conflict between multiple simultaneous
     * updates.</i>
     */
    INTEGRITY_WARNING(Classification.CLASS_WARNING, 9, "Not possible to perform the given action, as it will lead to data integrity problems."),

    /**
     * <b>Custom Warning 491 - Cryptographic Signature Problem</b>.<br>
     * <i>The Signature is not usable, as it has expired.</i>
     */
    SIGNATURE_WARNING(Classification.CLASS_WARNING, 91, "There Signature is not usable."),

    /**
     * <b>Custom Warning 492 - Setting Warning</b>.<br>
     * <i>It is not permitted to add, alter or delete the given setting.</i>
     */
    SETTING_WARNING(Classification.CLASS_WARNING, 92, "Not permitted to add, alter or delete the given Setting."),

    /**
     * <b>HTTP Status Code 500 - Internal Server Error</b>.<br>
     * <i>A generic error message, given when an unexpected condition was
     * encountered and no more specific message is suitable.</i>
     */
    ERROR(Classification.CLASS_ERROR, 0, "General Error occurred while handling the request."),

    /**
     * <b>Custom Warning 591 - General Cryptographic Error</b>.<br>
     * <i>A generic cryptographic error occurred during the request.</i>
     */
    CRYPTO_ERROR(Classification.CLASS_ERROR, 91, "Cryptographic Error occurred during the handling of the request."),

    /**
     * <b>Custom Warning 592 - Integrity Error</b>.<br>
     * <i>The encrypted data is having integrity errors.</i>
     */
    INTEGRITY_ERROR(Classification.CLASS_ERROR, 92, "The Encrypted Data is having integrity problems."),

    /**
     * <b>Custom Warning 593 - Setting Error</b>.<br>
     * <i>Error extracting the setting value.</i>
     */
    SETTING_ERROR(Classification.CLASS_ERROR, 93, "Error extracting settings value."),

    /**
     * <b>Custom Warning 594 - Identification Error</b>.<br>
     * <i>It is not possible to identify the requested data.</i>
     */
    IDENTIFICATION_ERROR(Classification.CLASS_ERROR, 94, "Not possible to positively identify the Requested Data."),

    /**
     * <b>Custom Warning 595 - Database Error</b>.<br>
     * <i>A generic problem with the database occurred.</i>
     */
    DATABASE_ERROR(Classification.CLASS_ERROR, 95, "Database Error occurred during the handling of the request."),

    /**
     * <b>Custom Warning 596 - Constraint Error</b>.<br>
     * <i>A unique constraint violation occurred in the database.</i>
     */
    CONSTRAINT_ERROR(Classification.CLASS_ERROR, 96, "Unique Constraint Violation in the Database.");

    /**
     * ReturnCode Classification, i.e. Basic Severity.
     */
    public enum Classification {

        /**
         * <p>The Classification Info, is used for for all Successful Responses
         * from the CWS.</p>
         */
        CLASS_INFO(200),

        /**
         * <p>The Classification Warning, is used for problems which prevents
         * the successful completion of a Request - but where the cause of the
         * problem is something that can be corrected by the Requesting Member.
         * This is most likely due to invalid input or insufficient input data
         * or Authentication/Authorization problems.</p>
         */
        CLASS_WARNING(400),

        /**
         * <p>The Classification Error, is used for internal problems, which
         * prevents the CWS from successfully completing the request. These
         * errors will be caused by errors in either the implementation of the
         * logic or the quality of the existing data. Generally, this kind of
         * problem cannot be correct by the Members, but require Administrative
         * intervention or corrections by the CWS Developers.</p>
         */
        CLASS_ERROR(500);

        private final int classificationCode;

        Classification(final int classificationCode) {
            this.classificationCode = classificationCode;
        }

        private int getClassificationCode() {
            return classificationCode;
        }
    }

    // =========================================================================
    // Internal functionality for the ReturnCode Enum
    // =========================================================================

    private static final int ERROR_CODE_NUMBER = 90;

    private final Classification classification;
    private final int code;
    private final String description;

    ReturnCode(final Classification classification, final int code, final String description) {
        this.classification = classification;
        this.code = code;
        this.description = description;
    }

    public Classification getClassification() {
        return classification;
    }

    public int getCode() {
        return classification.getClassificationCode() + code;
    }

    public int getHttpCode() {
        int httpCode = getCode();

        if (code > ERROR_CODE_NUMBER) {
            httpCode = classification.getClassificationCode();
        }

        return httpCode;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Converts the actual code (integer value) to a ReturnCode enum value. If
     * the value cannot be found, the ReturnCode {@link #ERROR} is then returned.
     *
     * @param code The integer value to convert to a ReturnCode enum value
     * @return ReturnCode enum value
     */
    public static ReturnCode findReturnCode(final int code) {
        ReturnCode found = ERROR;

        for (final ReturnCode returnCode : ReturnCode.values()) {
            if (returnCode.getCode() == code) {
                found = returnCode;
                break;
            }
        }

        return found;
    }
}
