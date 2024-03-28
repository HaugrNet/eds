/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core.jce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.Settings;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class MasterKeyTest {

    private static final String URL_CONTENT = "This is my test data";

    @Test
    void testReadMasterKeySecretFromUrl() {
        final var settings = Settings.getInstance();
        final MasterKey masterKey = createInstance(settings);
        final String stringURL = prepareDataUrl();
        final byte[] bytes = MasterKey.readMasterKeySecretFromUrl(stringURL);
        SecretEDSKey key = masterKey.generateMasterKey(bytes);
        masterKey.setKey(key);

        final String salt = masterKey.getKey().getSalt().getArmored();
        assertEquals(settings.getSalt(), salt);
    }

    private String prepareDataUrl() {
        try {
            final Path tempFile = Files.createTempFile("Master", "key");
            Files.write(tempFile, URL_CONTENT.getBytes(), StandardOpenOption.APPEND);
            final URI uri = tempFile.toUri();

            return uri.toString();
        } catch (IOException e) {
            throw new EDSException(ReturnCode.ERROR, e);
        }
    }

    private MasterKey createInstance(final Settings settings) {
        Constructor<?> constructor = MasterKey.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);

        try {
            return (MasterKey) constructor.newInstance(settings);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new EDSException(ReturnCode.ERROR, e);
        }
    }
}
