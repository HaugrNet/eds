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

import net.haugr.eds.core.features.EntryQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.FeatureState;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.dtos.Entry;
import net.haugr.eds.api.requests.EntryRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.responses.EntryResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.features.FeatureDeclaration;
import org.junit.jupiter.api.Test;

/**
 * <p>The declarations which Rune and Tolk bring along (FEATURES.md, "The
 * features"): the stone accepts them, and the chat's declaration lets a
 * client do what the chat of the stone did, with the stone knowing nothing
 * of a chat.</p>
 *
 * <p>The declarations are Rune's files; without Rune's sources these tests
 * are skipped.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class FeatureConventionsTest extends DatabaseSetup {

    private static final Path DECLARATIONS = Path.of("..", "eds-quarkus", "src", "main", "webui", "src", "features", "declarations");
    private static final String TALK = "chat";

    @Test
    void testTheStoneAcceptsTheFourDeclarations() throws IOException {
        final int largest = (int) settings.getDataMaxInlineSize();

        final FeatureDeclaration chat = FeatureDeclaration.parse(read("chat"), largest);
        assertEquals("chat", chat.name());
        assertEquals(List.of("message", "reaction", "receipt"), List.copyOf(chat.kinds().keySet()));
        assertEquals(2000, chat.signals());
        assertEquals(FeatureDeclaration.Keep.MEMBER_ABOUT, chat.kind("reaction").keep());
        assertEquals("message", chat.kind("reaction").about());
        assertEquals(FeatureDeclaration.Keep.MEMBER, chat.kind("receipt").keep());
        assertEquals(TrustLevel.READ, chat.kind("receipt").add());

        final FeatureDeclaration calendar = FeatureDeclaration.parse(read("calendar"), largest);
        assertEquals(List.of("event"), List.copyOf(calendar.kinds().keySet()));
        assertFalse(calendar.hasSignals());
        assertTrue(calendar.kind("event").change().contains(FeatureDeclaration.Who.ADDERS));

        final FeatureDeclaration location = FeatureDeclaration.parse(read("location"), largest);
        assertEquals(List.of("position", "track"), List.copyOf(location.kinds().keySet()));
        assertEquals(FeatureDeclaration.Keep.MEMBER, location.kind("position").keep());
        assertEquals(FeatureDeclaration.Keep.AGE, location.kind("track").keep());
        assertEquals(86400, location.kind("track").keepValue());
        assertTrue(location.kind("track").change().isEmpty());
        assertEquals(TrustLevel.READ, location.kind("position").add());

        final FeatureDeclaration plans = FeatureDeclaration.parse(read("plans"), largest);
        assertEquals(List.of("plan", "item", "expense", "payment", "confirmation"), List.copyOf(plans.kinds().keySet()));
        assertEquals(List.of(FeatureDeclaration.Who.AUTHOR, FeatureDeclaration.Who.ADMINISTRATORS), List.copyOf(plans.kind("plan").close()));
        assertEquals("plan", plans.kind("expense").about());
        assertEquals("payment", plans.kind("confirmation").about());
        assertEquals(List.of(FeatureDeclaration.Who.AUTHOR), List.copyOf(plans.kind("expense").change()));
        assertTrue(plans.kind("confirmation").change().isEmpty());
    }

    // =========================================================================
    // What the chat of the stone did (PLAN.md, L4 and "After L5"), with the
    // chat's declaration: sending, the history in pages, editing and
    // deleting by the author only, reactions, read markers and unread
    // messages, and typing.
    // =========================================================================

    @Test
    void testTheChatSendsAndReadsTheHistoryInPages() throws IOException {
        final FeatureBean features = chat();

        // Sending needs WRITE; the stone notes who and when.
        final Messages messages = messages(features);
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), add(features, MEMBER_3, "message", null, "{\"v\":1,\"text\":\"Not allowed\"}").getReturnCode());
        assertEquals(MEMBER_2_ID, messages.first().getMemberId());
        assertEquals(MEMBER_2, messages.first().getAccountName());
        assertNotNull(messages.first().getAdded());

        // The history is read with READ, in pages which go backwards.
        final List<Entry> newest = features.entries(session(MEMBER_3), CIRCLE_1_ID, TALK, EntryQuery.all().kind("message").limit(2)).getEntries();
        assertEquals(List.of(messages.third().getEntryId(), messages.second().getEntryId()), newest.stream().map(Entry::getEntryId).toList());
        final List<Entry> earlier = features.entries(session(MEMBER_3), CIRCLE_1_ID, TALK, EntryQuery.all().kind("message").before(messages.second().getPosition()).limit(2)).getEntries();
        assertEquals(List.of(messages.first().getEntryId()), earlier.stream().map(Entry::getEntryId).toList());
    }

    @Test
    void testOnlyItsAuthorEditsOrDeletesAMessage() throws IOException {
        final FeatureBean features = chat();
        final Entry second = messages(features).second();

        // Not an administrator.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.changeEntry(session(MEMBER_1), CIRCLE_1_ID, TALK, second.getEntryId(), body(null, null, "{}")).getReturnCode());
        final Entry edited = features.changeEntry(session(MEMBER_2), CIRCLE_1_ID, TALK, second.getEntryId(), body(null, null, "{\"v\":1,\"text\":\"Fixed\"}")).getEntries().getFirst();
        assertNotNull(edited.getChanged());
        assertEquals(second.getPosition(), edited.getPosition());
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), features.removeEntry(session(MEMBER_1), CIRCLE_1_ID, TALK, second.getEntryId()).getReturnCode());
    }

    @Test
    void testOneReactionPerMemberAndMessage() throws IOException {
        final FeatureBean features = chat();
        final Entry second = messages(features).second();

        // A reaction: one entry per Member about each message, which they
        // change to give another or take one back.
        final Entry reaction = add(features, MEMBER_1, "reaction", second.getEntryId(), "{\"v\":1,\"reactions\":[\"+1\"]}").getEntries().getFirst();
        final Entry again = add(features, MEMBER_1, "reaction", second.getEntryId(), "{\"v\":1,\"reactions\":[\"+1\",\"heart\"]}").getEntries().getFirst();
        assertEquals(reaction.getEntryId(), again.getEntryId());
        // Reactions need WRITE, as they did.
        assertEquals(ReturnCode.AUTHORIZATION_WARNING.getCode(), add(features, MEMBER_3, "reaction", second.getEntryId(), "{\"v\":1,\"reactions\":[\"+1\"]}").getReturnCode());
        assertEquals(1, features.entries(session(MEMBER_3), CIRCLE_1_ID, TALK, EntryQuery.all().kind("reaction").about(second.getEntryId())).getEntries().size());
    }

    @Test
    void testReadMarkersAndUnreadMessages() throws IOException {
        final FeatureBean features = chat();
        final Messages messages = messages(features);
        final Entry first = messages.first();
        final Entry third = messages.third();

        // A read marker per Member, also for one who may only read; what is
        // unread is counted after it.
        assertEquals(3L, features.count(session(MEMBER_3), CIRCLE_1_ID, TALK, "message", null).getCount());
        final Entry receipt = add(features, MEMBER_3, "receipt", null, "{\"v\":1,\"received\":" + third.getPosition() + ",\"read\":" + first.getPosition() + "}").getEntries().getFirst();
        assertEquals(2L, features.count(session(MEMBER_3), CIRCLE_1_ID, TALK, "message", first.getPosition()).getCount());
        final Entry moved = add(features, MEMBER_3, "receipt", null, "{\"v\":1,\"received\":" + third.getPosition() + ",\"read\":" + third.getPosition() + "}").getEntries().getFirst();
        assertEquals(receipt.getEntryId(), moved.getEntryId());
        assertEquals(0L, features.count(session(MEMBER_3), CIRCLE_1_ID, TALK, "message", third.getPosition()).getCount());
        // The markers of all, for "read by all".
        add(features, MEMBER_2, "receipt", null, "{\"v\":1,\"received\":1,\"read\":1}");
        assertEquals(2, features.entries(session(MEMBER_1), CIRCLE_1_ID, TALK, EntryQuery.all().kind("receipt")).getEntries().size());
    }

    @Test
    void testADeletedMessageTakesItsReactionsAlong() throws IOException {
        final FeatureBean features = chat();
        final Messages messages = messages(features);
        final Entry second = messages.second();
        add(features, MEMBER_1, "reaction", second.getEntryId(), "{\"v\":1,\"reactions\":[\"+1\"]}");
        add(features, MEMBER_3, "receipt", null, "{\"v\":1,\"received\":" + messages.third().getPosition() + ",\"read\":" + messages.third().getPosition() + "}");
        add(features, MEMBER_2, "receipt", null, "{\"v\":1,\"received\":1,\"read\":1}");

        // The message and its reaction go; the markers stay.
        final EntryResponse deleted = features.removeEntry(session(MEMBER_2), CIRCLE_1_ID, TALK, second.getEntryId());
        assertTrue(deleted.isOk(), deleted.getReturnMessage());
        assertEquals(2, deleted.getEntries().size());
        assertEquals(2L, features.count(session(MEMBER_3), CIRCLE_1_ID, TALK, "message", null).getCount());
        assertEquals(2L, features.count(session(MEMBER_3), CIRCLE_1_ID, TALK, "receipt", null).getCount());
    }

    @Test
    void testTypingIsASignal() throws IOException {
        final FeatureBean features = chat();

        // At most one in two seconds.
        assertEquals(2000L, features.signal(session(MEMBER_3), CIRCLE_1_ID, TALK));
    }


    // =========================================================================
    // Internal helper methods
    // =========================================================================

    /** The three messages the chat tests start with; the third replies to the first. */
    private record Messages(Entry first, Entry second, Entry third) {}

    /** The stone offers the chat, and circle1 has it switched on. */
    private FeatureBean chat() throws IOException {
        final FeatureBean features = new FeatureBean(entityManager, settings);
        assertTrue(features.putFeature(session(Constants.ADMIN_ACCOUNT), TALK, read("chat")).isOk());
        assertTrue(features.setCircleFeature(session(MEMBER_1), CIRCLE_1_ID, TALK, FeatureState.ON).isOk());
        return features;
    }

    private Messages messages(final FeatureBean features) {
        final Entry first = add(features, MEMBER_2, "message", null, "{\"v\":1,\"text\":\"Hello Circle\"}").getEntries().getFirst();
        final Entry second = add(features, MEMBER_2, "message", null, "{\"v\":1,\"text\":\"Second\"}").getEntries().getFirst();
        final Entry third = add(features, MEMBER_1, "message", null, "{\"v\":1,\"text\":\"Third\",\"reply\":\"" + first.getEntryId() + "\"}").getEntries().getFirst();
        return new Messages(first, second, third);
    }

    private static String read(final String feature) throws IOException {
        final Path file = DECLARATIONS.resolve(feature + ".json");
        assumeTrue(Files.exists(file), "Rune's sources aren't here: " + file);
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private EntryResponse add(final FeatureBean features, final String account, final String kind, final String about, final String text) {
        return features.addEntry(session(account), CIRCLE_1_ID, TALK, body(kind, about, text));
    }

    private static EntryRequest body(final String kind, final String about, final String text) {
        final EntryRequest body = new EntryRequest();
        body.setKind(kind);
        body.setAbout(about);
        body.setContent(Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
        return body;
    }

    private byte[] session(final String account) {
        final ProcessMemberRequest request = prepareRequest(ProcessMemberRequest.class, account);
        request.setAction(Action.LOGIN);
        final ProcessMemberResponse response = prepareManagementBean().processMember(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        return response.getSession();
    }
}
