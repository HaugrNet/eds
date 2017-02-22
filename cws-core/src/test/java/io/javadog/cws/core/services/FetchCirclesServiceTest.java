package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.model.DatabaseSetup;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.MemberEntity;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchCirclesServiceTest extends DatabaseSetup {

    @Test
    public void testFetchAll() {
        createTwoCircleWith5Members();
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = prepareService();

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(2));
        assertThat(response.getTrustees().size(), is(0));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getCircles().get(1).getName(), is("circle2"));
    }

    @Test
    public void testFetchCircleAsAdmin() {
        createTwoCircleWith5Members();
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = prepareService();

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount(Constants.ADMIN_ACCOUNT);
        request.setCredential(Constants.ADMIN_ACCOUNT.toCharArray());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(2));
        assertThat(response.getTrustees().size(), is(0));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getCircles().get(1).getName(), is("circle2"));

        request.setCircleId(response.getCircles().get(0).getId());
        final FetchCircleResponse response1 = service.perform(request);
        assertThat(response1, is(not(nullValue())));
        assertThat(response1.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response1.getReturnMessage(), is("Ok"));
        assertThat(response1.getCircles().size(), is(1));
        assertThat(response1.getTrustees().size(), is(4));
    }

    @Test
    public void testFetchCircleAsMember() {
        createTwoCircleWith5Members();
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = prepareService();

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount("member1");
        request.setCredential("member1".toCharArray());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(2));
        assertThat(response.getTrustees().size(), is(0));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getCircles().get(1).getName(), is("circle2"));

        request.setCircleId(response.getCircles().get(0).getId());
        final FetchCircleResponse response1 = service.perform(request);
        assertThat(response1, is(not(nullValue())));
        assertThat(response1.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response1.getReturnMessage(), is("Ok"));
        assertThat(response1.getCircles().size(), is(1));
        assertThat(response1.getCircles().get(0).getName(), is("circle1"));
        assertThat(response1.getTrustees().size(), is(4));
    }

    @Test
    public void testFetchCircleAsNonMember() {
        createTwoCircleWith5Members();
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = prepareService();

        final FetchCircleRequest request = new FetchCircleRequest();
        request.setAccount("member1");
        request.setCredential("member1".toCharArray());
        final FetchCircleResponse response = service.perform(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(2));
        assertThat(response.getTrustees().size(), is(0));
        assertThat(response.getCircles().get(0).getName(), is("circle1"));
        assertThat(response.getCircles().get(1).getName(), is("circle2"));

        prepareCause(ModelException.class, Constants.CONSTRAINT_ERROR, "No trustees found.");
        request.setCircleId(response.getCircles().get(1).getId());
        service.perform(request);
    }

    private Servicable<FetchCircleResponse, FetchCircleRequest> prepareService() {
        final Settings settings = new Settings();
        final Servicable<FetchCircleResponse, FetchCircleRequest> service = new FetchCirclesService(settings, entityManager);

        return service;
    }

    private void createTwoCircleWith5Members() {
        final MemberEntity member1 = createMember("member1");
        final MemberEntity member2 = createMember("member2");
        final MemberEntity member3 = createMember("member3");
        final MemberEntity member4 = createMember("member4");
        final MemberEntity member5 = createMember("member5");

        final CircleEntity circle1 = prepareCircle("circle1");
        final CircleEntity circle2 = prepareCircle("circle2");

        addKeyAndTrusteesToCircle(circle1, member1, member2, member3, member4);
        addKeyAndTrusteesToCircle(circle2, member3, member4, member5);
    }
}
