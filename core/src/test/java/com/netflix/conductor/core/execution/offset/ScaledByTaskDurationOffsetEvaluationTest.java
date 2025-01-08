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

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.netflix.conductor.core.config.ConductorProperties;
import com.netflix.conductor.model.TaskModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScaledByTaskDurationOffsetEvaluationTest {

    @Mock private TaskModel taskModel;
    @Mock private ConductorProperties conductorProperties;

    private static Stream<Arguments> testOffsetsProvider() {
        return Stream.of(
                Arguments.of(Map.of(10L, 30L, 20L, 60L, 30L, 120L), 1L, 0L),
                Arguments.of(Map.of(10L, 30L, 20L, 60L, 30L, 120L), 11L, 30L),
                Arguments.of(Map.of(10L, 30L, 20L, 60L, 30L, 120L), 100L, 120L),
                Arguments.of(Collections.emptyMap(), 20L, 0L),
                Arguments.of(Map.of(30L, 120L, 20L, 60L, 10L, 0L, 100L, 1200L), 35L, 120L));
    }

    @ParameterizedTest
    @MethodSource("testOffsetsProvider")
    void testComputeEvaluationOffset(
            final Map<Long, Long> offsets, final long taskDuration, final long expectedOffset) {
        final long scheduledTime = System.currentTimeMillis() - (taskDuration * 1000);
        when(conductorProperties.getTaskDurationToOffsetSteps()).thenReturn(offsets);
        if (!offsets.isEmpty()) {
            when(taskModel.getScheduledTime()).thenReturn(scheduledTime);
        }

        final var offsetEvaluation = new ScaledByTaskDurationOffsetEvaluation(conductorProperties);
        final var result = offsetEvaluation.computeEvaluationOffset(taskModel, 50);
        assertEquals(expectedOffset, result);
    }
}
