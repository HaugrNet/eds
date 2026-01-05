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
package net.haugr.eds.spring;

import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.FetchDataTypeRequest;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.requests.FetchSignatureRequest;
import net.haugr.eds.api.requests.FetchTrusteeRequest;
import net.haugr.eds.api.requests.InventoryRequest;
import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessDataTypeRequest;
import net.haugr.eds.api.requests.ProcessMemberRequest;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.requests.SanityRequest;
import net.haugr.eds.api.requests.SettingRequest;
import net.haugr.eds.api.requests.SignRequest;
import net.haugr.eds.api.requests.VerifyRequest;
import net.haugr.eds.api.responses.AuthenticateResponse;
import net.haugr.eds.api.responses.FetchCircleResponse;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.FetchDataTypeResponse;
import net.haugr.eds.api.responses.FetchMemberResponse;
import net.haugr.eds.api.responses.FetchSignatureResponse;
import net.haugr.eds.api.responses.FetchTrusteeResponse;
import net.haugr.eds.api.responses.InventoryResponse;
import net.haugr.eds.api.responses.MasterKeyResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.responses.ProcessDataTypeResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import net.haugr.eds.api.responses.SanityResponse;
import net.haugr.eds.api.responses.SettingResponse;
import net.haugr.eds.api.responses.SignResponse;
import net.haugr.eds.api.responses.VerifyResponse;
import net.haugr.eds.api.responses.VersionResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring REST Controller for all EDS endpoints.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class EDSRestController {

    private final SpringManagementBean managementBean;
    private final SpringShareBean shareBean;

    /**
     * Constructor with dependency injection.
     *
     * @param managementBean Spring Management Bean
     * @param shareBean      Spring Share Bean
     */
    public EDSRestController(final SpringManagementBean managementBean, final SpringShareBean shareBean) {
        this.managementBean = managementBean;
        this.shareBean = shareBean;
    }

    // =========================================================================
    // System Endpoints
    // =========================================================================

    @RequestMapping(value = "/version", method = {RequestMethod.GET, RequestMethod.POST})
    public VersionResponse version() {
        return managementBean.version();
    }

    @PostMapping("/settings")
    public SettingResponse settings(@RequestBody(required = false) final SettingRequest request) {
        return managementBean.settings(request != null ? request : new SettingRequest());
    }

    @PostMapping("/masterKey")
    public MasterKeyResponse masterKey(@RequestBody final MasterKeyRequest request) {
        return managementBean.masterKey(request);
    }

    @PostMapping("/sanitized")
    public SanityResponse sanitized(@RequestBody final SanityRequest request) {
        return managementBean.sanity(request);
    }

    @PostMapping("/inventory")
    public InventoryResponse inventory(@RequestBody final InventoryRequest request) {
        return managementBean.inventory(request);
    }

    @PostMapping("/authenticated")
    public AuthenticateResponse authenticated(@RequestBody final Authentication request) {
        return managementBean.authenticated(request);
    }

    // =========================================================================
    // Member Endpoints
    // =========================================================================

    @PostMapping("/members/createMember")
    public ProcessMemberResponse createMember(@RequestBody final ProcessMemberRequest request) {
        request.setAction(Action.CREATE);
        return managementBean.processMember(request);
    }

    @PostMapping("/members/inviteMember")
    public ProcessMemberResponse inviteMember(@RequestBody final ProcessMemberRequest request) {
        request.setAction(Action.INVITE);
        return managementBean.processMember(request);
    }

    @PostMapping("/members/login")
    public ProcessMemberResponse login(@RequestBody final ProcessMemberRequest request) {
        request.setAction(Action.LOGIN);
        return managementBean.processMember(request);
    }

    @PostMapping("/members/logout")
    public ProcessMemberResponse logout(@RequestBody final ProcessMemberRequest request) {
        request.setAction(Action.LOGOUT);
        return managementBean.processMember(request);
    }

    @PostMapping("/members/alterMember")
    public ProcessMemberResponse alterMember(@RequestBody final ProcessMemberRequest request) {
        request.setAction(Action.ALTER);
        return managementBean.processMember(request);
    }

    @PostMapping("/members/updateMember")
    public ProcessMemberResponse updateMember(@RequestBody final ProcessMemberRequest request) {
        request.setAction(Action.UPDATE);
        return managementBean.processMember(request);
    }

    @PostMapping("/members/invalidate")
    public ProcessMemberResponse invalidate(@RequestBody final ProcessMemberRequest request) {
        request.setAction(Action.INVALIDATE);
        return managementBean.processMember(request);
    }

    @PostMapping("/members/deleteMember")
    public ProcessMemberResponse deleteMember(@RequestBody final ProcessMemberRequest request) {
        request.setAction(Action.DELETE);
        return managementBean.processMember(request);
    }

    @PostMapping("/members/fetchMembers")
    public FetchMemberResponse fetchMembers(@RequestBody final FetchMemberRequest request) {
        return managementBean.fetchMembers(request);
    }

    // =========================================================================
    // Circle Endpoints
    // =========================================================================

    @PostMapping("/circles/createCircle")
    public ProcessCircleResponse createCircle(@RequestBody final ProcessCircleRequest request) {
        request.setAction(Action.CREATE);
        return managementBean.processCircle(request);
    }

    @PostMapping("/circles/updateCircle")
    public ProcessCircleResponse updateCircle(@RequestBody final ProcessCircleRequest request) {
        request.setAction(Action.UPDATE);
        return managementBean.processCircle(request);
    }

    @PostMapping("/circles/deleteCircle")
    public ProcessCircleResponse deleteCircle(@RequestBody final ProcessCircleRequest request) {
        request.setAction(Action.DELETE);
        return managementBean.processCircle(request);
    }

    @PostMapping("/circles/fetchCircles")
    public FetchCircleResponse fetchCircles(@RequestBody final FetchCircleRequest request) {
        return managementBean.fetchCircles(request);
    }

    // =========================================================================
    // Trustee Endpoints
    // =========================================================================

    @PostMapping("/trustees/addTrustee")
    public ProcessTrusteeResponse addTrustee(@RequestBody final ProcessTrusteeRequest request) {
        request.setAction(Action.ADD);
        return managementBean.processTrustee(request);
    }

    @PostMapping("/trustees/alterTrustee")
    public ProcessTrusteeResponse alterTrustee(@RequestBody final ProcessTrusteeRequest request) {
        request.setAction(Action.ALTER);
        return managementBean.processTrustee(request);
    }

    @PostMapping("/trustees/removeTrustee")
    public ProcessTrusteeResponse removeTrustee(@RequestBody final ProcessTrusteeRequest request) {
        request.setAction(Action.REMOVE);
        return managementBean.processTrustee(request);
    }

    @PostMapping("/trustees/fetchTrustees")
    public FetchTrusteeResponse fetchTrustees(@RequestBody final FetchTrusteeRequest request) {
        return managementBean.fetchTrustees(request);
    }

    // =========================================================================
    // DataType Endpoints
    // =========================================================================

    @PostMapping("/dataTypes/processDataType")
    public ProcessDataTypeResponse processDataType(@RequestBody final ProcessDataTypeRequest request) {
        return shareBean.processDataType(request);
    }

    @PostMapping("/dataTypes/deleteDataType")
    public ProcessDataTypeResponse deleteDataType(@RequestBody final ProcessDataTypeRequest request) {
        request.setAction(Action.DELETE);
        return shareBean.processDataType(request);
    }

    @PostMapping("/dataTypes/fetchDataTypes")
    public FetchDataTypeResponse fetchDataTypes(@RequestBody final FetchDataTypeRequest request) {
        return shareBean.fetchDataTypes(request);
    }

    // =========================================================================
    // Data Endpoints
    // =========================================================================

    @PostMapping("/data/addData")
    public ProcessDataResponse addData(@RequestBody final ProcessDataRequest request) {
        request.setAction(Action.ADD);
        return shareBean.processData(request);
    }

    @PostMapping("/data/copyData")
    public ProcessDataResponse copyData(@RequestBody final ProcessDataRequest request) {
        request.setAction(Action.COPY);
        return shareBean.processData(request);
    }

    @PostMapping("/data/moveData")
    public ProcessDataResponse moveData(@RequestBody final ProcessDataRequest request) {
        request.setAction(Action.MOVE);
        return shareBean.processData(request);
    }

    @PostMapping("/data/updateData")
    public ProcessDataResponse updateData(@RequestBody final ProcessDataRequest request) {
        request.setAction(Action.UPDATE);
        return shareBean.processData(request);
    }

    @PostMapping("/data/deleteData")
    public ProcessDataResponse deleteData(@RequestBody final ProcessDataRequest request) {
        request.setAction(Action.DELETE);
        return shareBean.processData(request);
    }

    @PostMapping("/data/fetchData")
    public FetchDataResponse fetchData(@RequestBody final FetchDataRequest request) {
        return shareBean.fetchData(request);
    }

    // =========================================================================
    // Signature Endpoints
    // =========================================================================

    @PostMapping("/signatures/signDocument")
    public SignResponse signDocument(@RequestBody final SignRequest request) {
        return shareBean.sign(request);
    }

    @PostMapping("/signatures/verifySignature")
    public VerifyResponse verifySignature(@RequestBody final VerifyRequest request) {
        return shareBean.verify(request);
    }

    @PostMapping("/signatures/fetchSignatures")
    public FetchSignatureResponse fetchSignatures(@RequestBody final FetchSignatureRequest request) {
        return shareBean.fetchSignatures(request);
    }
}
