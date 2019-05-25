/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
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
package io.javadog.cws.core.model.entities;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.StandardSetting;
import java.util.List;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class SettingEntityTest extends DatabaseSetup {

    @Test
    public void testEntity() {
        final SettingEntity entity = new SettingEntity();
        entity.setName("My.New.Setting");
        entity.setSetting("The Setting Value");
        persist(entity);

        final SettingEntity found = find(SettingEntity.class, entity.getId());
        assertNotNull(found);
        assertThat(found.getName(), is("My.New.Setting"));
        assertThat(found.getSetting(), is("The Setting Value"));

        found.setName("My.Altered.Setting");
        found.setSetting("The Altered Setting");
        persist(found);

        final SettingEntity updated = find(SettingEntity.class, entity.getId());
        assertNotNull(updated);
        assertThat(updated.getName(), is("My.Altered.Setting"));
        assertThat(updated.getSetting(), is("The Altered Setting"));

        final List<SettingEntity> mySettings = dao.findAllAscending(SettingEntity.class, "id");
        assertNotNull(mySettings);
        assertThat(mySettings.size(), is(StandardSetting.values().length + 1));
    }

    @Test
    public void testDaoFindSettings() {
        final List<SettingEntity> mySettings = dao.findAllAscending(SettingEntity.class, "id");
        assertNotNull(mySettings);
        assertThat(mySettings.size(), is(StandardSetting.values().length));
    }
}
