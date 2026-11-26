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
package net.haugr.eds.testing.load;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

/**
 * <p>One user of the load test: an account of its own, its own connections
 * and socket, in up to three rooms, doing what Rune and Tolk do, picked at
 * random by weight, one action after the other as a person's client does.</p>
 *
 * <p>A user changes and removes only what it made itself, so a refusal is a
 * fault of the stone; the exceptions are the reactions to the messages of
 * others, which may be gone by then, and are counted as expected.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class VirtualUser implements Runnable {

    private static final String CHAT = "chat";
    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
    private static final String[] AVATARS = { "raven", "wolf", "bear", "eagle", "serpent", "horse", "boar", "stag", "goat", "cat", "squirrel", "salmon" };
    private static final String[] REACTIONS = { "👍", "❤️", "😂", "😮", "😢", "🙏" };
    /** The return code of an entry or object which is gone. */
    private static final Set<Integer> GONE = Set.of(404);
    private static final int HELD_OBJECTS = 60;

    /** A Data Object this user made, and the content it must have. */
    private record Stored(String dataId, String circleId, String folderId, long seed, int size, String etag) {
        Stored withContent(final long newSeed, final int newSize, final String newEtag) {
            return new Stored(dataId, circleId, folderId, newSeed, newSize, newEtag);
        }
    }

    /** An entry of a feature this user made. */
    private record Own(String circleId, String feature, String kind, String entryId, String about, boolean closed) {
    }

    private final World world;
    private final int index;
    final String name;
    String passphrase;
    String memberId = null;
    final List<String> circles = new ArrayList<>();

    private final Http http;
    private final SplittableRandom random;
    private Socket socket = null;
    private long counter = 0;
    private final List<Stored> stored = new ArrayList<>();
    private final List<String> folders = new ArrayList<>();
    private final Map<String, List<Own>> own = new HashMap<>();
    private long held = 0;
    private final Map<String, Deque<String>> othersMessages = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> present = new ConcurrentHashMap<>();
    /** The newest position of a message heard in each room, from the socket. */
    private final Map<String, Long> heard = new ConcurrentHashMap<>();
    /** The position up to which this user has read each room, as its last receipt says. */
    private final Map<String, Long> read = new ConcurrentHashMap<>();
    private long lastTyping = 0;
    private final List<Action> actions = new ArrayList<>();
    private double totalWeight = 0;

    private record Action(String name, double weight, Runnable run) {
    }

    VirtualUser(final World world, final int index, final String passphrase) {
        this.world = world;
        this.index = index;
        this.name = world.run + "-user" + index;
        this.passphrase = passphrase;
        this.http = new Http(world.url, world.metrics);
        this.random = new SplittableRandom(world.settings.seed() * 31 + index);
        defineActions();
    }

    // =========================================================================
    // The actions, and how often each is picked
    // =========================================================================

    private void defineActions() {
        // The chat, mostly over the socket, as Rune does it.
        action("chat.send", 14, () -> sendMessage(true));
        action("chat.send.rest", 3, () -> sendMessage(false));
        action("chat.edit", 3, this::editMessage);
        action("chat.delete", 2, this::deleteMessage);
        action("chat.react", 4, this::react);
        action("chat.receipt", 3, this::receipt);
        action("chat.history", 4, () -> history(true));
        action("chat.history.rest", 2, () -> history(false));
        action("chat.count", 2, () -> count(true));
        action("chat.count.rest", 1, () -> count(false));
        action("chat.typing", 3, this::typing);
        action("entry.get", 2, this::readEntry);
        // The other features.
        action("calls.signal", 2, this::callSignal);
        action("plans", 3, this::plans);
        action("calendar", 2, this::calendar);
        action("location", 2, this::location);
        action("presence", 1, this::presence);
        // Files.
        action("data.upload", 6, () -> upload(false));
        action("data.inline", 3, () -> upload(true));
        action("data.folder", 0.5, this::folder);
        action("data.list", 5, this::list);
        action("data.fetch", 1, this::fetchData);
        action("data.metadata", 2, this::metadata);
        action("data.download", 6, this::download);
        action("data.range", 3, this::range);
        action("data.head", 2, this::head);
        action("data.token", 1, this::token);
        action("data.thumbnail", 1, this::thumbnail);
        action("data.rename", 1, this::rename);
        action("data.replace", 2, this::replace);
        action("data.copy", 1, this::copy);
        action("data.move", 1, this::move);
        action("data.delete", 2, this::deleteOne);
        // Reading what there is.
        action("read", 8, this::read);
        // The account.
        action("account.avatar", 0.5, this::avatar);
        action("account.relogin", 0.3, this::relogin);
        action("account.passphrase", 0.05, this::changePassphrase);
    }

    private void action(final String actionName, final double weight, final Runnable run) {
        actions.add(new Action(actionName, weight, run));
        totalWeight += weight;
    }

    private Action pick() {
        double at = random.nextDouble() * totalWeight;
        for (final Action action : actions) {
            at -= action.weight();
            if (at < 0) {
                return action;
            }
        }
        return actions.getLast();
    }

    // =========================================================================
    // Starting, running and stopping
    // =========================================================================

    /** Logs in; false if it failed (recorded). */
    boolean login() {
        final Http.Reply reply = http.post("account.login", "/members/login",
                Http.object().put("accountName", name).put("credential", World.base64(passphrase)));
        if (!reply.ok()) {
            return false;
        }
        http.session(reply.text("session"));
        memberId = reply.text("memberId");
        return true;
    }

    /** Opens the socket and joins the rooms. */
    boolean connect() {
        socket = new Socket(world.metrics, frameListener());
        if (!socket.open(http)) {
            return false;
        }
        for (final String circleId : circles) {
            join(circleId);
        }
        return true;
    }

    @Override
    public void run() {
        while (!world.stopping) {
            if ((socket == null) || !socket.isOpen()) {
                final String why = (socket != null) ? socket.closedBecause() : null;
                if (why != null) {
                    world.metrics.tally("socket.lost", Metrics.Outcome.FAILED, 0, 0, 0, name + ": " + why);
                }
                if (!connect()) {
                    pause(1000);
                    continue;
                }
            }
            final Action action = pick();
            try {
                action.run().run();
            } catch (RuntimeException e) {
                world.metrics.tally(action.name(), Metrics.Outcome.FAILED, 0, 0, 0, "the test failed: " + e);
            }
            if (world.settings.thinkMillis() > 0) {
                pause(random.nextInt(world.settings.thinkMillis() + 1));
            }
        }
    }

    void stop() {
        if (socket != null) {
            socket.close();
        }
    }

    void close() {
        stop();
        http.close();
    }

    // =========================================================================
    // What the socket hears
    // =========================================================================

    private Consumer<JsonNode> frameListener() {
        final String marker = "\"u\":" + index + ",";
        return frame -> {
            final String type = frame.path("type").asText();
            final String circleId = frame.path("circleId").asText();
            switch (type) {
                case "ADDED" -> {
                    final JsonNode entry = frame.path("entry");
                    noteHeard(heard, circleId, entry);
                    final String content = World.text(entry.path("content").asText(""));
                    if (CHAT.equals(entry.path("feature").asText()) && "message".equals(entry.path("kind").asText()) && !content.contains(marker)) {
                        final Deque<String> recent = othersMessages.computeIfAbsent(circleId, key -> new ConcurrentLinkedDeque<>());
                        recent.addFirst(entry.path("entryId").asText());
                        while (recent.size() > 20) {
                            recent.pollLast();
                        }
                        delivered("chat.delivered", content);
                    }
                }
                case "SIGNAL" -> delivered("signal.delivered", frame.path("signal").asText(""));
                case "PRESENT" -> present.computeIfAbsent(circleId, key -> ConcurrentHashMap.newKeySet()).add(frame.path("memberId").asText());
                case "ABSENT" -> present.computeIfAbsent(circleId, key -> ConcurrentHashMap.newKeySet()).remove(frame.path("memberId").asText());
                case "REMOVED" -> {
                    final JsonNode entry = frame.path("entry");
                    final Deque<String> recent = othersMessages.get(circleId);
                    if (recent != null) {
                        recent.remove(entry.path("entryId").asText());
                    }
                }
                default -> {
                    // CHANGED, FILES, FEATURE, LEFT: counted by the socket.
                }
            }
        };
    }

    /** The time from sending to hearing, from the "sent" in a message or signal of another user of this process. */
    private void delivered(final String operation, final String content) {
        final OptionalLong sent = sent(content);
        if (sent.isPresent()) {
            world.metrics.latency(operation, System.nanoTime() - sent.getAsLong());
        }
    }

    /** The "sent" of a message or signal of the test; empty if it has none, so it isn't one of the test's. */
    private static OptionalLong sent(final String content) {
        final int at = content.indexOf("\"sent\":");
        if (at < 0) {
            return OptionalLong.empty();
        }
        int end = at + 7;
        while ((end < content.length()) && Character.isDigit(content.charAt(end))) {
            end++;
        }
        try {
            return OptionalLong.of(Long.parseLong(content.substring(at + 7, end)));
        } catch (NumberFormatException _) {
            return OptionalLong.empty();
        }
    }

    // =========================================================================
    // The chat and the other features
    // =========================================================================

    private String room() {
        return circles.get(random.nextInt(circles.size()));
    }

    private String messageContent() {
        return "{\"v\":1,\"u\":" + index + ",\"sent\":" + System.nanoTime() + ",\"text\":\"" + name + " says hello, number " + (++counter)
                + " " + "lorem ipsum dolor sit amet ".repeat(random.nextInt(1, 12)) + "\"}";
    }

    private ObjectNode frame(final String type, final String circleId, final String feature) {
        return Http.object().put("type", type).put("circleId", circleId).put("feature", feature);
    }

    private String entriesPath(final String circleId, final String feature) {
        return "/circles/" + circleId + "/features/" + feature + "/entries";
    }

    /** Adds an entry over the socket or the REST API; its ID, or null. */
    private String add(final String operation, final boolean overSocket, final String circleId, final String feature, final String kind,
                       final String about, final String content, final Set<Integer> expected) {
        final JsonNode entries;
        if (overSocket) {
            final ObjectNode frame = frame("ADD", circleId, feature).put("kind", kind).put("content", World.base64(content));
            if (about != null) {
                frame.put("about", about);
            }
            final JsonNode done = socket.request(operation, frame, expected);
            entries = ((done != null) && "DONE".equals(done.path("type").asText())) ? done.path("entries") : null;
        } else {
            final ObjectNode body = Http.object().put("kind", kind).put("content", World.base64(content));
            if (about != null) {
                body.put("about", about);
            }
            final Http.Reply reply = http.json(operation, "POST", entriesPath(circleId, feature), body, expected);
            entries = reply.ok() ? reply.json().path("entries") : null;
        }
        if (entries != null) {
            for (final JsonNode entry : entries) {
                if (kind.equals(entry.path("kind").asText())) {
                    return entry.path("entryId").asText();
                }
            }
        }
        return null;
    }

    private void sendMessage(final boolean overSocket) {
        final String circleId = room();
        final String entryId = add(overSocket ? "chat.send" : "chat.send.rest", overSocket, circleId, CHAT, "message", null, messageContent(), Set.of());
        if (entryId != null) {
            ownOf(circleId).add(new Own(circleId, CHAT, "message", entryId, null, false));
            world.messagesOf(circleId).incrementAndGet();
        }
    }

    private List<Own> ownOf(final String circleId) {
        return own.computeIfAbsent(circleId, key -> new ArrayList<>());
    }

    private Own anyOwn(final String feature, final String kind) {
        final List<Own> mine = new ArrayList<>();
        for (final String circleId : circles) {
            for (final Own entry : ownOf(circleId)) {
                if (entry.feature().equals(feature) && entry.kind().equals(kind)) {
                    mine.add(entry);
                }
            }
        }
        return mine.isEmpty() ? null : mine.get(random.nextInt(mine.size()));
    }

    private void editMessage() {
        final Own message = anyOwn(CHAT, "message");
        if (message != null) {
            change("chat.edit", random.nextBoolean(), message, messageContent());
        }
    }

    private void change(final String operation, final boolean overSocket, final Own entry, final String content) {
        if (overSocket) {
            socket.request(operation, frame("CHANGE", entry.circleId(), entry.feature()).put("entryId", entry.entryId()).put("content", World.base64(content)), Set.of());
        } else {
            http.put(operation + ".rest", entriesPath(entry.circleId(), entry.feature()) + '/' + entry.entryId(), Http.object().put("content", World.base64(content)));
        }
    }

    private boolean remove(final String operation, final boolean overSocket, final Own entry) {
        if (overSocket) {
            final JsonNode done = socket.request(operation, frame("REMOVE", entry.circleId(), entry.feature()).put("entryId", entry.entryId()), Set.of());
            return (done != null) && "DONE".equals(done.path("type").asText());
        }
        return http.delete(operation + ".rest", entriesPath(entry.circleId(), entry.feature()) + '/' + entry.entryId()).ok();
    }

    private void deleteMessage() {
        final Own message = anyOwn(CHAT, "message");
        if ((message != null) && remove("chat.delete", random.nextBoolean(), message)) {
            ownOf(message.circleId()).remove(message);
            world.messagesOf(message.circleId()).decrementAndGet();
        }
    }

    private void react() {
        final String circleId = room();
        final Deque<String> recent = othersMessages.get(circleId);
        final String about = (recent != null) ? recent.peekFirst() : null;
        if (about != null) {
            final String content = "{\"v\":1,\"reactions\":[\"" + REACTIONS[random.nextInt(REACTIONS.length)] + "\"]}";
            add("chat.react", true, circleId, CHAT, "reaction", about, content, GONE);
        }
    }

    private void receipt() {
        // As Rune does: the receipt holds the position read up to.
        final String circleId = room();
        final long upTo = heard.getOrDefault(circleId, 0L);
        read.put(circleId, upTo);
        add("chat.receipt", random.nextBoolean(), circleId, CHAT, "receipt", null, "{\"v\":1,\"read\":" + upTo + "}", Set.of());
    }

    /**
     * Notes the position of a message heard in a room, if it is newer.
     *
     * @param heard    The newest position heard, by room
     * @param circleId The room
     * @param entry    The entry of an ADDED frame
     */
    static void noteHeard(final Map<String, Long> heard, final String circleId, final JsonNode entry) {
        if (CHAT.equals(entry.path("feature").asText()) && "message".equals(entry.path("kind").asText()) && entry.path("position").canConvertToLong()) {
            heard.merge(circleId, entry.path("position").asLong(), Math::max);
        }
    }

    private void history(final boolean overSocket) {
        final String circleId = room();
        final int limit = random.nextInt(10, 101);
        if (overSocket) {
            socket.request("chat.history", frame("LIST", circleId, CHAT).put("kind", "message").put("limit", limit), Set.of());
        } else {
            http.get("chat.history.rest", entriesPath(circleId, CHAT) + "?kind=message&limit=" + limit);
        }
    }

    private void count(final boolean overSocket) {
        // As Rune and Tolk do: the messages after the position read up to.
        final String circleId = room();
        final long after = read.getOrDefault(circleId, 0L);
        if (overSocket) {
            socket.request("chat.count", frame("COUNT", circleId, CHAT).put("kind", "message").put("after", after), Set.of());
        } else {
            http.get("chat.count.rest", "/circles/" + circleId + "/features/chat/count?kind=message&after=" + after);
        }
    }

    private void typing() {
        // The chat takes a signal every 2 s per connection; one sooner is dropped by the stone.
        final long now = System.nanoTime();
        if ((now - lastTyping) > 2_100_000_000L) {
            lastTyping = now;
            socket.fire("chat.typing", frame("SIGNAL", room(), CHAT).put("signal", "{\"t\":\"typing\",\"sent\":" + now + "}"));
        }
    }

    private void readEntry() {
        final Own entry = anyOwn(CHAT, "message");
        if (entry != null) {
            if (random.nextBoolean()) {
                socket.request("entry.get", frame("GET", entry.circleId(), entry.feature()).put("entryId", entry.entryId()), Set.of());
            } else {
                http.get("entry.get.rest", entriesPath(entry.circleId(), entry.feature()) + '/' + entry.entryId());
            }
        }
    }

    private void callSignal() {
        final String circleId = room();
        final List<String> others = new ArrayList<>(present.getOrDefault(circleId, Set.of()));
        others.remove(memberId);
        final ObjectNode frame = frame("SIGNAL", circleId, "calls")
                .put("signal", "{\"v\":1,\"t\":\"offer\",\"call\":\"" + name + "-" + (++counter) + "\",\"sent\":" + System.nanoTime() + ",\"sdp\":\"" + "x".repeat(random.nextInt(200, 3000)) + "\"}");
        if (!others.isEmpty() && random.nextBoolean()) {
            frame.put("to", others.get(random.nextInt(others.size())));
        }
        socket.fire("calls.signal", frame);
    }

    /** A plan with items: added, changed, closed and opened again, by its author. */
    private void plans() {
        final Own plan = anyOwn("plans", "plan");
        final int choice = random.nextInt(10);
        if ((plan == null) || (choice < 2)) {
            final String circleId = room();
            final String entryId = add("plans.plan", random.nextBoolean(), circleId, "plans", "plan", null,
                    "{\"v\":1,\"title\":\"plan " + (++counter) + " of " + name + "\"}", Set.of());
            if (entryId != null) {
                ownOf(circleId).add(new Own(circleId, "plans", "plan", entryId, null, false));
            }
        } else if (choice < 6 && !plan.closed()) {
            final String entryId = add("plans.item", random.nextBoolean(), plan.circleId(), "plans", "item", plan.entryId(),
                    "{\"v\":1,\"text\":\"item " + (++counter) + "\"}", Set.of());
            if (entryId != null) {
                ownOf(plan.circleId()).add(new Own(plan.circleId(), "plans", "item", entryId, plan.entryId(), false));
            }
        } else if (choice < 8) {
            final Own item = anyOwnAbout(plan);
            if ((item != null) && !plan.closed()) {
                change("plans.item.change", random.nextBoolean(), item, "{\"v\":1,\"text\":\"changed item " + (++counter) + "\",\"done\":true}");
            }
        } else {
            final boolean close = !plan.closed();
            final boolean done;
            if (random.nextBoolean()) {
                final JsonNode reply = socket.request("plans.close", frame("CLOSE", plan.circleId(), "plans").put("entryId", plan.entryId()).put("closed", close), Set.of());
                done = (reply != null) && "DONE".equals(reply.path("type").asText());
            } else {
                done = http.put("plans.close.rest", entriesPath(plan.circleId(), "plans") + '/' + plan.entryId() + "/closed",
                        Http.object().put("closed", close)).ok();
            }
            if (done) {
                final List<Own> list = ownOf(plan.circleId());
                list.set(list.indexOf(plan), new Own(plan.circleId(), "plans", "plan", plan.entryId(), null, close));
            }
        }
    }

    private Own anyOwnAbout(final Own about) {
        final List<Own> items = ownOf(about.circleId()).stream().filter(entry -> about.entryId().equals(entry.about())).toList();
        return items.isEmpty() ? null : items.get(random.nextInt(items.size()));
    }

    private void calendar() {
        final Own event = anyOwn("calendar", "event");
        final int choice = random.nextInt(10);
        if ((event == null) || (choice < 5)) {
            final String circleId = room();
            final String entryId = add("calendar.add", random.nextBoolean(), circleId, "calendar", "event", null,
                    "{\"v\":1,\"title\":\"meeting " + (++counter) + "\",\"start\":\"2026-12-0" + random.nextInt(1, 10) + "T10:00\"}", Set.of());
            if (entryId != null) {
                ownOf(circleId).add(new Own(circleId, "calendar", "event", entryId, null, false));
            }
        } else if (choice < 8) {
            change("calendar.change", random.nextBoolean(), event, "{\"v\":1,\"title\":\"moved " + (++counter) + "\",\"start\":\"2026-12-24T18:00\"}");
        } else if (remove("calendar.remove", random.nextBoolean(), event)) {
            ownOf(event.circleId()).remove(event);
        }
    }

    private void location() {
        final String circleId = room();
        final String position = "{\"v\":1,\"lat\":" + (55 + random.nextDouble()) + ",\"lon\":" + (12 + random.nextDouble()) + "}";
        // A position is kept once per member (the newer replaces it), a track by its age.
        add(random.nextBoolean() ? "location.position" : "location.track", random.nextBoolean(), circleId, "location",
                random.nextBoolean() ? "position" : "track", null, position, Set.of());
    }

    /** Leaves a room and joins it again, which the others hear as ABSENT and PRESENT. */
    private void presence() {
        final String circleId = room();
        socket.request("socket.LEAVE", Http.object().put("type", "LEAVE").put("circleId", circleId), Set.of());
        join(circleId);
    }

    private void join(final String circleId) {
        final JsonNode joined = socket.request("socket.JOIN", Http.object().put("type", "JOIN").put("circleId", circleId), Set.of());
        if ((joined != null) && "JOINED".equals(joined.path("type").asText())) {
            final Set<String> here = ConcurrentHashMap.newKeySet();
            joined.path("present").forEach(member -> here.add(member.asText()));
            present.put(circleId, here);
        }
    }

    // =========================================================================
    // Files
    // =========================================================================

    static byte[] content(final long seed, final int size) {
        final byte[] bytes = new byte[size];
        new SplittableRandom(seed).nextBytes(bytes);
        return bytes;
    }

    private int size() {
        final int max = world.settings.contentMax();
        return (random.nextInt(50) == 0)
                ? random.nextInt(4 << 20, (6 << 20) + 1)
                : (int) Math.min(max, Math.round(Math.exp(random.nextDouble() * Math.log(max))));
    }

    private Stored anyStored() {
        return stored.isEmpty() ? null : stored.get(random.nextInt(stored.size()));
    }

    private void keep(final Stored object) {
        stored.add(object);
        held += object.size();
        world.objectsOf(object.circleId()).add(object.dataId());
        while ((held > world.settings.heldMax()) || (stored.size() > HELD_OBJECTS)) {
            delete(stored.getFirst());
        }
    }

    private void forget(final Stored object) {
        stored.remove(object);
        held -= Math.max(0, object.size());
        world.objectsOf(object.circleId()).remove(object.dataId());
    }

    private void upload(final boolean inline) {
        final String circleId = room();
        final String folderId = (!folders.isEmpty() && random.nextInt(4) == 0) ? folders.get(random.nextInt(folders.size())) : null;
        if ((folderId != null) && !circleOfFolder(folderId).equals(circleId)) {
            return;
        }
        final long seed = random.nextLong();
        final int size = inline ? random.nextInt(1, 64 * 1024) : size();
        final byte[] bytes = content(seed, size);
        final ObjectNode body = Http.object().put("circleId", circleId).put("dataName", name + "-" + (++counter) + ".bin")
                .put("mediaType", "application/octet-stream");
        if (folderId != null) {
            body.put("folderId", folderId);
        }
        if (inline) {
            body.put("data", Base64.getEncoder().encodeToString(bytes));
        }
        final Http.Reply added = http.post(inline ? "data.add.inline" : "data.add", "/data", body);
        if (!added.ok()) {
            return;
        }
        final String dataId = added.text("dataId");
        String etag = added.text("etag");
        if (!inline) {
            final Http.Reply uploaded = http.bytes("data.upload", "PUT", "/data/" + dataId + "/content", bytes, "application/octet-stream", Map.of(), Set.of(), true);
            if (!uploaded.ok()) {
                world.objectsOf(circleId).add(dataId);
                stored.add(new Stored(dataId, circleId, folderId, seed, -1, null));
                return;
            }
            etag = (uploaded.header("ETag") != null) ? uploaded.header("ETag") : uploaded.text("etag");
        }
        keep(new Stored(dataId, circleId, folderId, seed, size, etag));
    }

    private final Map<String, String> folderCircles = new HashMap<>();

    private String circleOfFolder(final String folderId) {
        return folderCircles.getOrDefault(folderId, "");
    }

    private void folder() {
        if (folders.size() >= 6) {
            return;
        }
        final String circleId = room();
        final Http.Reply reply = http.post("data.folder", "/data", Http.object().put("circleId", circleId)
                .put("dataName", name + "-folder-" + (++counter)).put("typeName", "folder"));
        if (reply.ok()) {
            final String folderId = reply.text("dataId");
            folders.add(folderId);
            folderCircles.put(folderId, circleId);
            world.objectsOf(circleId).add(folderId);
        }
    }

    private void list() {
        if (!folders.isEmpty() && random.nextInt(5) == 0) {
            http.get("data.list.folder", "/data/" + folders.get(random.nextInt(folders.size())) + "?pageSize=100&pageNumber=1");
        } else {
            http.get("data.list", "/data?circleId=" + room() + "&pageSize=" + random.nextInt(10, 101) + "&pageNumber=1");
        }
    }

    private void fetchData() {
        http.post("data.fetch.json", "/data/fetchData", http.sessionBody().put("circleId", room()).put("pageSize", 50).put("pageNumber", 1));
    }

    private void metadata() {
        final Stored object = anyStored();
        if (object != null) {
            final Http.Reply reply = http.get("data.metadata", "/data/" + object.dataId());
            if (reply.ok() && (object.size() >= 0)) {
                final JsonNode first = reply.json().path("metadata").path(0);
                http.verdict("check.metadata", object.dataId().equals(first.path("dataId").asText()) && (first.path("size").asLong() == object.size()),
                        "metadata of " + object.dataId() + " says " + first);
            }
        }
    }

    private void download() {
        final Stored object = anyStored();
        if ((object != null) && (object.size() >= 0)) {
            verify(object, http.bytes("data.download", "GET", "/data/" + object.dataId() + "/content", null, null, Map.of(), Set.of(), true), "check.download");
        }
    }

    /** Compares a download with what was uploaded. */
    void verify(final Stored object, final Http.Reply reply, final String check) {
        if (reply.ok()) {
            final boolean same = Arrays.equals(reply.body(), content(object.seed(), object.size()));
            http.verdict(check, same, "the content of " + object.dataId() + " differs: " + reply.body().length + " bytes, uploaded " + object.size());
        }
    }

    private void range() {
        final Stored object = anyStored();
        if ((object != null) && (object.size() > 1)) {
            final int from = random.nextInt(object.size() - 1);
            final int to = random.nextInt(from, Math.min(object.size(), from + (2 << 20)));
            final Http.Reply reply = http.bytes("data.range", "GET", "/data/" + object.dataId() + "/content", null, null,
                    Map.of("Range", "bytes=" + from + '-' + to), Set.of(), true);
            if (reply.ok()) {
                final byte[] expected = Arrays.copyOfRange(content(object.seed(), object.size()), from, to + 1);
                http.verdict("check.range", (reply.code() == 206) && Arrays.equals(reply.body(), expected),
                        "range " + from + '-' + to + " of " + object.dataId() + ": HTTP " + reply.code() + ", " + reply.body().length + " bytes");
            }
        }
    }

    private void head() {
        final Stored object = anyStored();
        if ((object != null) && (object.size() >= 0)) {
            final Http.Reply reply = http.bytes("data.head", "HEAD", "/data/" + object.dataId() + "/content", null, null, Map.of(), Set.of(), true);
            if (reply.ok()) {
                http.verdict("check.head", String.valueOf(object.size()).equals(reply.header("Content-Length")),
                        "HEAD of " + object.dataId() + " says " + reply.header("Content-Length") + " bytes, uploaded " + object.size());
            }
        }
    }

    /** A download token, and the download with it instead of the Session, as a browser's media element does. */
    private void token() {
        final Stored object = anyStored();
        if ((object != null) && (object.size() >= 0)) {
            final Http.Reply reply = http.post("data.token", "/data/" + object.dataId() + "/token", null);
            if (reply.ok()) {
                verify(object, http.bytes("data.download.token", "GET", "/data/" + object.dataId() + "/content?token=" + Http.query(reply.text("token")),
                        null, null, Map.of(), Set.of(), false), "check.download.token");
            }
        }
    }

    private void thumbnail() {
        final Stored object = anyStored();
        if (object != null) {
            http.bytes("data.thumbnail", "PUT", "/data/" + object.dataId() + "/thumbnail", Base64.getDecoder().decode(PNG), "image/png", Map.of(), Set.of(), true);
        }
    }

    private void rename() {
        final Stored object = anyStored();
        if (object != null) {
            http.put("data.rename", "/data/" + object.dataId(), Http.object().put("dataName", name + "-renamed-" + (++counter) + ".bin"));
        }
    }

    /** New content for an object, only if it still has the content this user knows (If-Match). */
    private void replace() {
        final Stored object = anyStored();
        if ((object != null) && (object.etag() != null)) {
            final long seed = random.nextLong();
            final int size = size();
            final Http.Reply reply = http.bytes("data.replace", "PUT", "/data/" + object.dataId() + "/content", content(seed, size),
                    "application/octet-stream", Map.of("If-Match", object.etag()), Set.of(), true);
            if (reply.ok()) {
                final String etag = (reply.header("ETag") != null) ? reply.header("ETag") : reply.text("etag");
                stored.set(stored.indexOf(object), object.withContent(seed, size, etag));
                held += size - object.size();
            }
        }
    }

    private String otherRoom(final String circleId) {
        final List<String> others = circles.stream().filter(id -> !id.equals(circleId)).toList();
        return others.isEmpty() ? null : others.get(random.nextInt(others.size()));
    }

    private void copy() {
        final Stored object = anyStored();
        final String target = (object != null) ? otherRoom(object.circleId()) : null;
        if ((target != null) && (object.size() >= 0)) {
            final Http.Reply reply = http.post("data.copy", "/data/" + object.dataId() + "/copy", Http.object().put("targetCircleId", target));
            if (reply.ok()) {
                keep(new Stored(reply.text("dataId"), target, null, object.seed(), object.size(), null));
            }
        }
    }

    private void move() {
        final Stored object = anyStored();
        final String target = (object != null) ? otherRoom(object.circleId()) : null;
        if ((target != null) && (object.size() >= 0)) {
            final Http.Reply reply = http.post("data.move", "/data/" + object.dataId() + "/move", Http.object().put("targetCircleId", target));
            if (reply.ok()) {
                forget(object);
                final String dataId = (reply.text("dataId") != null) ? reply.text("dataId") : object.dataId();
                keep(new Stored(dataId, target, null, object.seed(), object.size(), null));
            }
        }
    }

    private void deleteOne() {
        final Stored object = anyStored();
        if (object != null) {
            delete(object);
        }
    }

    private void delete(final Stored object) {
        if (http.delete("data.delete", "/data/" + object.dataId()).ok()) {
            forget(object);
        } else {
            // Kept out of the way, but still expected in the room.
            stored.remove(object);
            held -= Math.max(0, object.size());
        }
    }

    // =========================================================================
    // Reading, and the account
    // =========================================================================

    private void read() {
        final String circleId = room();
        switch (random.nextInt(12)) {
            case 0 -> http.get("read.circles", "/circles");
            case 1 -> http.get("read.circle", "/circles/" + circleId);
            case 2 -> http.get("read.trustees", "/circles/" + circleId + "/trustees");
            case 3 -> http.get("read.members", "/members");
            case 4 -> http.get("read.member", "/members/" + memberId);
            case 5 -> http.get("read.features", "/features");
            case 6 -> http.get("read.circle.features", "/circles/" + circleId + "/features");
            case 7 -> http.get("read.dataTypes", "/dataTypes");
            case 8 -> http.post("read.fetchCircles.json", "/circles/fetchCircles", http.sessionBody());
            case 9 -> http.post("read.fetchTrustees.json", "/trustees/fetchTrustees", http.sessionBody().put("circleId", circleId));
            case 10 -> http.post("read.authenticated.json", "/authenticated", http.sessionBody());
            default -> http.post("read.version", "/version", Http.object());
        }
    }

    private void avatar() {
        http.post("account.avatar", "/members/updateMember", http.sessionBody().put("avatar", AVATARS[random.nextInt(AVATARS.length)]));
    }

    /** Logs out and in again, with a new socket: what a person does who closes the app. */
    private void relogin() {
        // The user ends its Session itself, so an error the stone sends the
        // old socket meanwhile is expected, as after a new passphrase.
        socket.sessionEnding();
        socket.close();
        http.post("account.logout", "/members/logout", http.sessionBody());
        loginAgain();
    }

    private void loginAgain() {
        http.session(null);
        if (login()) {
            connect();
        }
    }

    private void changePassphrase() {
        final String next = World.passphrase();
        socket.sessionEnding();
        final Http.Reply reply = http.post("account.passphrase", "/members/updateMember", Http.object().put("accountName", name)
                .put("credential", World.base64(passphrase)).put("credentialType", "PASSPHRASE").put("newCredential", World.base64(next)));
        if (reply.ok()) {
            passphrase = next;
            // A new passphrase ends the Session.
            socket.close();
            final Http.Reply ended = http.json("account.logout.after.passphrase", "POST", "/members/logout", http.sessionBody(), Set.of(403));
            http.verdict("check.passphrase.ends.session", !ended.ok(), name + " still had its Session after changing the passphrase");
            loginAgain();
        }
    }

    // =========================================================================
    // The checks at the end
    // =========================================================================

    /** Downloads every object this user holds and compares it with what was uploaded. */
    void verifyAll() {
        for (final Stored object : new ArrayList<>(stored)) {
            if (object.size() >= 0) {
                verify(object, http.bytes("final.download", "GET", "/data/" + object.dataId() + "/content", null, null, Map.of(), Set.of(), true), "final.check.content");
            }
        }
    }

    Http http() {
        return http;
    }

    // The think time between actions, and the pause before a new connection: deliberate, not a wait for a condition.
    @SuppressWarnings("java:S2925")
    private static void pause(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

}
