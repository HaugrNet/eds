/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-war)
 * =============================================================================
 */
package io.javadog.cws.war;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>CDI expects that all classes are uniquely identifiable, using type checks.
 * If multiple beans exists of the same type, then it is not able to correctly
 * find and inject this. By introducing a new Annotation for our Beans, it is
 * possible to better control what Bean is expected. The annotations is used for
 * both the Bean definition, and for the Bean injection, to better control
 * it.</p>
 *
 * <p>The annotation takes a type as value, which is then used to uniquely
 * identify the Bean. For more information about this approach, please see the
 * Blog from <a href="http://antoniogoncalves.org/2011/04/07/injection-with-cdi-part-i/">Antonia goncalves</a>.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, FIELD })
public @interface CWSBean {
}
