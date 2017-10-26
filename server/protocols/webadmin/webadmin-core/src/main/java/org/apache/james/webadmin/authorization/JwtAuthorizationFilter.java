/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.webadmin.authorization;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.jwt.JwtTokenVerifier;

import io.jsonwebtoken.Claims;
import spark.Request;
import spark.Response;

public class JwtAuthorizationFilter implements AuthorizationFilter {
    private static final String AUTHORIZATION_HEADER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";

    private final JwtTokenVerifier jwtTokenVerifier;

    @Inject
    public JwtAuthorizationFilter(JwtTokenVerifier jwtTokenVerifier) {
        this.jwtTokenVerifier = jwtTokenVerifier;
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
        if (request.requestMethod() != OPTIONS) {
            Optional<String> bearer = Optional.ofNullable(request.headers(AUTHORIZATION_HEADER_NAME))
                .filter(value -> value.startsWith(AUTHORIZATION_HEADER_PREFIX))
                .map(value -> value.substring(AUTHORIZATION_HEADER_PREFIX.length()));

            request.attribute(AUTHORIZATION_DATA, AuthorizationLevel.builder()
                .admin(isAdmin(bearer))
                .noCheckRequired(noneBearer(bearer))
                .validJwtToken(validJWT(bearer))
                .resource(isResource(bearer))
                .build());
        }
    }

    private Optional<AuthorizationResource> isResource(Optional<String> bearer) {
        return bearer.map(this::tokenToResource)
            .orElse(Optional.empty());
    }

    private Optional<AuthorizationResource> tokenToResource(String bearer) {
        Claims claims = jwtTokenVerifier.extractClaims(bearer);
        return Optional.of(new AuthorizationResource(
            claims.get("resourceType", String.class),
            claims.get("resourceName", String.class),
            claims.get("action", String.class)
        ));
    }

    private boolean isAdmin(Optional<String> bearer) {
        return jwtTokenVerifier.hasAttribute("admin", true, bearer.get());
    }

    private boolean validJWT(Optional<String> bearer) {
        return jwtTokenVerifier.verify(bearer.get());
    }

    private boolean noneBearer(Optional<String> bearer) {
        return !bearer.isPresent();
    }
}
