/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>Moving files between folders (Rune): a file moves into any folder of
 * its Circle, also one created after it. Folders can't be moved, which is
 * what keeps the folder structure free of loops.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class MoveFileTest extends DatabaseSetup {

    @Test
    void testFileMovesIntoANewerFolder() {
        final ShareBean bean = prepareShareBean();
        final String file = add("older file", null, 10);
        final String folder = add("newer folder", Constants.FOLDER_TYPENAME, 0);

        final ProcessDataResponse moved = bean.processData(update(file, folder));
        assertEquals(ReturnCode.SUCCESS.getCode(), moved.getReturnCode(), moved.getReturnMessage());

        final FetchDataRequest list = prepareRequest(FetchDataRequest.class, MEMBER_1);
        list.setDataId(folder);
        assertEquals("older file", bean.fetchData(list).getMetadata().getFirst().getDataName());
    }

    @Test
    void testFoldersCantBeMoved() {
        final ShareBean bean = prepareShareBean();
        final String outer = add("outer", Constants.FOLDER_TYPENAME, 0);
        final String inner = add("inner", Constants.FOLDER_TYPENAME, 0);

        final ProcessDataResponse refused = bean.processData(update(outer, inner));
        assertEquals(ReturnCode.ILLEGAL_ACTION.getCode(), refused.getReturnCode());
        assertEquals("It is not permitted to move Folders.", refused.getReturnMessage());
    }

    private String add(final String name, final String type, final int bytes) {
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, name, bytes);
        request.setTypeName(type);
        final ProcessDataResponse response = prepareShareBean().processData(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode(), response.getReturnMessage());
        return response.getDataId();
    }

    private static ProcessDataRequest update(final String dataId, final String folderId) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, MEMBER_1);
        request.setAction(Action.UPDATE);
        request.setDataId(dataId);
        request.setFolderId(folderId);
        return request;
    }
}
