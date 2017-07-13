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

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.Api;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.models.Swagger;
import org.reflections.Reflections;

public class SwaggerParser {
    public static String getSwaggerJson(String packageName) throws JsonProcessingException {
        Swagger swagger = getSwagger(packageName);
        String json = swaggerToJson(swagger);
        return json;
    }

    public static Swagger getSwagger(String packageName) {
        Reflections reflections = new Reflections(packageName);
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setResourcePackage(packageName);
        beanConfig.setScan(true);
        beanConfig.scanAndRead();
        Swagger swagger = beanConfig.getSwagger();

        Reader reader = new Reader(swagger);

        Set<Class<?>> apiClasses = reflections.getTypesAnnotatedWith(Api.class);
        return reader.read(apiClasses);
    }

    public static String swaggerToJson(Swagger swagger) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        String json = objectMapper.writeValueAsString(swagger);
        return json;
    }
}
