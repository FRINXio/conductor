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

/**
 * Strategies used for computation of the task offset. The offset is used to postpone the task
 * execution in the queue.
 */
public enum OffsetEvaluationStrategy {
    /**
     * Constant offset evaluation strategy - using default offset value.
     *
     * @see ConstantDefaultOffsetEvaluation
     */
    CONSTANT_DEFAULT_OFFSET(new ConstantDefaultOffsetEvaluation()),
    /**
     * Computes the evaluation offset for a postponed task based on the task's poll count and a
     * default offset. In this strategy offset increases exponentially until it reaches the default
     * offset.
     *
     * @see BackoffToDefaultOffsetEvaluation
     */
    BACKOFF_TO_DEFAULT_OFFSET(new BackoffToDefaultOffsetEvaluation()),
    /**
     * Computes the evaluation offset for a postponed task based on the queue size and the task's
     * poll count. In this strategy offset increases exponentially until it reaches the (default
     * offset * queue size) value.
     *
     * @see ScaledByQueueSizeOffsetEvaluation
     */
    SCALED_BY_QUEUE_SIZE(new ScaledByQueueSizeOffsetEvaluation());

    private final TaskOffsetEvaluation taskOffsetEvaluation;

    OffsetEvaluationStrategy(final TaskOffsetEvaluation taskOffsetEvaluation) {
        this.taskOffsetEvaluation = taskOffsetEvaluation;
    }

    /**
     * Get the task offset evaluation strategy.
     *
     * @return {@link TaskOffsetEvaluation}
     */
    public TaskOffsetEvaluation getTaskOffsetEvaluation() {
        return taskOffsetEvaluation;
    }
}
