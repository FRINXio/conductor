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

/**
 * Computes the evaluation offset for a postponed task based on the queue size and the task's poll
 * count. In this strategy offset increases exponentially until it reaches the (default offset *
 * queue size) value.<br>
 * This strategy is appropriate for relatively big queues (100-1000s tasks) that contain
 * long-running tasks (days-weeks) with high number of poll-counts.<br>
 * Sample evaluationOffset for different pollCounts, defaultOffset and queueSize:
 *
 * <table>
 * <tr><th>pollCount</th><th>defaultOffset</th><th>queueSize</th><th>evaluationOffset</th></tr>
 * <tr><td>0</td><td>-</td><td>-</td><td>0</td></tr>
 * <tr><td>1</td><td>-</td><td>-</td><td>0</td></tr>
 * <tr><td>2</td><td>5</td><td>1</td><td>2</td></tr>
 * <tr><td>3</td><td>5</td><td>1</td><td>4</td></tr>
 * <tr><td>4</td><td>5</td><td>1</td><td>5</td></tr>
 * <tr><td>4</td><td>5</td><td>0</td><td>5</td></tr>
 * <tr><td>4</td><td>5</td><td>2</td><td>8</td></tr>
 * </table>
 */
@Component
final class ScaledByQueueSizeOffsetEvaluation implements TaskOffsetEvaluation {

    private final long defaultOffset;

    ScaledByQueueSizeOffsetEvaluation(final ConductorProperties conductorProperties) {
        defaultOffset = conductorProperties.getSystemTaskWorkerCallbackDuration().toSeconds();
    }

    @Override
    public OffsetEvaluationStrategy type() {
        return OffsetEvaluationStrategy.SCALED_BY_QUEUE_SIZE;
    }

    @Override
    public long computeEvaluationOffset(final TaskModel taskModel, final int queueSize) {
        int index = taskModel.getPollCount() > 0 ? taskModel.getPollCount() - 1 : 0;
        if (index == 0) {
            return 0L;
        }
        final long scaledOffset = queueSize > 0 ? queueSize * defaultOffset : defaultOffset;
        return Math.min((long) Math.pow(2, index), scaledOffset);
    }
}
