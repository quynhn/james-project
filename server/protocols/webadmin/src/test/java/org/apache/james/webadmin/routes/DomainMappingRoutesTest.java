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

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.apache.james.webadmin.WebAdminServer.NO_CONFIGURATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.net.InetAddress;

import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.lib.MappingImpl;
import org.apache.james.rrt.lib.MappingsImpl;
import org.apache.james.rrt.memory.MemoryRecipientRewriteTable;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.service.DomainMappingService;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;
import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
public class DomainMappingRoutesTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainMappingRoutesTest.class);
    private WebAdminServer webAdminServer;

    private void createServer(RecipientRewriteTable recipientRewriteTable) throws Exception {
        webAdminServer = new WebAdminServer(new DomainMappingRoutes(recipientRewriteTable, new JsonTransformer(), new DomainMappingService(recipientRewriteTable)));
        webAdminServer.configure(NO_CONFIGURATION);
        webAdminServer.await();

        RestAssured.requestSpecification = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8)))
            .setPort(webAdminServer.getPort().toInt())
            .setBasePath(DomainMappingRoutes.BASE_PATH)
            .build();

    }

    @After
    public void stop() {
        webAdminServer.destroy();
    }

    public class NormalBehaviour {

        private MemoryRecipientRewriteTable recipientRewriteTable;

        @Before
        public void setUp() throws Exception {
            DNSService dnsService = mock(DNSService.class);
            Mockito.when(dnsService.getHostName(any())).thenReturn("localhost");
            Mockito.when(dnsService.getLocalHost()).thenReturn(InetAddress.getByName("localhost"));

            recipientRewriteTable = new MemoryRecipientRewriteTable();

            recipientRewriteTable.setLog(LOGGER);

            createServer(recipientRewriteTable);
        }

        @Test
        public void getMappingsShouldRespondEmptyMapByDefault() {
            String expectedBody = "{}";

            given()
                .get()
            .then()
                .statusCode(200)
                .body(is(expectedBody));
        }

        @Test
        public void getMappingsShouldRespondDomainMappings() throws Exception {
            String body = "{\"real1.com\":[\"alias1.com\"],\"real2.com\":[\"alias2.com\"]}";

            recipientRewriteTable.addAliasDomainMapping("alias1.com", "real1.com");
            recipientRewriteTable.addAliasDomainMapping("alias2.com", "real2.com");

            given()
                .get()
            .then()
                .statusCode(200)
                .body(is(body));
        }

        @Test
        public void getMappingsShouldRespondDomainMappingsByRealDomain() throws Exception {
            String body = "{\"real.com\":[\"alias1.com\",\"alias2.com\"]}";

            recipientRewriteTable.addAliasDomainMapping("alias1.com", "real.com");
            recipientRewriteTable.addAliasDomainMapping("alias2.com", "real.com");

            given()
                .get()
            .then()
                .statusCode(200)
                .body(is(body));
        }

        @Test
        public void putMappingShouldRespondNoContentOnSuccess() throws Exception {
            String body = "{ \"realDomain\": \"email.com\", \"aliases\": [\"linagora.com\", \"linagora.org\"] }";

            given()
                .body(body)
            .when()
                .put()
            .then()
                .statusCode(204);

            assertThat(recipientRewriteTable.getAllMappings())
                .containsEntry(
                    "*@linagora.com",
                    MappingsImpl.builder()
                        .add(MappingImpl.domain("email.com"))
                        .build());
        }

        @Test
        public void deleteAliasMappingShouldRemoveMappingDomains() throws Exception {
            recipientRewriteTable.addAliasDomainMapping("alias1.com", "real.com");
            recipientRewriteTable.addAliasDomainMapping("alias2.org", "real.com");

            String body = "{ \"realDomain\": \"real.com\", \"aliases\": [\"alias1.com\", \"alias2.org\"] }";

            given()
                .body(body)
            .when()
                .delete()
                .then()
                .statusCode(204);

            assertThat(recipientRewriteTable.getAllMappings()).isNull();
        }
    }

}