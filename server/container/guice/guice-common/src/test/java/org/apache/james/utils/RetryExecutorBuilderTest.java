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

import com.nurkiewicz.asyncretry.AsyncRetryExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

public class RetryExecutorBuilderTest {
    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final int MIN_DELAY = 3000;
    @Mock
    protected FaultyService serviceMock;

    private ScheduledExecutorService scheduledExecutor;
    private CompletableFuture<String> result;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    @After
    public void tearDown() throws Exception {
        scheduledExecutor.shutdownNow();
    }

    @Test
    public void retryOnTaskShouldThrowWhenNullExecutor() throws Exception {
        assertThatThrownBy(() -> new RetryExecutorBuilder().retryOnTask())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void retryOnTaskShouldThrowWhenNoRetryTask() throws Exception {
        assertThatThrownBy(() -> new RetryExecutorBuilder()
                    .executor(new AsyncRetryExecutor(scheduledExecutor))
                    .maxRetries(DEFAULT_MAX_RETRIES)
                    .minDelay(MIN_DELAY)
                    .clazzException(IllegalArgumentException.class)
                    .retryOnTask())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void retryOnTaskShouldNotRetryWhenNullBindingOnExceptionTryOn() throws Exception {
        given(serviceMock.faultyService())
                .willThrow(IllegalArgumentException.class)
                .willReturn("Foo");
        result = new RetryExecutorBuilder()
                .executor(new AsyncRetryExecutor(scheduledExecutor))
                .maxRetries(DEFAULT_MAX_RETRIES)
                .minDelay(MIN_DELAY)
                .task(context -> serviceMock.faultyService())
                .retryOnTask();

        assertThatThrownBy(() -> result.get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void retryOnTaskShouldRethrowWhenScheduledServiceAlwaysThrowException() throws Exception {
        given(serviceMock.faultyService())
                .willThrow(IllegalArgumentException.class)
                .willThrow(IllegalArgumentException.class)
                .willThrow(IllegalArgumentException.class);

        result = new RetryExecutorBuilder()
                .executor(new AsyncRetryExecutor(scheduledExecutor))
                .maxRetries(DEFAULT_MAX_RETRIES)
                .minDelay(MIN_DELAY)
                .clazzException(IllegalArgumentException.class)
                .task(context -> serviceMock.faultyService())
                .retryOnTask();

        assertThatThrownBy(() -> result.get())
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void retryOnTaskAndExecuteShouldNotRetryWhenDoesNotMatchException() throws Exception {
        given(serviceMock.faultyService())
                .willThrow(IllegalStateException.class)
                .willReturn("Foo");

        result = new RetryExecutorBuilder()
                .executor(new AsyncRetryExecutor(scheduledExecutor))
                .maxRetries(DEFAULT_MAX_RETRIES)
                .minDelay(MIN_DELAY)
                .clazzException(IllegalArgumentException.class)
                .task(context -> serviceMock.faultyService())
                .retryOnTask();

        assertThatThrownBy(() -> result.get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    public void retryOnTaskAndExecuteShouldRetryWithMaxTimesAndReturnValue() throws Exception {
        given(serviceMock.faultyService())
                .willThrow(IllegalStateException.class, IllegalStateException.class, IllegalStateException.class)
                .willReturn("Foo");

        result = new RetryExecutorBuilder()
                .executor(new AsyncRetryExecutor(scheduledExecutor))
                .maxRetries(DEFAULT_MAX_RETRIES)
                .minDelay(MIN_DELAY)
                .clazzException(IllegalStateException.class)
                .task(context -> serviceMock.faultyService())
                .retryOnTask();

        assertThat(result.get()).isEqualTo("Foo");
    }

    @Test
    public void retryOnTaskAndExecuteShouldFailIfFailMoreThanMaxRetry() throws Exception {
        given(serviceMock.faultyService()).
            willThrow(IllegalStateException.class, IllegalStateException.class, IllegalStateException.class, IllegalStateException.class).
            willReturn("Foo");

        result = new RetryExecutorBuilder()
                .executor(new AsyncRetryExecutor(scheduledExecutor))
                .maxRetries(DEFAULT_MAX_RETRIES)
                .minDelay(MIN_DELAY)
                .clazzException(IllegalStateException.class)
                .task(context -> serviceMock.faultyService())
                .retryOnTask();

        assertThatThrownBy(() -> result.get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}