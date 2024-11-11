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

import com.netflix.conductor.model.TaskModel;

/** Dummy implementation of {@link TaskOffsetEvaluation} that always returns the default offset. */
final class ConstantDefaultOffsetEvaluation implements TaskOffsetEvaluation {
    @Override
    public long computeEvaluationOffset(
            final TaskModel taskModel, final long defaultOffset, final int queueSize) {
        return defaultOffset;
    }
}
