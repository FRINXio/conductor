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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.netflix.conductor.core.config.OffsetEvaluationStrategy;

@Component
public final class TaskOffsetEvaluationSelector {

    private final Map<OffsetEvaluationStrategy, TaskOffsetEvaluation> evaluations;

    @Autowired
    public TaskOffsetEvaluationSelector(final List<TaskOffsetEvaluation> evaluations) {
        this.evaluations =
                evaluations.stream()
                        .collect(Collectors.toMap(TaskOffsetEvaluation::type, Function.identity()));
    }

    /**
     * Get the implementation of the offset evaluation for the given strategy.
     *
     * @param strategy the strategy to get the implementation for
     * @return {@link TaskOffsetEvaluation}
     * @throws IllegalStateException if no implementation is found for the given strategy
     */
    @NonNull
    public TaskOffsetEvaluation taskOffsetEvaluation(final OffsetEvaluationStrategy strategy) {
        final var taskOffsetEvaluation = evaluations.get(strategy);
        if (taskOffsetEvaluation == null) {
            throw new IllegalStateException(
                    "No TaskOffsetEvaluation found for strategy: " + strategy);
        }
        return taskOffsetEvaluation;
    }
}
