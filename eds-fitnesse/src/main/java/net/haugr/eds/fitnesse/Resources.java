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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.haugr.eds.client.ApiException;
import net.haugr.eds.client.ApiResponse;
import net.haugr.eds.client.api.CirclesApi;
import net.haugr.eds.client.api.DataApi;
import net.haugr.eds.client.api.DataTypesApi;
import net.haugr.eds.client.api.MembersApi;
import net.haugr.eds.client.api.StatusApi;
import net.haugr.eds.client.model.Action;
import net.haugr.eds.client.model.DownloadTokenResponse;
import net.haugr.eds.fitnesse.callers.CallShare;
import net.haugr.eds.client.model.EDSResponse;
import net.haugr.eds.client.model.Circle;
import net.haugr.eds.client.model.FetchCircleResponse;
import net.haugr.eds.client.model.FetchDataResponse;
import net.haugr.eds.client.model.FetchDataTypeResponse;
import net.haugr.eds.client.model.FetchMemberResponse;
import net.haugr.eds.client.model.FetchTrusteeResponse;
import net.haugr.eds.client.model.MemberRole;
import net.haugr.eds.client.model.Metadata;
import net.haugr.eds.client.model.StatusKeyRequest;
import net.haugr.eds.client.model.StatusKeyResponse;
import net.haugr.eds.client.model.StatusResponse;
import net.haugr.eds.client.model.ProcessCircleRequest;
import net.haugr.eds.client.model.ProcessCircleResponse;
import net.haugr.eds.client.model.ProcessDataRequest;
import net.haugr.eds.client.model.ProcessDataResponse;
import net.haugr.eds.client.model.ProcessDataTypeRequest;
import net.haugr.eds.client.model.ProcessDataTypeResponse;
import net.haugr.eds.client.model.ProcessMemberRequest;
import net.haugr.eds.client.model.ProcessMemberResponse;
import net.haugr.eds.client.model.ProcessTrusteeRequest;
import net.haugr.eds.client.model.ProcessTrusteeResponse;
import net.haugr.eds.client.model.TrustLevel;
import net.haugr.eds.fitnesse.callers.Calls;
import net.haugr.eds.fitnesse.exceptions.StopTestException;
import net.haugr.eds.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the ID-based resource API (PLAN.md, L1): one
 * row is one operation, named by its operationId in the contract. The
 * credential is the name a Session was stored under at login; the API only
 * accepts a Session, as Bearer token. The IDs are given by the names they
 * were stored under, and the ID a create returns is stored under the name in
 * the "new key" column.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class Resources extends EDSRequest<EDSResponse> {

    private String operation = null;
    private String memberKey = null;
    private String circleKey = null;
    private String dataKey = null;
    private String targetCircleKey = null;
    private String name = null;
    private String content = null;
    private String newCredential = null;
    private String role = null;
    private String trustLevel = null;
    private String type = null;
    private Boolean includeHidden = null;
    private String newKey = null;
    private String mediaType = null;
    private String secrets = null;
    private String externalKey = null;
    private int status = 0;

    public void setOperation(final String operation) {
        this.operation = Converter.preCheck(operation);
    }

    public void setMemberId(final String memberId) {
        this.memberKey = Converter.preCheck(memberId);
    }

    public void setCircleId(final String circleId) {
        this.circleKey = Converter.preCheck(circleId);
    }

    public void setDataId(final String dataId) {
        this.dataKey = Converter.preCheck(dataId);
    }

    public void setTargetCircleId(final String targetCircleId) {
        this.targetCircleKey = Converter.preCheck(targetCircleId);
    }

    public void setName(final String name) {
        this.name = Converter.preCheck(name);
    }

    public void setContent(final String content) {
        this.content = Converter.preCheck(content);
    }

    public void setNewCredential(final String newCredential) {
        this.newCredential = Converter.preCheck(newCredential);
    }

    public void setRole(final String role) {
        this.role = Converter.preCheck(role);
    }

    public void setTrustLevel(final String trustLevel) {
        this.trustLevel = Converter.preCheck(trustLevel);
    }

    public void setIncludeHidden(final String includeHidden) {
        final String value = Converter.preCheck(includeHidden);
        this.includeHidden = (value != null) ? Boolean.valueOf(value) : null;
    }

    public void setType(final String type) {
        this.type = Converter.preCheck(type);
    }

    /** The media type of a thumbnail (putThumbnail). */
    public void setMediaType(final String mediaType) {
        this.mediaType = Converter.preCheck(mediaType);
    }

    /** The secrets of Trustees (putCircleSecrets), as {@code member_id=secret, ...}. */
    public void setSecrets(final String secrets) {
        this.secrets = Converter.preCheck(secrets);
    }

    /** The external key of the Circle (putCircleSecrets). */
    public void setExternalKey(final String externalKey) {
        this.externalKey = Converter.preCheck(externalKey);
    }

    public void setNewKey(final String newKey) {
        this.newKey = Converter.preCheck(newKey);
    }

    /** The HTTP status, which is the return code for this API. */
    public String status() {
        return (status != 0) ? String.valueOf(status) : null;
    }

    /** The name the returned ID is stored under. */
    public String id() {
        final String id = switch (response) {
            case ProcessMemberResponse member -> member.getMemberId();
            case ProcessCircleResponse circle -> circle.getCircleId();
            case ProcessDataResponse data -> data.getDataId();
            case DownloadTokenResponse token -> token.getToken();
            case null, default -> null;
        };
        return getKey(id);
    }

    /** The thumbnail of the first listed Data Object, as the name its ID is stored under. */
    public String thumbnailId() {
        if ((response instanceof FetchDataResponse data) && !data.getMetadata().isEmpty()) {
            final String thumbnailId = data.getMetadata().getFirst().getThumbnailId();
            return (thumbnailId != null) ? getKey(thumbnailId) : null;
        }
        return null;
    }

    /**
     * The listed records: account names with role, Circle names, Trustees
     * with trust level, Data Object names or DataType names.
     */
    public String names() {
        final List<String> names = switch (response) {
            case FetchMemberResponse members -> members.getMembers().stream()
                    .map(member -> member.getAccountName() + ':' + member.getMemberRole()).toList();
            case FetchCircleResponse circles -> circles.getCircles().stream()
                    .map(circle -> circle.getCircleName()).toList();
            case FetchTrusteeResponse trustees -> trustees.getTrustees().stream()
                    .map(trustee -> trustee.getAccountName() + ':' + trustee.getTrustLevel()).toList();
            case FetchDataResponse data -> data.getMetadata().stream()
                    .map(Metadata::getDataName).toList();
            case FetchDataTypeResponse dataTypes -> dataTypes.getDataTypes().stream()
                    .map(dataType -> dataType.getTypeName()).toList();
            case StatusKeyResponse keys when status == 200 -> keys.getKeys();
            case null, default -> null;
        };
        return (names != null) ? names.stream().sorted().collect(Collectors.joining(", ", "[", "]")) : null;
    }

    /** Whether the Circle has a time its secret is to be rotated next (getCircleById): set or null. */
    public String rotationDue() {
        final Circle circle = circle();
        return (circle != null) ? ((circle.getRotationDue() != null) ? "set" : "null") : null;
    }

    /**
     * Whether the Circle's secret was rotated since the Circle was made, and
     * no rotation is under way (waitForRotation).
     */
    public String rotated() {
        final Circle circle = circle();
        return (circle != null) ? String.valueOf(isRotated(circle)) : null;
    }

    private Circle circle() {
        return ((status == 200) && (response instanceof FetchCircleResponse found) && (found.getCircles() != null) && !found.getCircles().isEmpty())
                ? found.getCircles().getFirst() : null;
    }

    private static boolean isRotated(final Circle circle) {
        return (circle.getRotation() == null) && (circle.getSecretRotated() != null) && (circle.getAdded() != null)
                && LocalDateTime.parse(circle.getSecretRotated()).isAfter(LocalDateTime.parse(circle.getAdded()));
    }

    /** Whether the stone is awake (getStatus); null for a refused request. */
    public String awake() {
        return ((status == 200) && (response instanceof StatusResponse stone)) ? String.valueOf(stone.getAwake()) : null;
    }

    /** The version of EDS (getStatus). */
    public String version() {
        return ((status == 200) && (response instanceof StatusResponse stone)) ? stone.getVersion() : null;
    }

    /** Whether the answer has samples of the load and of the inventory (getStatus). */
    public String samples() {
        return ((status == 200) && (response instanceof StatusResponse stone)) ? String.valueOf((stone.getLoad() != null) && (stone.getInventory() != null)) : null;
    }

    /** The Data Types of the listed Data Objects, sorted. */
    public String types() {
        return (response instanceof FetchDataResponse data)
                ? data.getMetadata().stream().map(Metadata::getTypeName).sorted().collect(Collectors.joining(", ", "[", "]"))
                : null;
    }

    /** Size of the content of the first listed Data Object. */
    public String size() {
        if ((response instanceof FetchDataResponse data) && !data.getMetadata().isEmpty()) {
            final Long size = data.getMetadata().getFirst().getSize();
            return (size != null) ? String.valueOf(size) : null;
        }
        return null;
    }

    /** Whether a fetched Data Object came without its content. */
    public String withoutContent() {
        return (response instanceof FetchDataResponse data) ? String.valueOf(data.getData() == null) : null;
    }

    @Override
    public void execute() {
        final Map<String, String> bearer = Calls.bearer(credential);
        final MembersApi members = Calls.api(requestUrl, MembersApi.class, MembersApi::new);
        final CirclesApi circles = Calls.api(requestUrl, CirclesApi.class, CirclesApi::new);
        final DataApi data = Calls.api(requestUrl, DataApi.class, DataApi::new);
        final DataTypesApi dataTypes = Calls.api(requestUrl, DataTypesApi.class, DataTypesApi::new);
        final StatusApi stone = Calls.api(requestUrl, StatusApi.class, StatusApi::new);
        final String memberId = id(memberKey);
        final String circleId = id(circleKey);
        final String dataId = id(dataKey);

        switch (operation) {
            case "getMembers" -> call(FetchMemberResponse.class, () -> members.getMembersWithHttpInfo(bearer));
            case "getMemberById" -> call(FetchMemberResponse.class, () -> members.getMemberByIdWithHttpInfo(memberId, bearer));
            case "postMembers" -> call(ProcessMemberResponse.class, () -> members.postMembersWithHttpInfo(memberRequest(), bearer));
            case "putMemberById" -> call(ProcessMemberResponse.class, () -> members.putMemberByIdWithHttpInfo(memberId, memberRequest(), bearer));
            case "deleteMemberById" -> call(ProcessMemberResponse.class, () -> members.deleteMemberByIdWithHttpInfo(memberId, bearer));
            case "getCircles" -> call(FetchCircleResponse.class, () -> circles.getCirclesWithHttpInfo(bearer));
            case "getCircleById" -> call(FetchCircleResponse.class, () -> circles.getCircleByIdWithHttpInfo(circleId, bearer));
            case "postCircles" -> call(ProcessCircleResponse.class, () -> circles.postCirclesWithHttpInfo(circleRequest(), bearer));
            case "putCircleById" -> call(ProcessCircleResponse.class, () -> circles.putCircleByIdWithHttpInfo(circleId, circleRequest(), bearer));
            case "putCircleRotation" -> call(ProcessCircleResponse.class, () -> circles.putCircleRotationWithHttpInfo(circleId, bearer));
            case "waitForRotation" -> waitForRotation(circles, circleId, bearer);
            case "deleteCircleById" -> call(ProcessCircleResponse.class, () -> circles.deleteCircleByIdWithHttpInfo(circleId, bearer));
            case "getCircleTrustees" -> call(FetchTrusteeResponse.class, () -> circles.getCircleTrusteesWithHttpInfo(circleId, bearer));
            case "postCircleTrustees" -> call(ProcessTrusteeResponse.class, () -> circles.postCircleTrusteesWithHttpInfo(circleId, trusteeRequest(memberId), bearer));
            case "putCircleTrusteeById" -> call(ProcessTrusteeResponse.class, () -> circles.putCircleTrusteeByIdWithHttpInfo(circleId, memberId, trusteeRequest(null), bearer));
            case "putCircleSecrets" -> call(ProcessTrusteeResponse.class, () -> circles.putCircleSecretsWithHttpInfo(circleId,
                    new ProcessTrusteeRequest().secrets((secrets != null) ? trusteeSecrets(secrets) : null).circleKey(externalKey), bearer));
            case "deleteCircleTrusteeById" -> call(ProcessTrusteeResponse.class, () -> circles.deleteCircleTrusteeByIdWithHttpInfo(circleId, memberId, bearer));
            case "getData" -> call(FetchDataResponse.class, () -> data.getDataWithHttpInfo(circleId, includeHidden, null, null, bearer));
            case "getDataById" -> call(FetchDataResponse.class, () -> data.getDataByIdWithHttpInfo(dataId, includeHidden, null, null, bearer));
            case "postData" -> call(ProcessDataResponse.class, () -> data.postDataWithHttpInfo(dataRequest(circleId), bearer));
            case "putDataById" -> call(ProcessDataResponse.class, () -> data.putDataByIdWithHttpInfo(dataId, dataRequest(null), bearer));
            case "deleteDataById" -> call(ProcessDataResponse.class, () -> data.deleteDataByIdWithHttpInfo(dataId, bearer));
            case "postDataCopy" -> call(ProcessDataResponse.class, () -> data.postDataCopyWithHttpInfo(dataId, dataRequest(null), bearer));
            case "postDataMove" -> call(ProcessDataResponse.class, () -> data.postDataMoveWithHttpInfo(dataId, dataRequest(null), bearer));
            case "createDownloadToken" -> call(DownloadTokenResponse.class, () -> data.createDownloadTokenWithHttpInfo(dataId, bearer));
            case "putThumbnail" -> call(ProcessDataResponse.class, () -> CallShare.putThumbnail(requestUrl, credential, dataId,
                    (content != null) ? content.getBytes(java.nio.charset.StandardCharsets.UTF_8) : new byte[0], mediaType));
            case "getDataTypes" -> call(FetchDataTypeResponse.class, () -> dataTypes.getDataTypesWithHttpInfo(bearer));
            case "putDataTypeByName" -> call(ProcessDataTypeResponse.class, () -> dataTypes.putDataTypeByNameWithHttpInfo(name, new ProcessDataTypeRequest().type(type), bearer));
            case "deleteDataTypeByName" -> call(ProcessDataTypeResponse.class, () -> dataTypes.deleteDataTypeByNameWithHttpInfo(name, bearer));
            case "getStatus" -> call(StatusResponse.class, () -> stone.getStatusWithHttpInfo(null, null, bearer));
            case "getStatusKeys" -> call(StatusKeyResponse.class, () -> stone.getStatusKeysWithHttpInfo(bearer));
            case "postStatusKey" -> call(StatusKeyResponse.class, () -> stone.postStatusKeyWithHttpInfo(new StatusKeyRequest().label(name), bearer));
            case "deleteStatusKey" -> call(StatusKeyResponse.class, () -> stone.deleteStatusKeyWithHttpInfo(name, bearer));
            case null, default -> throw new StopTestException("Unknown operation: " + operation);
        }

        if ((status == 200) && (newKey != null) && (response instanceof StatusKeyResponse created)) {
            setStatusKey(newKey, created.getKey());
        } else if ((status == 200) && (newKey != null)) {
            final String id = switch (response) {
                case ProcessMemberResponse member -> member.getMemberId();
                case ProcessCircleResponse circle -> circle.getCircleId();
                case ProcessDataResponse processed -> processed.getDataId();
                case DownloadTokenResponse token -> token.getToken();
                default -> null;
            };
            processId(Action.CREATE, null, newKey, id);
        }
        if (status == 200) {
            final String removed = switch (operation) {
                case "deleteMemberById" -> memberKey;
                case "deleteCircleById" -> circleKey;
                case "deleteDataById", "postDataMove" -> dataKey;
                default -> null;
            };
            if (removed != null) {
                processId(Action.DELETE, removed, null, (String) null);
            }
        }
    }

    /**
     * Reads the Circle until its secret was rotated and no rotation is under
     * way, as the rotation runs in the background of the stone; at most 60
     * seconds.
     */
    private void waitForRotation(final CirclesApi circles, final String circleId, final Map<String, String> bearer) {
        final long end = System.currentTimeMillis() + 60_000;
        do {
            call(FetchCircleResponse.class, () -> circles.getCircleByIdWithHttpInfo(circleId, bearer));
            final Circle circle = circle();
            if ((circle == null) || isRotated(circle)) {
                return;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        } while (System.currentTimeMillis() < end);
    }

    private String id(final String key) {
        final String id = getId(key);
        return (id != null) ? id : key;
    }

    private ProcessMemberRequest memberRequest() {
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setNewAccountName(name);
        request.setNewCredential(Converter.convertBytes(newCredential));
        request.setMemberRole((role != null) ? MemberRole.valueOf(role) : null);
        return request;
    }

    private ProcessCircleRequest circleRequest() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setCircleName(name);
        return request;
    }

    private ProcessTrusteeRequest trusteeRequest(final String memberId) {
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        request.setMemberId(memberId);
        request.setTrustLevel((trustLevel != null) ? TrustLevel.valueOf(trustLevel) : null);
        return request;
    }

    private ProcessDataRequest dataRequest(final String circleId) {
        final ProcessDataRequest request = new ProcessDataRequest();
        request.setCircleId(circleId);
        request.setTargetCircleId(id(targetCircleKey));
        request.setDataName(name);
        request.setData(Converter.convertBytes(content));
        return request;
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
        memberKey = null;
        circleKey = null;
        dataKey = null;
        targetCircleKey = null;
        name = null;
        content = null;
        newCredential = null;
        role = null;
        trustLevel = null;
        type = null;
        includeHidden = null;
        newKey = null;
        mediaType = null;
        secrets = null;
        externalKey = null;
        status = 0;
        response = null;
    }
}
