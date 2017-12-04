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

import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.dto.CassandraVersionRequest;
import org.apache.james.webadmin.service.CassandraMigrationService;
import org.apache.james.webadmin.service.MigrationException;
import org.apache.james.webadmin.utils.ErrorResponder;
import org.apache.james.webadmin.utils.ErrorResponder.ErrorType;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Service;

public class CassandraMigrationRoutes implements Routes {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraMigrationRoutes.class);

    public static final String VERSION_BASE = "/cassandra/version";
    private static final String VERSION_BASE_LATEST = VERSION_BASE + "/latest";
    private static final String VERSION_UPGRADE_BASE = VERSION_BASE + "/upgrade";
    private static final String VERSION_UPGRADE_TO_LATEST_BASE = VERSION_UPGRADE_BASE + "/latest";

    private final CassandraMigrationService cassandraMigrationService;
    private final JsonTransformer jsonTransformer;

    public static String INVALID_VERSION_UPGRADE_REQUEST = "Invalid request for version upgrade";
    public static String MIGRATION_REQUEST_CAN_NOT_BE_DONE = "The migration requested can not be performed";
    public static String PARTIAL_MIGRATION_PROCESS = "An error lead to partial migration process";

    @Inject
    public CassandraMigrationRoutes(CassandraMigrationService cassandraMigrationService, JsonTransformer jsonTransformer) {
        this.cassandraMigrationService = cassandraMigrationService;
        this.jsonTransformer = jsonTransformer;
    }

    @Override
    public void define(Service service) {
        service.get(VERSION_BASE,
            (request, response) -> cassandraMigrationService.getCurrentVersion(),
            jsonTransformer);

        service.get(VERSION_BASE_LATEST,
            (request, response) -> cassandraMigrationService.getLatestVersion(),
            jsonTransformer);

        service.post(VERSION_UPGRADE_BASE, (request, response) -> {
            LOGGER.debug("Cassandra upgrade launched");
            try {
                CassandraVersionRequest cassandraVersionRequest = CassandraVersionRequest.parse(request.body());
                cassandraMigrationService.upgradeToVersion(cassandraVersionRequest.getValue());
                response.status(HttpStatus.NO_CONTENT_204);
            } catch (NullPointerException | IllegalArgumentException e) {
                LOGGER.info(INVALID_VERSION_UPGRADE_REQUEST);
                throw ErrorResponder.builder()
                    .statusCode(HttpStatus.BAD_REQUEST_400)
                    .type(ErrorType.INVALID_ARGUMENT)
                    .message(INVALID_VERSION_UPGRADE_REQUEST)
                    .cause(e)
                    .haltError();
            } catch (IllegalStateException e) {
                LOGGER.info(MIGRATION_REQUEST_CAN_NOT_BE_DONE, e);
                throw ErrorResponder.builder()
                    .statusCode(HttpStatus.CONFLICT_409)
                    .type(ErrorType.WRONG_STATE)
                    .message(MIGRATION_REQUEST_CAN_NOT_BE_DONE)
                    .cause(e)
                    .haltError();
            } catch (MigrationException e) {
                LOGGER.error(PARTIAL_MIGRATION_PROCESS, e);
                throw ErrorResponder.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500)
                    .type(ErrorType.SERVER_ERROR)
                    .message(PARTIAL_MIGRATION_PROCESS)
                    .cause(e)
                    .haltError();
            }
            return Constants.EMPTY_BODY;
        });

        service.post(VERSION_UPGRADE_TO_LATEST_BASE, (request, response) -> {
            try {
                cassandraMigrationService.upgradeToLastVersion();
            } catch (IllegalStateException e) {
                LOGGER.info(MIGRATION_REQUEST_CAN_NOT_BE_DONE, e);
                throw ErrorResponder.builder()
                    .statusCode(HttpStatus.CONFLICT_409)
                    .type(ErrorType.WRONG_STATE)
                    .message(MIGRATION_REQUEST_CAN_NOT_BE_DONE)
                    .cause(e)
                    .haltError();
            } catch (MigrationException e) {
                LOGGER.error(PARTIAL_MIGRATION_PROCESS, e);
                throw ErrorResponder.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500)
                    .type(ErrorType.SERVER_ERROR)
                    .message(PARTIAL_MIGRATION_PROCESS)
                    .cause(e)
                    .haltError();
            }

            return Constants.EMPTY_BODY;
        });
    }
}
