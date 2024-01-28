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
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.core.exceptions.EDSException;
import java.nio.charset.Charset;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>During the development of an application, using EDS to store data, it was
 * discovered that an exception was thrown with the following text:</p>
 *
 * <p><i>Given final block not properly padded. Such issues can arise if a bad
 * key is used during decryption.</i></p>
 *
 * <p>This test is written to see if it is possible to reproduce the error.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.1.1
 */
final class ShareBeanTest extends DatabaseSetup {

    private static final String DATA_NAME = "status";

    @Test
    void testUpdateEncryptedObject() {
        final ShareBean bean = prepareShareBean();

        final String initContent = "NEW";
        final String updateContent = "ACCEPTED";

        // Step 2; Add & Update Data Objects
        final String dataId = addData(bean, toBytes(initContent));
        updateData(bean, dataId, toBytes(updateContent));

        // Step 3; Check the stored content of the Circle
        final byte[] read = readData(bean, dataId);
        assertEquals(updateContent, toString(read));
    }

    private String addData(final ShareBean bean, final byte[] data) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        request.setAction(Action.ADD);
        request.setCircleId(CIRCLE_1_ID);
        request.setDataName(DATA_NAME);
        request.setData(data);

        final ProcessDataResponse response = bean.processData(request);
        throwIfFailed(response);

        return response.getDataId();
    }

    private void updateData(final ShareBean bean, final String dataId, final byte[] data) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setCircleId(CIRCLE_1_ID);
        request.setDataId(dataId);
        request.setDataName(DATA_NAME);
        request.setData(data);

        final ProcessDataResponse response = bean.processData(request);
        throwIfFailed(response);
    }

    private byte[] readData(final ShareBean bean, final String dataId) {
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class, MEMBER_1);
        request.setDataId(dataId);

        final FetchDataResponse response = bean.fetchData(request);
        throwIfFailed(response);

        return response.getData();
    }

    private static byte[] toBytes(final String str) {
        return str.getBytes(Charset.defaultCharset());
    }

    private static String toString(final byte[] bytes) {
        return new String(bytes, Charset.defaultCharset());
    }

    private static void throwIfFailed(final EDSResponse response) {
        if (!response.isOk()) {
            throw new EDSException(ReturnCode.findReturnCode(response.getReturnCode()), response.getReturnMessage());
        }
    }
}
