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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.netflix.conductor.core.config.ConductorProperties;
import com.netflix.conductor.core.config.OffsetEvaluationStrategy;
import com.netflix.conductor.model.TaskModel;

/**
 * Computes the evaluation offset for a postponed task based on the task's duration and settings
 * that define the offset for different levels of task durations.<br>
 * In this strategy offset increases by steps based on settings that define the offset for different
 * levels of task durations. Task duration is derived from {@link TaskModel#getScheduledTime()} and
 * current time.<br>
 * This strategy is appropriate for tasks that have a wide range of durations and the offset should
 * be scaled based on the task's duration.<br>
 * The defined keys in the settings compose the duration intervals for which the offset will be set
 * to the corresponding value: <0, d1) = 0, <d1, d2) = d1, <d2, d3) = d2.<br>
 * The order of the keys is not important as the map is sorted by the key before the evaluation.
 */
@Component
final class ScaledByTaskDurationOffsetEvaluation implements TaskOffsetEvaluation {

    private final Map<Long, Long> taskDurationToOffsetSteps;

    ScaledByTaskDurationOffsetEvaluation(final ConductorProperties conductorProperties) {
        taskDurationToOffsetSteps = sortByTaskDuration(conductorProperties);
    }

    private static LinkedHashMap<Long, Long> sortByTaskDuration(
            final ConductorProperties conductorProperties) {
        return conductorProperties.getTaskDurationToOffsetSteps().entrySet().stream()
                .sorted(Entry.comparingByKey(Comparator.reverseOrder()))
                .collect(
                        Collectors.toMap(
                                Entry::getKey,
                                Entry::getValue,
                                (e1, e2) -> e1,
                                LinkedHashMap::new));
    }

    @Override
    public OffsetEvaluationStrategy type() {
        return OffsetEvaluationStrategy.SCALED_BY_TASK_DURATION;
    }

    @Override
    public long computeEvaluationOffset(final TaskModel taskModel, final int queueSize) {
        if (taskDurationToOffsetSteps.isEmpty()) {
            return 0L;
        }
        final long taskDuration =
                (System.currentTimeMillis() - taskModel.getScheduledTime()) / 1000;
        return taskDurationToOffsetSteps.entrySet().stream()
                .filter(entry -> taskDuration >= entry.getKey())
                .map(Entry::getValue)
                .findFirst()
                .orElse(0L);
    }
}
