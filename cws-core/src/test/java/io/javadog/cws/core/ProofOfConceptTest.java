package io.javadog.cws.core;

import io.javadog.cws.core.common.Crypto;
import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.KeyPair;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Proof Of Concept, showing that the simple Cryptographic Operations will work,
 * and how to build up the simple components.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProofOfConceptTest {

    /**
     * There's two types of Cryptography applied in CWS. This test will
     * demonstrate the Symmetric Encryption part, which is used for all Objects
     * shared to a Group.
     */
    @Test
    public void testObjectEncryption() {
        // First, filling out Crypto Objects
        final IvParameterSpec iv = Crypto.generateNewInitialVector();
        final SecretKey key = Crypto.generateSymmetricKey();
        final Crypto crypto = new Crypto(iv, key);

        // Now, we're going to encrypt some data
        final String cleartext = "This is just an example";
        final byte[] encrypted = crypto.encrypt(Crypto.stringToBytes(cleartext));

        // And decrypt it so we can verify it
        final byte[] decrypted = crypto.decrypt(encrypted);
        final String result = Crypto.bytesToString(decrypted);

        assertThat(result, is(cleartext));
    }

    /**
     * For the Group Members, we're storing the Symmetric Key per Member using
     * Asymmetric Encryption - this will allow that Group Members can be changed
     * independently of the Group Data. The Member's Public Key is stored with
     * the Member, encrypted using the Member's Public Key. The Private Key
     * which may be stored elsewhere or additionally encrypted can then be used
     * by the Member to access the data.
     */
    @Test
    public void testMemberEncryption() {
        final KeyPair keyPair = Crypto.generateAsymmetricKey();
        final Crypto crypto = new Crypto(keyPair);

        final String cleartext = "This is just an example";
        final byte[] encrypted = crypto.encrypt(Crypto.stringToBytes(cleartext));

        final byte[] decrypted = crypto.decrypt(encrypted);
        final String result = Crypto.bytesToString(decrypted);

        assertThat(result, is(cleartext));
    }

    /**
     * <p>Members of a Group must have both a Public and a Private Key. If they
     * are not providing a Private Key as part of initializing a Session, we
     * need a different way to retrieve it. We can, of course, generate a Key
     * Pair and store with the Member information, but storing the Private key
     * thus, can hardly be considered a good idea! So, instead we need to
     * encrypt it and store it so.</p>
     *
     * <p>This means that we must take a secret information provided by the
     * Member, and convert this into a Key, which we can then use. The standard
     * for this used to be PBKDF2 (Password-Based Key Derivation Function 2),
     * but as it has some weaknesses, a contest was made in 2015, which aimed at
     * replacing it. And the replacement is Argon2. However, as there is yet to
     * be added proper support for Argon2 in Java, we're sticking with PBKDF2
     * for our immediate needs.</p>
     */
    @Test
    public void testPasswordToKey() {
        final char[] password = "MySuperSecretPassword".toCharArray();
        final String salt = "SystemSpecificSalt";
        final SecretKey key = Crypto.convertPasswordToKey(password, salt);

        assertThat(key.getAlgorithm(), is("AES"));
        //assertThat(key.getEncoded().length, is(32));

        final IvParameterSpec iv = Crypto.generateNewInitialVector();
        final Crypto crypto = new Crypto(iv, key);

        // Now, we're going to encrypt some data
        final String cleartext = "This is just an example";
        final byte[] encrypted = crypto.encrypt(Crypto.stringToBytes(cleartext));

        // And decrypt it so we can verify it
        final byte[] decrypted = crypto.decrypt(encrypted);
        final String result = Crypto.bytesToString(decrypted);

        assertThat(result, is(cleartext));
    }
}
