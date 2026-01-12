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
package net.haugr.eds.core.services;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.requests.SettingRequest;
import net.haugr.eds.core.ManagementBean;
import net.haugr.eds.core.model.Settings;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * <p>REST interface for the Setting functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_SETTINGS)
@Tag(name = "Settings", description = "Operations for managing system settings.")
public class SettingService {

    private final ManagementBean managementBean;
    private final Settings settings;

    public SettingService() {
        this(null);
    }

    @Inject
    public SettingService(final ManagementBean managementBean) {
        this.managementBean = managementBean;
        this.settings = Settings.getInstance();
    }

    /**
     * The REST Settings Endpoint.
     *
     * @param settingRequest Settings Request
     * @return Settings Response
     */
    @Operation(
            summary = "Manage settings",
            description = "Allows the System Administrator to read and alter the settings of this EDS system. " +
                    "Some fields cannot be altered once the system is started, as it may have fatal consequences for running the system. " +
                    "Available settings include: " +
                    "eds.crypto.symmetric.algorithm (AES encryption algorithm for data), " +
                    "eds.crypto.asymmetric.algorithm (RSA algorithm for key sharing), " +
                    "eds.crypto.signature.algorithm (SHA algorithm for signatures), " +
                    "eds.crypto.pbe.algorithm (Password Based Encryption algorithm), " +
                    "eds.crypto.pbe.iterations (PBKDF iteration count), " +
                    "eds.crypto.hash.algorithm (hashing algorithm for checksums), " +
                    "eds.system.salt (system-specific salt for PBE), " +
                    "eds.system.locale (locale for string handling), " +
                    "eds.system.charset (character set for encoding), " +
                    "eds.show.all.circles (whether non-trustees can view all circles), " +
                    "eds.show.trustees (whether members can view unrelated members), " +
                    "eds.sanity.check.startup (enable sanity checks at startup), " +
                    "eds.sanity.check.interval.days (sanity check interval), " +
                    "eds.session.timeout.minutes (session duration), " +
                    "eds.masterkey.url (URL for master key secret), " +
                    "eds.cors.value (CORS configuration).")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response settings(@NotNull final SettingRequest settingRequest) {
        return CommonService.runRequest(settings, () -> managementBean.settings(settingRequest), Constants.REST_SETTINGS);
    }
}
