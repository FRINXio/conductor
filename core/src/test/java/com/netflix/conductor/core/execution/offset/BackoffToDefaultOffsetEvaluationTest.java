/*
 * Copyright 2024 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.core.execution.offset;

import java.time.Duration;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.netflix.conductor.core.config.ConductorProperties;
import com.netflix.conductor.model.TaskModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackoffToDefaultOffsetEvaluationTest {

    @Mock private TaskModel taskModel;
    @Mock private ConductorProperties conductorProperties;

    @ParameterizedTest
    @CsvSource({"0, 5, 0", "1, 5, 0", "2, 5, 2", "3, 5, 4", "4, 5, 5", "4, 10, 8", "5, 10, 10"})
    void testComputeEvaluationOffset(
            final int pollCount, final long defaultOffset, final long expectedOffset) {
        when(conductorProperties.getSystemTaskWorkerCallbackDuration())
                .thenReturn(Duration.ofSeconds(defaultOffset));
        final var offsetEvaluation = new BackoffToDefaultOffsetEvaluation(conductorProperties);
        when(taskModel.getPollCount()).thenReturn(pollCount);
        final var result = offsetEvaluation.computeEvaluationOffset(taskModel, 10);
        assertEquals(expectedOffset, result);
    }
}
