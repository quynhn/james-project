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

package org.apache.james.webadmin.swagger;

import static spark.Spark.before;
import static spark.Spark.get;

import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

@SwaggerDefinition(host = "localhost:4567", //
        info = @Info(description = "JAMES API", //
                version = "V1.0", //
                title = "Some JAMES apis for OpenPaaS") , //
        schemes = { SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS }, //
        consumes = { "application/json" }, //
        produces = { "application/json" }, //
        tags = { @Tag(name = "swagger") })
public class GenerateSwaggerAPIs {
    public static final String APP_PACKAGE = "org.apache.james.webadmin.routes";

    public static void main(String[] args) {

        try {
            // Quite unsafe!
            before(new CorsFilter());
            new OptionsController();

            // Scan classes with @Api annotation and add as routes
            RouteBuilder.setupRoutes(APP_PACKAGE);

            // Build swagger json description
            final String swaggerJson = SwaggerParser.getSwaggerJson(APP_PACKAGE);
            get("/swagger", (req, res) -> {
                return swaggerJson;
            });

        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }
}
