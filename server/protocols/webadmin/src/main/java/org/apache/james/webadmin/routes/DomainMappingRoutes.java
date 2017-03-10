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

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.dto.DomainMappingRequest;
import org.apache.james.webadmin.service.DomainMappingService;
import org.apache.james.webadmin.utils.JsonExtractException;
import org.apache.james.webadmin.utils.JsonExtractor;
import org.apache.james.webadmin.utils.JsonTransformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Service;


public class DomainMappingRoutes implements Routes {

    public static final String BASE_PATH = "/domain_mappings";

	private final RecipientRewriteTable recipientRewriteTable;
	private final JsonTransformer jsonTransformer;
    private final JsonExtractor<DomainMappingRequest> jsonExtractor;

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainMappingRoutes.class);
    private final DomainMappingService domainMappingService;

    @Inject
	public DomainMappingRoutes(RecipientRewriteTable recipientRewriteTable, JsonTransformer jsonTransformer, DomainMappingService domainMappingService) {
		this.recipientRewriteTable = recipientRewriteTable;
		this.jsonTransformer = jsonTransformer;
		this.domainMappingService = domainMappingService;
        this.jsonExtractor = new JsonExtractor<>(DomainMappingRequest.class);
	}

	public void define(Service service) {
		service.get(BASE_PATH,
				(request, response) -> domainMappingService.getMappings(),
				jsonTransformer);

		service.put(BASE_PATH, this::addMapping);

        service.delete(BASE_PATH, this::removeMapping);
	}

    private String addMapping(Request request, Response response) {
        try {
            DomainMappingRequest domainMappingRequest = jsonExtractor.parse(request.body());

            domainMappingRequest.getAliases()
                .stream()
                .forEach(alias -> {
                    try {
                        recipientRewriteTable.addAliasDomainMapping(alias, domainMappingRequest.getRealDomain());
                    } catch (RecipientRewriteTableException e) {
                        LOGGER.warn("Can not update RecipientRewriteTable", e);
                    }
                });

            response.status(204);
        } catch (JsonExtractException e) {
            e.printStackTrace();
            LOGGER.info("Invalid request data", e.getMessage());
            response.status(400);
        }

        return Constants.EMPTY_BODY;
    }

    private String removeMapping(Request request, Response response) {
        try {
            DomainMappingRequest domainMappingRequest = jsonExtractor.parse(request.body());

            domainMappingRequest.getAliases()
                .stream()
                .forEach(alias -> {
                    try {
                        recipientRewriteTable.removeAliasDomainMapping(alias, domainMappingRequest.getRealDomain());
                    } catch (RecipientRewriteTableException e) {
                        LOGGER.warn("Can not remove domain mapping", e);
                    }
                });

            response.status(204);
        } catch (JsonExtractException e) {
            e.printStackTrace();
            LOGGER.info("Invalid request data", e.getMessage());
            response.status(400);
        }

        return Constants.EMPTY_BODY;
    }
}
