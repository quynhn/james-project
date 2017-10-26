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

import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationLevel {
    public enum ResourceType {
        ADDRESS_GROUPS("group");

        private final String resourceType;

        ResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getResourceType() {
            return resourceType;
        }
    }

    public enum Action {
        ADDRESS_GROUPS_LIST_GROUPS(ResourceType.ADDRESS_GROUPS, "listGroups"),
        ADDRESS_GROUPS_VIEW_MEMBERS(ResourceType.ADDRESS_GROUPS, "viewMembers"),
        ADDRESS_GROUPS_ADD_MEMBER(ResourceType.ADDRESS_GROUPS, "addMember"),
        ADDRESS_GROUPS_REMOVE_MEMBER(ResourceType.ADDRESS_GROUPS, "removeMember");

        private final ResourceType resourceType;
        private final String action;

        Action(ResourceType resourceType, String action) {
            this.resourceType = resourceType;
            this.action = action;
        }

        public String getAction() {
            return action;
        }
    }

    private final Logger LOG = LoggerFactory.getLogger(AuthorizationLevel.class);

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Optional<Boolean> admin;
        private Optional<Boolean> noCheckRequired;
        private Optional<Boolean> validJwtToken;
        private Optional<AuthorizationResource> resource;

        public Builder() {
            admin = Optional.empty();
            noCheckRequired = Optional.empty();
            validJwtToken = Optional.empty();
            resource = Optional.empty();
        }

        public Builder admin(Boolean admin) {
            this.admin = Optional.of(admin);
            return this;
        }

        public Builder noCheckRequired(Boolean noCheckRequired) {
            this.noCheckRequired = Optional.of(noCheckRequired);
            return this;
        }

        public Builder validJwtToken(Boolean validJwtToken) {
            this.validJwtToken = Optional.of(validJwtToken);
            return this;
        }

        public Builder resource(Optional<AuthorizationResource> resource) {
            this.resource = resource;
            return this;
        }

        public AuthorizationLevel build() {
            boolean DEFAULT_VALUE = false;
            return new AuthorizationLevel(admin.orElse(DEFAULT_VALUE),
                noCheckRequired.orElse(DEFAULT_VALUE),
                validJwtToken.orElse(DEFAULT_VALUE),
                resource);
        }
    }


    private final boolean admin;
    private final boolean noCheckRequired;
    private final boolean validJwtToken;
    private final Optional<AuthorizationResource> resource;

    public AuthorizationLevel(boolean admin, boolean noCheckRequired, boolean validJwtToken, Optional<AuthorizationResource> resource) {
        this.admin = admin;
        this.noCheckRequired = noCheckRequired;
        this.validJwtToken = validJwtToken;
        this.resource = resource;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isNoCheckRequired() {
        return noCheckRequired;
    }

    public boolean isValidJwtToken() {
        return validJwtToken;
    }

    public Optional<AuthorizationResource> getResource() {
        return resource;
    }

    public boolean allowOnResource(AuthorizationResource... resources) {
        if (isNoCheckRequired()) {
            LOG.info("No check required");
            return true;
        }
        if (!isValidJwtToken()) {
            LOG.info("Not valid token");
            return false;
        }
        if (isAdmin()) {
            return true;
        }

        return this.resource
            .map(res -> Arrays.asList(resources).contains(res))
            .orElse(false);

    }
}
