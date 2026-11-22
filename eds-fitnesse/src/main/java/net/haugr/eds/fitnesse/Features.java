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
package net.haugr.eds.fitnesse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import net.haugr.eds.client.ApiException;
import net.haugr.eds.client.ApiResponse;
import net.haugr.eds.client.api.FeaturesApi;
import net.haugr.eds.client.model.Action;
import net.haugr.eds.client.model.CircleFeatureRequest;
import net.haugr.eds.client.model.CircleFeatureResponse;
import net.haugr.eds.client.model.EDSResponse;
import net.haugr.eds.client.model.Entry;
import net.haugr.eds.client.model.EntryRequest;
import net.haugr.eds.client.model.EntryResponse;
import net.haugr.eds.client.model.FeatureRequest;
import net.haugr.eds.client.model.FeatureResponse;
import net.haugr.eds.client.model.FeatureState;
import net.haugr.eds.fitnesse.callers.Calls;
import net.haugr.eds.fitnesse.exceptions.StopTestException;
import net.haugr.eds.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the features (FEATURES.md): the features a stone offers,
 * the features of a Circle, and their entries. The operations are named by
 * their operationId in the API contract, as in {@link Resources}.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class Features extends EDSRequest<EDSResponse> {

    private String operation = null;
    private String circleKey = null;
    private String feature = null;
    private String declaration = null;
    private String state = null;
    private String kind = null;
    private String aboutKey = null;
    private String entryKey = null;
    private String name = null;
    private String content = null;
    private Boolean closed = null;
    private String newKey = null;
    private int status = 0;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setOperation(final String operation) {
        this.operation = Converter.preCheck(operation);
    }

    public void setCircleId(final String circleId) {
        this.circleKey = Converter.preCheck(circleId);
    }

    public void setFeature(final String feature) {
        this.feature = Converter.preCheck(feature);
    }

    public void setDeclaration(final String declaration) {
        this.declaration = Converter.preCheck(declaration);
    }

    public void setState(final String state) {
        this.state = Converter.preCheck(state);
    }

    public void setKind(final String kind) {
        this.kind = Converter.preCheck(kind);
    }

    public void setAbout(final String about) {
        this.aboutKey = Converter.preCheck(about);
    }

    public void setEntryId(final String entryId) {
        this.entryKey = Converter.preCheck(entryId);
    }

    public void setName(final String name) {
        this.name = Converter.preCheck(name);
    }

    public void setContent(final String content) {
        this.content = Converter.preCheck(content);
    }

    public void setClosed(final String closed) {
        final String value = Converter.preCheck(closed);
        this.closed = (value != null) ? Boolean.valueOf(value) : null;
    }

    public void setNewKey(final String newKey) {
        this.newKey = Converter.preCheck(newKey);
    }

    public String status() {
        return (status != 0) ? String.valueOf(status) : null;
    }

    /**
     * The features of the response: of the stone with their version, of a
     * Circle with their state.
     *
     * @return The features, sorted by name
     */
    public String features() {
        return switch (response) {
            case FeatureResponse offered -> offered.getFeatures().stream()
                    .map(found -> found.getName() + ":v" + found.getVersion())
                    .sorted().collect(Collectors.joining(", ", "[", "]"));
            case CircleFeatureResponse used -> used.getFeatures().stream()
                    .map(found -> found.getName() + ':' + found.getState())
                    .sorted().collect(Collectors.joining(", ", "[", "]"));
            case null, default -> null;
        };
    }

    /**
     * The entries of the response, in the order of their positions: the
     * kind, the author and the content as text, and what EDS notes beside.
     *
     * @return The entries
     */
    public String entries() {
        if (!(response instanceof EntryResponse found) || (found.getCount() != null)) {
            return null;
        }
        return found.getEntries().stream()
                .sorted(Comparator.comparing(Entry::getPosition))
                .map(entry -> entry.getKind() + ':' + entry.getAccountName() + ':' + text(entry)
                        + (Boolean.TRUE.equals(entry.getClosed()) ? ":closed" : "")
                        + ((entry.getChanged() != null) ? ":changed" : ""))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    public String count() {
        return ((response instanceof EntryResponse found) && (found.getCount() != null)) ? String.valueOf(found.getCount()) : null;
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    @Override
    public void execute() {
        final Map<String, String> bearer = Calls.bearer(credential);
        final FeaturesApi api = Calls.api(requestUrl, FeaturesApi.class, FeaturesApi::new);
        final String circleId = id(circleKey);
        final String entryId = id(entryKey);

        switch (operation) {
            case "getFeatures" -> call(FeatureResponse.class, () -> api.getFeaturesWithHttpInfo(bearer));
            case "putFeature" -> call(FeatureResponse.class, () -> api.putFeatureWithHttpInfo(feature, new FeatureRequest().declaration(declaration), bearer));
            case "deleteFeature" -> call(FeatureResponse.class, () -> api.deleteFeatureWithHttpInfo(feature, bearer));
            case "getCircleFeatures" -> call(CircleFeatureResponse.class, () -> api.getCircleFeaturesWithHttpInfo(circleId, bearer));
            case "putCircleFeature" -> call(CircleFeatureResponse.class, () -> api.putCircleFeatureWithHttpInfo(circleId, feature,
                    new CircleFeatureRequest().state((state != null) ? FeatureState.valueOf(state) : null), bearer));
            case "deleteCircleFeature" -> call(CircleFeatureResponse.class, () -> api.deleteCircleFeatureWithHttpInfo(circleId, feature, bearer));
            case "getEntries" -> call(EntryResponse.class, () -> api.getEntriesWithHttpInfo(circleId, feature, id(aboutKey), null, null, null, kind, null, name, null, bearer));
            case "postEntry" -> call(EntryResponse.class, () -> api.postEntryWithHttpInfo(circleId, feature, entryRequest(), bearer));
            case "getEntryCount" -> call(EntryResponse.class, () -> api.getEntryCountWithHttpInfo(circleId, feature, null, kind, bearer));
            case "getEntry" -> call(EntryResponse.class, () -> api.getEntryWithHttpInfo(circleId, entryId, feature, bearer));
            case "putEntry" -> call(EntryResponse.class, () -> api.putEntryWithHttpInfo(circleId, entryId, feature, entryRequest(), bearer));
            case "deleteEntry" -> call(EntryResponse.class, () -> api.deleteEntryWithHttpInfo(circleId, entryId, feature, bearer));
            case "putEntryClosed" -> call(EntryResponse.class, () -> api.putEntryClosedWithHttpInfo(circleId, entryId, feature, new EntryRequest().closed(closed), bearer));
            case null, default -> throw new StopTestException("Unknown operation: " + operation);
        }

        if ((status == 200) && (newKey != null) && (response instanceof EntryResponse added) && !added.getEntries().isEmpty()) {
            processId(Action.CREATE, null, newKey, added.getEntries().getFirst().getEntryId());
        }
        if ((status == 200) && "deleteEntry".equals(operation)) {
            processId(Action.DELETE, entryKey, null, (String) null);
        }
    }

    private String id(final String key) {
        final String id = getId(key);
        return (id != null) ? id : key;
    }

    private EntryRequest entryRequest() {
        final EntryRequest request = new EntryRequest();
        request.setKind(kind);
        request.setAbout(id(aboutKey));
        request.setName(name);
        request.setContent((content != null) ? Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)) : null);

        return request;
    }

    private static String text(final Entry entry) {
        return (entry.getContent() != null) ? new String(Base64.getDecoder().decode(entry.getContent()), StandardCharsets.UTF_8) : "-";
    }

    private <T extends EDSResponse> void call(final Class<T> type, final Calls.Call<ApiResponse<T>> call) {
        try {
            final ApiResponse<T> result = call.run();
            status = result.getStatusCode();
            response = result.getData();
        } catch (ApiException e) {
            status = e.getCode();
            response = Calls.readError(requestUrl, type, e);
        }
    }

    @Override
    public void reset() {
        super.reset();
        operation = null;
        circleKey = null;
        feature = null;
        declaration = null;
        state = null;
        kind = null;
        aboutKey = null;
        entryKey = null;
        name = null;
        content = null;
        closed = null;
        newKey = null;
        status = 0;
        response = null;
    }
}
