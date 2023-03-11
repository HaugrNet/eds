/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
package net.haugr.eds.core.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.enums.StandardSetting;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class SettingEntityTest extends DatabaseSetup {

    @Test
    void testEntity() {
        final SettingEntity entity = new SettingEntity();
        entity.setName("My.New.Setting");
        entity.setSetting("The Setting Value");
        save(entity);

        final SettingEntity found = find(SettingEntity.class, entity.getId());
        assertNotNull(found);
        assertEquals("My.New.Setting", found.getName());
        assertEquals("The Setting Value", found.getSetting());

        found.setName("My.Altered.Setting");
        found.setSetting("The Altered Setting");
        save(found);

        final SettingEntity updated = find(SettingEntity.class, entity.getId());
        assertNotNull(updated);
        assertEquals("My.Altered.Setting", updated.getName());
        assertEquals("The Altered Setting", updated.getSetting());

        final List<SettingEntity> mySettings = dao.findAllAscending(SettingEntity.class, "id");
        assertNotNull(mySettings);
        Assertions.assertEquals(StandardSetting.values().length + 1, mySettings.size());
    }

    @Test
    void testDaoFindSettings() {
        final List<SettingEntity> mySettings = dao.findAllAscending(SettingEntity.class, "id");
        assertNotNull(mySettings);
        assertEquals(StandardSetting.values().length, mySettings.size());
    }
}
