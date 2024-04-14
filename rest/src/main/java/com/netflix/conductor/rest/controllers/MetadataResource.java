/*
 * Copyright 2020 Netflix, Inc.
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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;

import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowDefSummary;
import com.netflix.conductor.common.model.BulkResponse;
import com.netflix.conductor.rest.rbac.HeaderValidatorFilter;
import com.netflix.conductor.rest.rbac.RbacUtils;
import com.netflix.conductor.service.MetadataService;

import io.swagger.v3.oas.annotations.Operation;

import static com.netflix.conductor.rest.config.RequestMappingConstants.METADATA;

@RestController
@RequestMapping(value = METADATA)
public class MetadataResource {

    private final MetadataService metadataService;

    public MetadataResource(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @Autowired HeaderValidatorFilter filter;

    @PostMapping("/workflow")
    @Operation(summary = "Create a new workflow definition")
    public void create(@RequestBody WorkflowDef workflowDef) {

        if (filter.getUser().isAdmin()) {
            metadataService.registerWorkflowDef(workflowDef);
        } else {
            throw new HttpServerErrorException(HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/workflow/validate")
    @Operation(summary = "Validates a new workflow definition")
    public void validate(@RequestBody WorkflowDef workflowDef) {
        metadataService.validateWorkflowDef(workflowDef);
    }

    @PutMapping("/workflow")
    @Operation(summary = "Create or update workflow definition")
    public BulkResponse update(@RequestBody List<WorkflowDef> workflowDefs) {

        if (filter.getUser().isAdmin()) {
            return metadataService.updateWorkflowDef(workflowDefs);
        }
        throw new HttpServerErrorException(HttpStatus.UNAUTHORIZED);
    }

    @Operation(summary = "Retrieves workflow definition along with blueprint")
    @GetMapping("/workflow/{name}")
    public WorkflowDef get(
            @PathVariable("name") String name,
            @RequestParam(value = "version", required = false) Integer version) {

        if (filter.getUser().isAdmin()) {
            return metadataService.getWorkflowDef(name, version);
        } else if (RbacUtils.isUserInGroup(
                filter.getUser().getGroupsAndRoles(),
                metadataService.getWorkflowDescription(name, version))) {
            return metadataService.getWorkflowDef(name, version);
        }
        throw new HttpServerErrorException(HttpStatus.UNAUTHORIZED);
    }

    @Operation(summary = "Retrieves all workflow definition along with blueprint")
    @GetMapping("/workflow")
    public List<WorkflowDef> getAll() {

        if (filter.getUser().isAdmin()) {
            return metadataService.getWorkflowDefs();
        } else {
            return metadataService.getUserWorkflowDefs(filter.getUser().getGroupsAndRoles());
        }
    }

    @Operation(summary = "Returns workflow names and versions only (no definition bodies)")
    @GetMapping("/workflow/names-and-versions")
    public Map<String, ? extends Iterable<WorkflowDefSummary>> getWorkflowNamesAndVersions() {
        return metadataService.getWorkflowNamesAndVersions();
    }

    @Operation(summary = "Returns only the latest version of all workflow definitions")
    @GetMapping("/workflow/latest-versions")
    public List<WorkflowDef> getAllWorkflowsWithLatestVersions() {
        return metadataService.getWorkflowDefsLatestVersions();
    }

    @DeleteMapping("/workflow/{name}/{version}")
    @Operation(
            summary =
                    "Removes workflow definition. It does not remove workflows associated with the definition.")
    public void unregisterWorkflowDef(
            @PathVariable("name") String name, @PathVariable("version") Integer version) {

        if (filter.getUser().isAdmin()) {
            metadataService.unregisterWorkflowDef(name, version);
        } else {
            throw new HttpServerErrorException(HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/taskdefs")
    @Operation(summary = "Create new task definition(s)")
    public void registerTaskDef(@RequestBody List<TaskDef> taskDefs) {

        if (filter.getUser().isAdmin()) {
            metadataService.registerTaskDef(taskDefs);
        } else {
            throw new HttpServerErrorException(HttpStatus.UNAUTHORIZED);
        }
    }

    @PutMapping("/taskdefs")
    @Operation(summary = "Update an existing task")
    public void registerTaskDef(@RequestBody TaskDef taskDef) {

        if (filter.getUser().isAdmin()) {
            metadataService.updateTaskDef(taskDef);
        } else {
            throw new HttpServerErrorException(HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping(value = "/taskdefs")
    @Operation(summary = "Gets all task definition")
    public List<TaskDef> getTaskDefs() {

        if (filter.getUser().isAdmin()) {
            return metadataService.getTaskDefs();
        } else {
            return metadataService.getUserTaskDefs(filter.getUser().getGroupsAndRoles());
        }
    }

    @GetMapping("/taskdefs/{tasktype}")
    @Operation(summary = "Gets the task definition")
    public TaskDef getTaskDef(@PathVariable("tasktype") String taskType) {

        if (filter.getUser().isAdmin()) {
            return metadataService.getTaskDef(taskType);
        } else if (RbacUtils.isUserInGroup(
                filter.getUser().getGroupsAndRoles(),
                metadataService.getTaskDescription(taskType))) {
            return metadataService.getTaskDef(taskType);
        }
        throw new HttpServerErrorException(HttpStatus.UNAUTHORIZED);
    }

    @DeleteMapping("/taskdefs/{tasktype}")
    @Operation(summary = "Remove a task definition")
    public void unregisterTaskDef(@PathVariable("tasktype") String taskType) {

        if (filter.getUser().isAdmin()) {
            metadataService.unregisterTaskDef(taskType);
        } else {
            throw new HttpServerErrorException(HttpStatus.UNAUTHORIZED);
        }
    }
}
