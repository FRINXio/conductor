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

/** Service used for computation of the evaluation offset for the postponed task. */
public sealed interface TaskOffsetEvaluation
        permits BackoffToDefaultOffsetEvaluation,
                ConstantDefaultOffsetEvaluation,
                ScaledByQueueSizeOffsetEvaluation {
    /**
     * Compute the evaluation offset for the postponed task.
     *
     * @param taskModel details about the postponed task
     * @param defaultOffset the default offset provided by the configuration properties [seconds]
     * @param queueSize the actual size of the queue before the task is postponed
     * @return the computed evaluation offset [seconds]
     */
    long computeEvaluationOffset(TaskModel taskModel, long defaultOffset, int queueSize);
}
