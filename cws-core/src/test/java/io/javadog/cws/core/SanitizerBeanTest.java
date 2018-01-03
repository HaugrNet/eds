/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.core.enums.SanityStatus;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.entities.DataEntity;
import io.javadog.cws.core.services.ProcessDataService;
import org.junit.Test;

import javax.persistence.Query;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SanitizerBeanTest extends DatabaseSetup {

    @Test
    public void testBean() {
        final SanitizerBean bean = prepareSanitizeBean();
        prepareInvalidData();

        // Check that there is nothing to scan/check at first
        final List<Long> idsBefore = bean.findNextBatch(100);
        assertThat(idsBefore.size(), is(6));

        // Run the actual sanitizing
        bean.sanitize();

        // Finally, verify that all records have been sanitized.
        final List<Long> idsAfter = bean.findNextBatch(100);
        assertThat(idsAfter.isEmpty(), is(true));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private SanitizerBean prepareSanitizeBean() {
        try {
            final SanitizerBean bean = SanitizerBean.class.getConstructor().newInstance();

            // Inject Dependencies
            setField(bean, "entityManager", entityManager);
            setField(bean, "settingBean", prepareSettingBean());

            // Invoke PostConstructor
            bean.init();

            return bean;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private SettingBean prepareSettingBean() {
        try {
            final SettingBean bean = SettingBean.class.getConstructor().newInstance();
            setField(bean, "entityManager", entityManager);
            setField(bean, "settings", settings);

            // Just invoking the postConstruct method, has to be done manually,
            // as the Bean is not managed in our tests ;-)
            bean.init();

            return bean;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private static void setField(final Object instance, final String fieldName, final Object value) {
        try {
            final Class<?> clazz = instance.getClass();
            final Field field;

            field = clazz.getDeclaredField(fieldName);
            final boolean accessible = field.isAccessible();

            field.setAccessible(true);
            field.set(instance, value);
            field.setAccessible(accessible);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot set Field", e);
        }
    }

    private void prepareInvalidData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        timeWarpChecksum(service.perform(prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "Valid Data1", 1048576)), new Date(1L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "Invalidated Data1", 1048576)), new Date(2L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddRequest(MEMBER_1, CIRCLE_1_ID, "Invalidated Data2", 524288)), new Date(), SanityStatus.OK);
        timeWarpChecksum(service.perform(prepareAddRequest(MEMBER_1, CIRCLE_2_ID, "Valid Data2", 1048576)), new Date(3L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddRequest(MEMBER_1, CIRCLE_2_ID, "Invalidated Data3", 1048576)), new Date(4L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddRequest(MEMBER_1, CIRCLE_2_ID, "Invalidated Data4", 524288)), new Date(), SanityStatus.OK);
        timeWarpChecksum(service.perform(prepareAddRequest(MEMBER_4, CIRCLE_3_ID, "Valid Data3", 1048576)), new Date(5L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddRequest(MEMBER_4, CIRCLE_3_ID, "Invalidated Data5", 1048576)), new Date(6L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddRequest(MEMBER_4, CIRCLE_3_ID, "Invalidated Data6", 524288)), new Date(), SanityStatus.OK);
    }

    private void timeWarpChecksum(final ProcessDataResponse response, final Date sanityCheck, final SanityStatus status) {
        // Now to the tricky part. We wish to test that the checksum is invalid,
        // and thus resulting in a correct error message. As the checksum is
        // controlled internally by CWS, it cannot be altered (rightfully) via
        // the API, hence we have to modify it directly in the database!
        final String jql = "select d from DataEntity d where d.metadata.externalId = :eid";
        final Query query = entityManager.createQuery(jql);
        query.setParameter("eid", response.getDataId());
        final DataEntity entity = (DataEntity) query.getSingleResult();
        entity.setSanityStatus(status);
        entity.setSanityChecked(sanityCheck);
        entityManager.persist(entity);
    }

    private static ProcessDataRequest prepareAddRequest(final String account, final String circleId, final String dataName, final int bytes) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, account);
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setDataName(dataName);
        request.setTypeName(Constants.DATA_TYPENAME);
        request.setData(generateData(bytes));

        return request;
    }
}