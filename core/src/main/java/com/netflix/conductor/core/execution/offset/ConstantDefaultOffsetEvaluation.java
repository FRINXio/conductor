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

import org.springframework.stereotype.Component;

import com.netflix.conductor.core.config.ConductorProperties;
import com.netflix.conductor.core.config.OffsetEvaluationStrategy;
import com.netflix.conductor.model.TaskModel;

/** Dummy implementation of {@link TaskOffsetEvaluation} that always returns the default offset. */
@Component
final class ConstantDefaultOffsetEvaluation implements TaskOffsetEvaluation {

    private final long defaultOffset;

    ConstantDefaultOffsetEvaluation(final ConductorProperties conductorProperties) {
        defaultOffset = conductorProperties.getSystemTaskWorkerCallbackDuration().toSeconds();
    }

    @Override
    public OffsetEvaluationStrategy type() {
        return OffsetEvaluationStrategy.CONSTANT_DEFAULT_OFFSET;
    }

    @Override
    public long computeEvaluationOffset(final TaskModel taskModel, final int queueSize) {
        return defaultOffset;
    }
}
