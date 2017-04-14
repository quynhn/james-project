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
import com.nurkiewicz.asyncretry.RetryExecutor;
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

public class RetryExecutorUtilTest {
    @Mock
    protected FaultyService serviceMock;

    private RetryExecutor retryExecutor;
    private ScheduledExecutorService scheduledExecutor;

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
    public void retryOnClazzAndExecuteShouldRethrowIfOnlyException() throws Exception {
        given(serviceMock.faultyService()).willThrow(IllegalArgumentException.class);
        retryExecutor = RetryExecutorUtil.retryOnExceptions(new AsyncRetryExecutor(scheduledExecutor), 2, 3000, IllegalArgumentException.class);

        assertThatThrownBy(() -> retryExecutor.getWithRetry(serviceMock::faultyService).get())
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void retryOnClazzAndExecuteShouldNCompletedWhenException() throws Exception {
        given(serviceMock.faultyService()).willThrow(IllegalArgumentException.class);
        retryExecutor = RetryExecutorUtil.retryOnExceptions(new AsyncRetryExecutor(scheduledExecutor), 2, 3000, IllegalArgumentException.class);

        final CompletableFuture<String> future = retryExecutor.getWithRetry(serviceMock::faultyService);

        assertThat(future.isCompletedExceptionally()).isFalse();
    }

    @Test
    public void retryOnClazzAndExecuteShouldRetryWhenMatchExceptionAndSuccess() throws Exception {
        given(serviceMock.faultyService())
                .willThrow(IllegalArgumentException.class)
                .willReturn("Foo");
        retryExecutor = RetryExecutorUtil.retryOnExceptions(new AsyncRetryExecutor(scheduledExecutor), 2, 3000, IllegalArgumentException.class);

        final CompletableFuture<String> future = retryExecutor.getWithRetry(serviceMock::faultyService);

        assertThat(future.get()).isEqualTo("Foo");
    }

    @Test
    public void retryOnClazzAndExecuteShouldNotRetryWhenDoesNotMatchException() throws Exception {
        given(serviceMock.faultyService())
                .willThrow(IllegalStateException.class)
                .willReturn("Foo");

        retryExecutor = RetryExecutorUtil.retryOnExceptions(new AsyncRetryExecutor(scheduledExecutor), 2, 3000, IllegalArgumentException.class);

        assertThatThrownBy(() -> retryExecutor.getWithRetry(serviceMock::faultyService).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    public void retryOnClazzAndExecuteShouldRetryWithMaxTimesAndReturnValue() throws Exception {
        given(serviceMock.faultyService())
                .willThrow(IllegalStateException.class, IllegalStateException.class, IllegalStateException.class)
                .willReturn("Foo");

        retryExecutor = RetryExecutorUtil.retryOnExceptions(new AsyncRetryExecutor(scheduledExecutor), 3, 3000, IllegalStateException.class);

        final CompletableFuture<String> future = retryExecutor.getWithRetry(serviceMock::faultyService);

        assertThat(future.get()).isEqualTo("Foo");
    }

    @Test
    public void retryOnClazzAndExecuteShouldFailIfFailMoreThanMaxRetry() throws Exception {
        given(serviceMock.faultyService()).
            willThrow(IllegalStateException.class, IllegalStateException.class, IllegalStateException.class, IllegalStateException.class).
            willReturn("Foo");

        retryExecutor = RetryExecutorUtil.retryOnExceptions(new AsyncRetryExecutor(scheduledExecutor), 3, 3000, IllegalStateException.class);

        assertThatThrownBy(() -> retryExecutor.getWithRetry(serviceMock::faultyService).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}