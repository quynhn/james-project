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

package org.apache.james.webadmin.integration;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.apache.james.webadmin.Constants.SEPARATOR;

import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.james.CassandraJmapTestRule;
import org.apache.james.DockerCassandraRule;
import org.apache.james.GuiceJamesServer;
import org.apache.james.jwt.JwtConfiguration;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.WebAdminGuiceProbe;
import org.apache.james.webadmin.authorization.AuthorizationFilter;
import org.apache.james.webadmin.authorization.JwtAuthorizationFilter;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Header;

public class JwtAuthorizationFilterIntegrationTest {
    private static final String DOMAIN = "localhost";
    private static final String GROUP1 = "go@" + DOMAIN;
    private static final String USER_A = "user@" + DOMAIN;

    private static final String ADDRESS_GROUPS = "/address/groups";

    private static final String VALID_TOKEN_WITH_IS_ADMIN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhZG1pbiI6dHJ1ZSwiaW" +
        "F0IjoxNTA5MDE0MDA3fQ.BJC0Y3VjUUlqHV9IwB1jusrPqndOQQd-7XKFAbTq45joamGjckFebMOM4_uFL5fchG0Fw2LduynuhL4uY8Yf1EtK8S" +
        "6_7SSyxW2oQ2WtLPH__bPmvothmfq1zxNBFvx6zKbl_N0ZDSrNO3SfKMcEp_u-Yze-_7YR9ya0r9m0-3gPLTz3fno3YQpOfanPLlnzuDhEH1_Wc" +
        "v6KUyAwJ5e_YWnNPlMJBx7rcgdEKU195cROYqHak31IW_ogbwVIyPR32jMVi2eeOXXs_71kv3jwSPhmlRxiCIwifwBHCKJfmiX9GKx3tlQV4pou" +
        "FaJSDkvhshpcCOxsu94ab0Xt5HzV6w";

    private static final String VALID_TOKEN_FOR_LIST = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhZG1pbiI6ZmFsc2UsInJlc29" +
        "1cmNlVHlwZSI6Imdyb3VwIiwicmVzb3VyY2VOYW1lIjoiIiwiYWN0aW9uIjoibGlzdEdyb3VwcyIsImlhdCI6MTUwOTA3NzA0MH0.m2Q5ICIqC_" +
        "wrGpvHOD7-Fqy9jyDgZSDfpTBEybsiPeecFr3i1X4HiIMXxYrUi5VOUNDwalUq-6_tVEuCjttcKUuxf3lsCiRCrroi4r94AQb1peJInP4gWVbwW" +
        "ucaUeK_CDw_SucA02XYg4JsJPToY9rgrHBMKWv__6rEcUM7o2qgT5RuS2AEmnfDHiYZBRYbRisHMvVVSvl6ye8s-1y7kA8eM7PcsbyrLq6gXaIP" +
        "Da8ePi8Ayx2yeKboxotgTcj_4QHyOC7zfkpCeHZH5g251p6Re3fqeCZU9c18Cy87HQmpD2Oj4Px5CRTDMe25iX1eCkC1kCsQk_zrRnkkItN9Mw";

    private static final String VALID_TOKEN_FOR_ADDING_MEMBER = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGxvY2FsaG9zdCIsIm5hbWUiOiJKb2huIERvZSIsImFkbWluIjp0cnVlLCJyZXNvdXJjZVR5cGUiOiJncm91cCIsInJlc291cmNlTmFtZSI6ImdvQGxvY2FsaG9zdCIsImFjdGlvbiI6ImFkZE1lbWJlciJ9.qeDBCbOiswu0ltzlObWILX107zjo07qeyV-pyFImrBeNJTajzgAVSw8TF4s0pPJRXLrT2-6IX6oayamNB61ZnaaGy3I8BW1ESLrirLxlvuOU1KPviE3iRVpWZ4QZrVbkqKLmRVR_gkTRB8jFAG5mrfaT8Hgj582jheUKnFBmPIU";

    private static final String VALID_TOKEN_FOR_REMOVING_MEMBER = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhZG1pbiI6ZmFs" +
        "c2UsInJlc291cmNlVHlwZSI6Imdyb3VwIiwicmVzb3VyY2VOYW1lIjoiZ29AbG9jYWxob3N0IiwiYWN0aW9uIjoicmVtb3ZlTWVtYmVyIiwiaWF" +
        "0IjoxNTA5MDc3MTUxfQ.fQgCjjHc3YzWcGN6raZG3np1k896QCKjk5KkBptSO_LjctQMa2rNIr4f9HNCmggRRfSErWwnLdhxfRzM8Uvcr9-gYT8" +
        "H3SZB_OYl1KrR4IeZoF6LgOtOSH7zcFs9EM5Zn_sZV8BRVj0k4vAQYscdY8txcY4ToucpMssiWFE70BLlPE9VaV_6Ll1WeeHo1yqAee3w_GXIf3" +
        "flftyrQhs-Wihb6qN-f6VAGbqrBwjYq8XC4hTDk-V3P5cacAk-XJTwp9OQzMPFFOVH3GDNM-cMoO8OCVnJpJ030VIjcJFyjfpYs3Ru-GxOHvoBh" +
        "3DrLENxUgNYQ9YfI23NJteM0q8erg";
    private static final String VALID_TOKEN_FOR_VIEW_MEMBERS = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhZG1pbiI6ZmFsc2U" +
        "sInJlc291cmNlVHlwZSI6Imdyb3VwIiwicmVzb3VyY2VOYW1lIjoiZ29AbG9jYWxob3N0IiwiYWN0aW9uIjoidmlld01lbWJlcnMiLCJpYXQiOj" +
        "E1MDkwNzcxODF9.tBXO74y-xUr3SI9bXeYpJNN1Hdr4KUC8w2SoN9eQiyb91gR7h1vrIiu5hUt9B-gB3sl4xlc-Ss9MD9S4q0LD7TwplqGB8Bk4" +
        "3LEBsThrXrtQNCRHV5Mh7jt6F5ev1iOLt-8R-cxtk0vFXk8a-DeZeadCql6itSxXJnTqOLEfbMk24wW7PR3TjKF1hiuI_ht-Q3qdkti3WzQOrny" +
        "SDQYCFnm0CH6W3CmmqWzBGsoU75bNcTMOE5FHNgZS78tPY4-VBEoyYWIgqGkFWjTubB2qr-3V0gxdzMr5JR-2Q63-eX8vz9FP286M-la5PWDAws" +
        "5GvAHecLuIJimm25V-ut3CWw";

    @ClassRule
    public static DockerCassandraRule cassandra = new DockerCassandraRule();
    
    @Rule
    public CassandraJmapTestRule cassandraJmapTestRule = CassandraJmapTestRule.defaultTestRule();

    private GuiceJamesServer guiceJamesServer;
    private DataProbeImpl dataProbe;
    private WebAdminGuiceProbe webAdminGuiceProbe;

    @Before
    public void setUp() throws Exception {
        JwtConfiguration jwtConfiguration = new JwtConfiguration(
            Optional.of(
                IOUtils.toString(ClassLoader.getSystemResourceAsStream("jwt_publickey"), Charsets.UTF_8)));

        guiceJamesServer = cassandraJmapTestRule.jmapServer(cassandra.getModule())
            .overrideWith(new WebAdminConfigurationModule(),
                binder -> binder.bind(AuthorizationFilter.class).to(JwtAuthorizationFilter.class),
                binder -> binder.bind(JwtConfiguration.class).toInstance(jwtConfiguration));
        guiceJamesServer.start();
        dataProbe = guiceJamesServer.getProbe(DataProbeImpl.class);
        webAdminGuiceProbe = guiceJamesServer.getProbe(WebAdminGuiceProbe.class);

        RestAssured.requestSpecification = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8)))
            .build();
    }

    @After
    public void tearDown() {
        guiceJamesServer.stop();
    }

    @Test
    public void listGroupsShouldReturnSuccessWhenIsAdmin() {
        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_WITH_IS_ADMIN))
        .when()
            .get(ADDRESS_GROUPS)
        .then()
            .statusCode(200);
    }

    @Test
    public void listGroupsShouldReturnSuccessWhenHasAccess() {
        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_FOR_LIST))
        .when()
            .get(ADDRESS_GROUPS)
        .then()
            .statusCode(200);
    }

    @Test
    public void listGroupsShouldReturnForbiddenWhenHasNoAccess() {
        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_FOR_ADDING_MEMBER))
        .when()
            .get(ADDRESS_GROUPS)
        .then()
            .statusCode(403);
    }

    @Test
    public void listMembersShouldReturnSuccessWhenIsAdmin() {
        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_WITH_IS_ADMIN))
        .when()
            .put(ADDRESS_GROUPS + SEPARATOR + GROUP1 + SEPARATOR + USER_A);

        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_WITH_IS_ADMIN))
        .when()
            .get(ADDRESS_GROUPS + SEPARATOR + GROUP1)
            .then()
            .statusCode(200);
    }

    @Test
    public void listMembersShouldReturnSuccessWhenHasAccess() {
        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_WITH_IS_ADMIN))
        .when()
            .put(ADDRESS_GROUPS + SEPARATOR + GROUP1 + SEPARATOR + USER_A);

        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_FOR_VIEW_MEMBERS))
        .when()
            .get(ADDRESS_GROUPS + SEPARATOR + GROUP1)
        .then()
            .statusCode(200);
    }

    @Test
    public void listMembersShouldReturnForbiddenWhenHasNoAccess() {
        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_WITH_IS_ADMIN))
        .when()
            .put(ADDRESS_GROUPS + SEPARATOR + GROUP1 + SEPARATOR + USER_A);

        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_FOR_ADDING_MEMBER))
        .when()
            .get(ADDRESS_GROUPS + SEPARATOR + GROUP1)
            .then()
            .statusCode(403);
    }

    @Test
    public void addMemberShouldSuccessWhenIsAdmin() {
        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_WITH_IS_ADMIN))
        .when()
            .put(ADDRESS_GROUPS + SEPARATOR + GROUP1 + SEPARATOR + USER_A)
        .then()
            .statusCode(201);
    }

    @Test
    public void addMemberShouldSuccessWhenHaveAccess() {
        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_FOR_ADDING_MEMBER))
        .when()
            .put(ADDRESS_GROUPS + SEPARATOR + GROUP1 + SEPARATOR + USER_A)
        .then()
            .statusCode(201);
    }

    @Test
    public void addShouldForbiddenWhenInvalidResourceInTokenHeader() {
        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_FOR_REMOVING_MEMBER))
        .when()
            .put(ADDRESS_GROUPS + SEPARATOR + GROUP1 + SEPARATOR + USER_A)
        .then()
            .statusCode(403);
    }

    @Test
    public void removeMemberShouldSuccessWhenHaveAccess() {
        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_WITH_IS_ADMIN))
        .when()
            .put(ADDRESS_GROUPS + SEPARATOR + GROUP1 + SEPARATOR + USER_A);

        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_FOR_REMOVING_MEMBER))
        .when()
            .delete(ADDRESS_GROUPS + SEPARATOR + GROUP1 + SEPARATOR + USER_A)
        .then()
            .statusCode(200);
    }

    @Test
    public void removeMemberShouldForbiddenWhenInvalidResourceInTokenHeader() {
        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_WITH_IS_ADMIN))
        .when()
            .put(ADDRESS_GROUPS + SEPARATOR + GROUP1 + SEPARATOR + USER_A);

        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_FOR_ADDING_MEMBER))
        .when()
            .delete(ADDRESS_GROUPS + SEPARATOR + GROUP1 + SEPARATOR + USER_A)
        .then()
            .statusCode(403);
    }

    @Test
    public void removeMemberShouldSuccessWhenIsAdmin() {
        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_WITH_IS_ADMIN))
        .when()
            .put(ADDRESS_GROUPS + SEPARATOR + GROUP1 + SEPARATOR + USER_A);

        given()
            .port(webAdminGuiceProbe.getWebAdminPort())
            .header(createHeader(VALID_TOKEN_WITH_IS_ADMIN))
        .when()
            .delete(ADDRESS_GROUPS + SEPARATOR + GROUP1 + SEPARATOR + USER_A)
        .then()
            .statusCode(200);
    }

    @NotNull
    private Header createHeader(String value) {
        return new Header("Authorization", "Bearer " + value);
    }
}
