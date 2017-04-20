/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.utils;

import com.google.common.base.Preconditions;
import com.nurkiewicz.asyncretry.AsyncRetryExecutor;
import com.nurkiewicz.asyncretry.function.RetryCallable;

import java.util.concurrent.CompletableFuture;

public class RetryExecutorBuilder<V> {

    private static final int INITIAL_DELAY_MILLIS = 500;
    private static final int MULTIPLIER = 2;
    private static final int DEFAULT_MAX_RETRIES = 7;
    private static final int DEFAULT_MIN_DELAY = 3000;

    private int maxRetries;
    private int minDelay;
    private AsyncRetryExecutor executor;
    private Class<? extends Throwable> clazzException;
    private RetryCallable<V> task;

    public RetryExecutorBuilder() {
        maxRetries = DEFAULT_MAX_RETRIES;
        minDelay = DEFAULT_MIN_DELAY;
    }

    public RetryExecutorBuilder executor(AsyncRetryExecutor executor) {
        this.executor = executor;
        return this;
    }

    public RetryExecutorBuilder maxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public RetryExecutorBuilder minDelay(int minDelay) {
        this.minDelay = minDelay;
        return this;
    }

    public RetryExecutorBuilder clazzException(Class<? extends Throwable> clazzException) {
        this.clazzException = clazzException;
        return this;
    }

    public RetryExecutorBuilder task(RetryCallable<V> task) {
        this.task = task;
        return this;
    }

    public CompletableFuture<V> retryOnTask() {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(task);
        return executor
                .retryOn(clazzException)
                .withExponentialBackoff(INITIAL_DELAY_MILLIS, MULTIPLIER)
                .withProportionalJitter()
                .withMaxRetries(maxRetries)
                .withMinDelay(minDelay)
                .getWithRetry(task);
    }

}
