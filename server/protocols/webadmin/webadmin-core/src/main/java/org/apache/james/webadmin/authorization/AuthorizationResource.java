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

import java.util.Objects;

public class AuthorizationResource {
    private final String resourceType;
    private final String resourceName;
    private final String action;

    public AuthorizationResource(String resourceType, String resourceName, String action) {
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        this.action = action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getAction() {
        return action;
    }

    public boolean check(AuthorizationResource resource) {
        return this.equals(resource);
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof AuthorizationResource) {
            AuthorizationResource that = (AuthorizationResource) o;

            return Objects.equals(this.resourceType, that.resourceType)
                && Objects.equals(this.resourceName, that.resourceName)
                && Objects.equals(this.action, that.action);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(resourceType, resourceName, action);
    }
}
