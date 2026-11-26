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

import net.haugr.eds.api.common.SocketFrameType;
import net.haugr.eds.api.dtos.SocketFrame;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import net.haugr.eds.api.common.Constants;
import java.util.HashSet;
import java.util.Set;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * <p>The EDS REST Application, i.e., the listing of all EDS-based REST Service
 * classes, which should be exposed.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@ApplicationPath(Constants.REST_API)
// The frames of the WebSocket (FEATURES.md) aren't part of an
// operation, so their schemas are added here; the info itself comes from the
// configuration of the OpenAPI plugin, which takes precedence.
@OpenAPIDefinition(info = @Info(title = "EDS", version = "2.0"),
        components = @Components(schemas = {
                @Schema(name = "SocketFrame", implementation = SocketFrame.class),
                @Schema(name = "SocketFrameType", implementation = SocketFrameType.class)
        }))
@SecurityScheme(securitySchemeName = CommonService.SESSION_SCHEME, type = SecuritySchemeType.HTTP, scheme = "bearer",
        description = "The Session token from login (`session` in its response), Base64-encoded, as `Authorization: Bearer <token>`. "
                + "Used by the content endpoints and the ID-based resource API; the other operations take the credentials in the JSON body.")
public class EDSApplication extends Application {

    /**
     * Default Constructor.
     */
    public EDSApplication() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> set = new HashSet<>();

        set.add(AuthenticatedService.class);
        set.add(CircleService.class);
        set.add(DataService.class);
        set.add(DataTypeService.class);
        set.add(MasterKeyService.class);
        set.add(MemberService.class);
        set.add(SanityService.class);
        set.add(SettingService.class);
        set.add(SpecificationService.class);
        set.add(TrusteeService.class);
        set.add(FeatureService.class);
        set.add(StatusService.class);
        // Providers: the requests are counted for the status, and pages of
        // allowed origins may call the stone.
        set.add(StatusFilter.class);
        set.add(OriginFilter.class);
        set.add(BodyCounter.class);
        set.add(CircleFeatureService.class);
        set.add(VersionService.class);

        return set;
    }
}
