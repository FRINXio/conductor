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

/**
 * Computes the evaluation offset for a postponed task based on the task's poll count and a default
 * offset. In this strategy offset increases exponentially until it reaches the default offset.<br>
 * This strategy is appropriate for queues that require low latency of all tasks.<br>
 * Sample evaluationOffset for different pollCounts and defaultOffset (queueSize is ignored):
 *
 * <table>
 * <tr><th>pollCount</th><th>defaultOffset</th><th>evaluationOffset</th></tr>
 * <tr><td>0</td><td>5</td><td>0</td></tr>
 * <tr><td>1</td><td>5</td><td>0</td></tr>
 * <tr><td>2</td><td>5</td><td>2</td></tr>
 * <tr><td>3</td><td>5</td><td>4</td></tr>
 * <tr><td>4</td><td>5</td><td>5</td></tr>
 * <tr><td>4</td><td>10</td><td>8</td></tr>
 * <tr><td>5</td><td>10</td><td>10</td></tr>
 * </table>
 */
final class BackoffToDefaultOffsetEvaluation implements TaskOffsetEvaluation {

    @Override
    public long computeEvaluationOffset(
            final TaskModel taskModel, final long defaultOffset, final int queueSize) {
        final int index = taskModel.getPollCount() > 0 ? taskModel.getPollCount() - 1 : 0;
        if (index == 0) {
            return 0L;
        }
        return Math.min((long) Math.pow(2, index), defaultOffset);
    }
}
