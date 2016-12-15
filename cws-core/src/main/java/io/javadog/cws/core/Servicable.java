package io.javadog.cws.core;

import io.javadog.cws.api.common.Verifiable;
import io.javadog.cws.api.dtos.Authenticate;
import io.javadog.cws.api.responses.CWSResponse;
import io.javadog.cws.core.exceptions.VerificationException;

import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public abstract class Servicable<R extends CWSResponse, V extends Authenticate> {

    /**
     * <p>The main processing method for the given Service. Takes care of the
     * Business Logic for the request, and returns the response.</p>
     *
     * <p>The method will all an unverified Request Object, and will as the
     * first step verify it before starting the actual processing. The
     * verification of the request Object is made regardless, to avoid that any
     * strange errors can or will occur.</p>
     *
     * @param request Request Object to process
     * @return Response Object with the result of the processing
     * @throws RuntimeException if an unknown error occurred
     */
    public abstract R process(V request);

    /**
     * <p>General Verification Method, takes the given Request Object and
     * invokes the validate method on it, to ensure that it is correct.</p>
     *
     * <p>If the given Object is null, or if it contains one or more problems,
     * then an Exception is thrown, as it is not possible for the CWS to
     * complete the request with this Request Object, the thrown Exception will
     * contain all the information needed to correct the problem.</p>
     *
     * @param verifiable Given Request Object to verify
     * @throws VerificationException if the given Object is null or invalid
     */
    protected static void verify(final Verifiable verifiable) {
        if (verifiable != null) {
            final Map<String, String> errors = verifiable.validate();
            if (!errors.isEmpty()) {
                final int capacity = errors.size() * 75;
                final StringBuilder builder = new StringBuilder(capacity);

                for (final Map.Entry<String, String> error : errors.entrySet()) {
                    builder.append("Key: ");
                    builder.append(error.getKey());
                    builder.append("Error: ");
                    builder.append(error.getValue());
                    builder.append('\n');
                }

                throw new VerificationException("Request Object contained errors: " + builder);
            }
        } else {
            throw new VerificationException("Cannot Process a NULL Object.");
        }
    }
}
