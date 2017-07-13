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

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;
import java.lang.reflect.Method;
import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import io.swagger.annotations.Api;
import org.reflections.Reflections;
import spark.Route;

public class RouteBuilder {
    public static void setupRoutes(String packageName) throws InstantiationException, IllegalAccessException {

        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> apiRoutes = reflections.getTypesAnnotatedWith(Api.class);

        for (Class<?> clazz : apiRoutes) {
            Route sparkRoute = (Route) clazz.newInstance();
            Path path = clazz.getAnnotation(Path.class);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                POST post = method.getAnnotation(POST.class);
                String friendlyRoute = path.value().replaceAll("\\{(.*?)\\}", ":$1");
                if (post != null) {
                    post(friendlyRoute, sparkRoute);
                    break;
                }

                GET get = method.getAnnotation(GET.class);
                if (get != null) {
                    get(friendlyRoute, sparkRoute);
                    break;
                }

                DELETE delete = method.getAnnotation(DELETE.class);
                if (delete != null) {
                    delete(friendlyRoute, sparkRoute);
                    break;
                }

                PUT put = method.getAnnotation(PUT.class);
                if (put != null) {
                    put(friendlyRoute, sparkRoute);
                    break;
                }
            }

        }
    }
}
