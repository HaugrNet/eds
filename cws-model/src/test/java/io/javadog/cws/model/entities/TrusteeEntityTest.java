package io.javadog.cws.model.entities;

import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.model.EntityManagerSetup;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class TrusteeEntityTest extends EntityManagerSetup {

    @Test
    public void testEntity() {
        final MemberEntity member = prepareMember("Trustee Member", "public Key", "private Key");
        final CircleEntity circle = prepareCircle("Trustee Circle");
        final TrusteeEntity entity = new TrusteeEntity();
        entity.setMember(member);
        entity.setCircle(circle);
        entity.setArmoredKey("Circle Key");
        entity.setTrustLevel(TrustLevel.ADMIN);
        persist(entity);

        assertThat(entity.getId(), is(not(nullValue())));
    }
}
