package io.javadog.cws.core;

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
        final String algorithm = "AES/CBC/PKCS5Padding";
        final int keysize = 256;

        // First, filling out Crypto Objects
        final IvParameterSpec iv = Crypto.generateNewInitialVector();
        final SecretKey key = Crypto.generateSymmetricKey(algorithm, keysize);
        final Crypto crypto = new Crypto(iv, key, algorithm);

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
        final String algorithm = "RSA";
        final int keysize = 2048;
        final KeyPair keyPair = Crypto.generateAsymmetricKey(algorithm, keysize);
        final Crypto crypto = new Crypto(keyPair, algorithm);

        final String cleartext = "This is just an example";
        final byte[] encrypted = crypto.encrypt(Crypto.stringToBytes(cleartext));

        final byte[] decrypted = crypto.decrypt(encrypted);
        final String result = Crypto.bytesToString(decrypted);

        assertThat(result, is(cleartext));
    }
}
