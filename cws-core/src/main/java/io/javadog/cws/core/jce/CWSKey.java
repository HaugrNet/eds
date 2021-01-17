/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.core.jce;

import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.model.Settings;
import java.lang.reflect.Field;
import java.security.Key;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * <p>Common CWS Key, used for all crypto operations.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public abstract class CWSKey<T extends Key> {

    private static final Logger LOG = Logger.getLogger(CWSKey.class.getName());

    protected boolean destroyed = false;
    protected final T key;

    private final KeyAlgorithm algorithm;

    /**
     * Default Constructor.
     *
     * @param algorithm Key Algorithm
     * @param key       Key
     */
    protected CWSKey(final KeyAlgorithm algorithm, final T key) {
        this.algorithm = algorithm;
        this.key = key;
    }

    public abstract T getKey();

    public final byte[] getEncoded() {
        return key.getEncoded();
    }

    public final KeyAlgorithm getAlgorithm() {
        return algorithm;
    }

    public final boolean isDestroyed() {
        return destroyed;
    }

    /**
     * <p>JCE provides an interface for the Keys, so it is possible to destroy
     * them, see {@link javax.security.auth.Destroyable#destroy()}. However,
     * not all Keys implement this, meaning that they will revert to throwing
     * a {@link javax.security.auth.DestroyFailedException }. Meaning, that
     * the keys used in CWS won't be destroyed as expected.</p>
     *
     * <p>The issues with this has been addressed in the following tickets, but
     * with no resolution other than &quot;won't fix&quot;:</p>
     *
     * <ul>
     *   <li>https://bugs.openjdk.java.net/browse/JDK-8008795</li>
     *   <li>https://bugs.openjdk.java.net/browse/JDK-8158689</li>
     *   <li>https://bugs.openjdk.java.net/browse/JDK-8160206</li>
     * </ul>
     *
     * <p>The reason for not implementing these seems to be that it is not
     * possible to guarantee that the keys are completely destroyed since the
     * memory containing them may have been swapped to disc. However, for CWS,
     * the keys only live for the duration of a request, so this is not a
     * problem.</p>
     *
     * <p>To resolve this problem in CWS, a rather bad hack using Reflection
     * has been applied, meaning that this method will use Reflection to scan
     * through a Class, and all instances of a byte array will be replaced with
     * a new nullified byte array.</p>
     *
     * <p>Keys use either BigInteger (RSA Keys) or byte arrays (Symmetric Keys),
     * so destroying them is not trivial. Since BigInteger have been designed
     * as Immutable Objects, they are harder to destroy, whereas simple byte
     * arrays are easier to destroy, so this logic will only destroy those.</p>
     *
     * <p>It should be noted, that Oracle have made some changes to how the
     * Reflection API works in Java, so this code has been tested with Java
     * version 8 &amp; 11. But, as the changes in the Reflection API may come
     * soon, there is no guarantee that it will work in later versions of
     * Java.</p>
     *
     * <p>One more note, since the code here is suppose to just work, i.e. be
     * as error prone as possible, any Exception thrown here is simply caught
     * and logged as a debug statement.</p>
     */
    protected void destroyKey() {
        try {
            final Field[] fields = key.getClass().getDeclaredFields();
            for (final Field field : fields) {
                // The type is a class instance, but it is not possible to
                // compare a class type directly using instanceof, when the
                // expected type is a byte array, hence this little trick
                // with comparing.
                if (field.getType() == byte[].class) {
                    // Hacking the field, requires that it is accessible, yet
                    // with saving the original value
                    final boolean accessible = setAccessible(field, true);

                    // First, read out the content of the field from the Object
                    final byte[] bytes = (byte[]) field.get(key);
                    // then, fill it with zeros
                    Arrays.fill(bytes, (byte) 0);
                    // and override the reference with a null, to trigger GC
                    field.set(key, null);

                    // Hacking completed, restoring the old Access information
                    setAccessible(field, accessible);
                }
            }
        } catch (IllegalAccessException | SecurityException e) {
            // This should never happen, but - if so, just log and ignore, at
            // this level, CWS should be as error prone as possible
            LOG.log(Settings.WARN, e, () -> "Unable to delete Key: " + e.getMessage());
        }
    }

    /**
     * <p>The method is wrapping the setAccessible method in the Java 8
     * Reflection API, as it has seen a rather substantial semantically change
     * in Java 9+. To minimize the number of annoying warnings to a minimum,
     * this method will simply wrap the call to the Java 8 setAccessible
     * method.</p>
     *
     * <p>The current value of the field accessibility is being saved prior to
     * updating it, so it can be returned.</p>
     *
     * @param field      The field to set the Access Flag for
     * @param accessible The Access Flag to set
     * @return True if this field is accessible, otherwise false
     * @throws SecurityException if the request is denied by the security manager
     */
    private boolean setAccessible(final Field field, final boolean accessible) {
        final boolean current = field.canAccess(key);
        field.setAccessible(accessible);

        return current;
    }
}
