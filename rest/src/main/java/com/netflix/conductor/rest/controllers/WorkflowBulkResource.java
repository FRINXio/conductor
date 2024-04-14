/*
 * Copyright 2021 Netflix, Inc.
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
package com.netflix.conductor.rest.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;

import com.netflix.conductor.common.model.BulkResponse;
import com.netflix.conductor.rest.rbac.HeaderValidatorFilter;
import com.netflix.conductor.service.WorkflowBulkService;

import io.swagger.v3.oas.annotations.Operation;

import static com.netflix.conductor.rest.config.RequestMappingConstants.WORKFLOW_BULK;

/** Synchronous Bulk APIs to process the workflows in batches */
@RestController
@RequestMapping(WORKFLOW_BULK)
public class WorkflowBulkResource {

    private final WorkflowBulkService workflowBulkService;

    public WorkflowBulkResource(WorkflowBulkService workflowBulkService) {
        this.workflowBulkService = workflowBulkService;
    }

    @Autowired HeaderValidatorFilter filter;

    /**
     * Pause the list of workflows.
     *
     * @param workflowIds - list of workflow Ids to perform pause operation on
     * @return bulk response object containing a list of succeeded workflows and a list of failed
     *     ones with errors
     */
    @PutMapping("/pause")
    @Operation(summary = "Pause the list of workflows")
    public BulkResponse pauseWorkflow(@RequestBody List<String> workflowIds) {

        if (filter.getUser().isAdmin()) {
            return workflowBulkService.pauseWorkflow(workflowIds);
        } else {
            List<String> userIds =
                    workflowBulkService.getUserIds(
                            filter.getUser().getGroupsAndRoles(), workflowIds);
            if (!userIds.isEmpty()) {
                return workflowBulkService.pauseWorkflow(userIds);
            } else {
                throw new HttpServerErrorException(
                        HttpStatus.UNAUTHORIZED, "Workflows from list cannot be paused by user");
            }
        }
    }

    /**
     * Resume the list of workflows.
     *
     * @param workflowIds - list of workflow Ids to perform resume operation on
     * @return bulk response object containing a list of succeeded workflows and a list of failed
     *     ones with errors
     */
    @PutMapping("/resume")
    @Operation(summary = "Resume the list of workflows")
    public BulkResponse resumeWorkflow(@RequestBody List<String> workflowIds) {

        if (filter.getUser().isAdmin()) {
            return workflowBulkService.resumeWorkflow(workflowIds);
        } else {
            List<String> userIds =
                    workflowBulkService.getUserIds(
                            filter.getUser().getGroupsAndRoles(), workflowIds);
            if (!userIds.isEmpty()) {
                return workflowBulkService.resumeWorkflow(userIds);
            } else {
                throw new HttpServerErrorException(
                        HttpStatus.UNAUTHORIZED, "Workflows from list cannot be resumed by user");
            }
        }
    }

    /**
     * Restart the list of workflows.
     *
     * @param workflowIds - list of workflow Ids to perform restart operation on
     * @param useLatestDefinitions if true, use latest workflow and task definitions upon restart
     * @return bulk response object containing a list of succeeded workflows and a list of failed
     *     ones with errors
     */
    @PostMapping("/restart")
    @Operation(summary = "Restart the list of completed workflow")
    public BulkResponse restart(
            @RequestBody List<String> workflowIds,
            @RequestParam(value = "useLatestDefinitions", defaultValue = "false", required = false)
                    boolean useLatestDefinitions) {

        if (filter.getUser().isAdmin()) {
            return workflowBulkService.restart(workflowIds, useLatestDefinitions);
        } else {
            List<String> userIds =
                    workflowBulkService.getUserIds(
                            filter.getUser().getGroupsAndRoles(), workflowIds);
            if (!userIds.isEmpty()) {
                return workflowBulkService.restart(userIds, useLatestDefinitions);
            } else {
                throw new HttpServerErrorException(
                        HttpStatus.UNAUTHORIZED, "Workflows from list cannot be restarted by user");
            }
        }
    }

    /**
     * Retry the last failed task for each workflow from the list.
     *
     * @param workflowIds - list of workflow Ids to perform retry operation on
     * @return bulk response object containing a list of succeeded workflows and a list of failed
     *     ones with errors
     */
    @PostMapping("/retry")
    @Operation(summary = "Retry the last failed task for each workflow from the list")
    public BulkResponse retry(@RequestBody List<String> workflowIds) {

        if (filter.getUser().isAdmin()) {
            return workflowBulkService.retry(workflowIds);
        } else {
            List<String> userIds =
                    workflowBulkService.getUserIds(
                            filter.getUser().getGroupsAndRoles(), workflowIds);
            if (!userIds.isEmpty()) {
                return workflowBulkService.retry(userIds);
            } else {
                throw new HttpServerErrorException(
                        HttpStatus.UNAUTHORIZED, "Workflows from list cannot be retried by user");
            }
        }
    }

    /**
     * Terminate workflows execution.
     *
     * @param workflowIds - list of workflow Ids to perform terminate operation on
     * @param reason - description to be specified for the terminated workflow for future
     *     references.
     * @return bulk response object containing a list of succeeded workflows and a list of failed
     *     ones with errors
     */
    @PostMapping("/terminate")
    @Operation(summary = "Terminate workflows execution")
    public BulkResponse terminate(
            @RequestBody List<String> workflowIds,
            @RequestParam(value = "reason", required = false) String reason) {

        if (filter.getUser().isAdmin()) {
            return workflowBulkService.terminate(workflowIds, reason);
        } else {
            List<String> userIds =
                    workflowBulkService.getUserIds(
                            filter.getUser().getGroupsAndRoles(), workflowIds);
            if (!userIds.isEmpty()) {
                return workflowBulkService.terminate(userIds, reason);
            } else {
                throw new HttpServerErrorException(
                        HttpStatus.UNAUTHORIZED,
                        "Workflows from list cannot be terminated by user");
            }
        }
    }
}
