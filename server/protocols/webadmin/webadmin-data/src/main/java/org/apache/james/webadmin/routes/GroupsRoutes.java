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

package org.apache.james.webadmin.routes;

import static org.apache.james.webadmin.Constants.SEPARATOR;
import static spark.Spark.halt;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.james.core.MailAddress;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.lib.Mapping;
import org.apache.james.rrt.lib.Mappings;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.util.streams.Iterators;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.authorization.AuthorizationFilter;
import org.apache.james.webadmin.authorization.AuthorizationLevel;
import org.apache.james.webadmin.authorization.AuthorizationLevel.Action;
import org.apache.james.webadmin.authorization.AuthorizationLevel.ResourceType;
import org.apache.james.webadmin.authorization.AuthorizationResource;
import org.apache.james.webadmin.authorization.JwtAuthorizationFilter;
import org.apache.james.webadmin.utils.JsonExtractException;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.eclipse.jetty.http.HttpStatus;

import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSortedSet;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import spark.HaltException;
import spark.Request;
import spark.Response;
import spark.Service;

@Api(tags = "Address Groups")
@Path(GroupsRoutes.ROOT_PATH)
@Produces(Constants.JSON_CONTENT_TYPE)
public class GroupsRoutes implements Routes {

    public static final String ROOT_PATH = "address/groups";
    private static final String GROUP_ADDRESS = "groupAddress";
    private static final String GROUP_ADDRESS_PATH = ROOT_PATH + SEPARATOR + ":" + GROUP_ADDRESS;
    private static final String USER_ADDRESS = "userAddress";
    private static final String USER_IN_GROUP_ADDRESS_PATH = GROUP_ADDRESS_PATH + SEPARATOR + ":" + USER_ADDRESS;

    private final UsersRepository usersRepository;
    private final DomainList domainList;
    private final JsonTransformer jsonTransformer;
    private final RecipientRewriteTable recipientRewriteTable;

    @Inject
    @VisibleForTesting
    GroupsRoutes(RecipientRewriteTable recipientRewriteTable, UsersRepository usersRepository,
                 DomainList domainList, JsonTransformer jsonTransformer) {
        this.usersRepository = usersRepository;
        this.domainList = domainList;
        this.jsonTransformer = jsonTransformer;
        this.recipientRewriteTable = recipientRewriteTable;
    }

    @Override
    public void define(Service service) {
        service.get(ROOT_PATH, this::listGroups, jsonTransformer);
        service.get(GROUP_ADDRESS_PATH, this::listGroupMembers, jsonTransformer);
        service.put(GROUP_ADDRESS_PATH, (request, response) -> halt(HttpStatus.BAD_REQUEST_400));
        service.put(USER_IN_GROUP_ADDRESS_PATH, this::addToGroup);
        service.delete(GROUP_ADDRESS_PATH, (request, response) -> halt(HttpStatus.BAD_REQUEST_400));
        service.delete(USER_IN_GROUP_ADDRESS_PATH, this::removeFromGroup);
    }

    @GET
    @Path(ROOT_PATH)
    @ApiOperation(value = "getting groups list")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = List.class),
        @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    public Set<String> listGroups(Request request, Response response) throws RecipientRewriteTableException {
        boolean isAllow = Optional.<AuthorizationLevel>ofNullable(request.attribute(JwtAuthorizationFilter.AUTHORIZATION_DATA))
            .orElse(AuthorizationFilter.JWT_DISABLED)
            .allowOnResource(new AuthorizationResource(ResourceType.ADDRESS_GROUPS.toString(), "", Action.ADDRESS_GROUPS_LIST_GROUPS.getAction()),
                             new AuthorizationResource(ResourceType.ADDRESS_GROUPS.toString(), null, Action.ADDRESS_GROUPS_LIST_GROUPS.getAction()));
        if (isAllow) {
            return Optional.ofNullable(recipientRewriteTable.getAllMappings())
                .map(mappings ->
                    mappings.entrySet().stream()
                        .filter(e -> e.getValue().contains(Mapping.Type.Address))
                        .map(Map.Entry::getKey)
                        .collect(Guavate.toImmutableSortedSet()))
                .orElse(ImmutableSortedSet.of());
        }
        response.status(HttpStatus.FORBIDDEN_403);
        return ImmutableSortedSet.of();
    }

    @PUT
    @Path(ROOT_PATH + "/{" + GROUP_ADDRESS + "}/{" + USER_ADDRESS + "}")
    @ApiOperation(value = "adding a member into a group")
    @ApiImplicitParams({
        @ApiImplicitParam(required = true, dataType = "string", name = GROUP_ADDRESS, paramType = "path"),
        @ApiImplicitParam(required = true, dataType = "string", name = USER_ADDRESS, paramType = "path")
    })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = List.class),
        @ApiResponse(code = 400, message = GROUP_ADDRESS + " or group structure format is not valid"),
        @ApiResponse(code = 403, message = "server doesn't own the domain"),
        @ApiResponse(code = 409, message = "requested group address is already used for another purpose"),
        @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    public HaltException addToGroup(Request request, Response response) throws JsonExtractException, AddressException, RecipientRewriteTableException, UsersRepositoryException, DomainListException {
        String groupAddressString = request.params(GROUP_ADDRESS);
        boolean isAllow = Optional.<AuthorizationLevel>ofNullable(request.attribute(JwtAuthorizationFilter.AUTHORIZATION_DATA))
            .orElse(AuthorizationFilter.JWT_DISABLED)
            .allowOnResource(new AuthorizationResource(ResourceType.ADDRESS_GROUPS.toString(), groupAddressString, Action.ADDRESS_GROUPS_ADD_MEMBER.getAction()));
        if (isAllow) {
            MailAddress groupAddress = parseMailAddress(request.params(GROUP_ADDRESS));
            ensureRegisteredDomain(groupAddress.getDomain());
            ensureNotShadowingAnotherAddress(groupAddress);
            MailAddress userAddress = parseMailAddress(request.params(USER_ADDRESS));
            recipientRewriteTable.addAddressMapping(groupAddress.getLocalPart(), groupAddress.getDomain(), userAddress.asString());
            return halt(HttpStatus.CREATED_201);
        }
        return halt(HttpStatus.FORBIDDEN_403);
    }

    private void ensureRegisteredDomain(String domain) throws DomainListException {
        if (!domainList.containsDomain(domain)) {
            throw halt(HttpStatus.FORBIDDEN_403);
        }
    }

    private void ensureNotShadowingAnotherAddress(MailAddress groupAddress) throws UsersRepositoryException {
        if (usersRepository.contains(groupAddress.asString())) {
            throw halt(HttpStatus.CONFLICT_409);
        }
    }


    @DELETE
    @Path(ROOT_PATH + "/{" + GROUP_ADDRESS + "}/{" + USER_ADDRESS + "}")
    @ApiOperation(value = "remove a member from a group")
    @ApiImplicitParams({
        @ApiImplicitParam(required = true, dataType = "string", name = GROUP_ADDRESS, paramType = "path"),
        @ApiImplicitParam(required = true, dataType = "string", name = USER_ADDRESS, paramType = "path")
    })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = List.class),
        @ApiResponse(code = 400, message = GROUP_ADDRESS + " or group structure format is not valid"),
        @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    public HaltException removeFromGroup(Request request, Response response) throws JsonExtractException, AddressException, RecipientRewriteTableException {
        String groupAddressString = request.params(GROUP_ADDRESS);
        boolean isAllow = Optional.<AuthorizationLevel>ofNullable(request.attribute(JwtAuthorizationFilter.AUTHORIZATION_DATA))
            .orElse(AuthorizationFilter.JWT_DISABLED)
            .allowOnResource(new AuthorizationResource(ResourceType.ADDRESS_GROUPS.toString(), groupAddressString, Action.ADDRESS_GROUPS_REMOVE_MEMBER.getAction()));
        if (isAllow) {
            MailAddress groupAddress = parseMailAddress(groupAddressString);
            MailAddress userAddress = parseMailAddress(request.params(USER_ADDRESS));
            recipientRewriteTable.removeAddressMapping(groupAddress.getLocalPart(), groupAddress.getDomain(), userAddress.asString());
            return halt(HttpStatus.OK_200);
        }
        return halt(HttpStatus.FORBIDDEN_403);
    }

    @GET
    @Path(ROOT_PATH + "/{" + GROUP_ADDRESS + "}")
    @ApiOperation(value = "listing group members")
    @ApiImplicitParams({
        @ApiImplicitParam(required = true, dataType = "string", name = GROUP_ADDRESS, paramType = "path")
    })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = List.class),
        @ApiResponse(code = 400, message = "The group is not an address"),
        @ApiResponse(code = 404, message = "The group does not exist"),
        @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    public ImmutableSortedSet<String> listGroupMembers(Request request, Response response) throws RecipientRewriteTable.ErrorMappingException, RecipientRewriteTableException {
        String groupAddressString = request.params(GROUP_ADDRESS);
        boolean isAllow = Optional.<AuthorizationLevel>ofNullable(request.attribute(JwtAuthorizationFilter.AUTHORIZATION_DATA))
            .orElse(AuthorizationFilter.JWT_DISABLED)
            .allowOnResource(new AuthorizationResource(ResourceType.ADDRESS_GROUPS.toString(), groupAddressString, Action.ADDRESS_GROUPS_VIEW_MEMBERS.getAction()));
        if (isAllow) {
            MailAddress groupAddress = parseMailAddress(groupAddressString);
            Mappings mappings = recipientRewriteTable.getMappings(groupAddress.getLocalPart(), groupAddress.getDomain());

            ensureNonEmptyMappings(mappings);

            return Iterators
                .toStream(mappings.select(Mapping.Type.Address).iterator())
                .map(Mapping::getAddress)
                .collect(Guavate.toImmutableSortedSet());
        }
        response.status(HttpStatus.FORBIDDEN_403);
        return ImmutableSortedSet.of();
    }

    private MailAddress parseMailAddress(String address) {
        try {
            return new MailAddress(address);
        } catch (AddressException e) {
            throw halt(HttpStatus.BAD_REQUEST_400);
        }
    }

    private void ensureNonEmptyMappings(Mappings mappings) {
        if (mappings == null || mappings.isEmpty()) {
            throw halt(HttpStatus.NOT_FOUND_404);
        }
    }
}
