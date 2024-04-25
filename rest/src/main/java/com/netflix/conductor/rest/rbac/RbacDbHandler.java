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
package com.netflix.conductor.rest.rbac;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.netflix.conductor.common.metadata.BaseDef;
import com.netflix.conductor.common.run.SearchResult;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.common.run.WorkflowSummary;
import com.netflix.conductor.core.exception.NotFoundException;
import com.netflix.conductor.service.MetadataService;
import com.netflix.conductor.service.WorkflowService;

@Component
public class RbacDbHandler {

    private final MetadataService metadataService;

    private final WorkflowService workflowService;

    private static final String METADATA_TYPE = "metadata";

    private static final String START_WF = "startWf";

    @Autowired
    public RbacDbHandler(MetadataService metadataService, WorkflowService workflowService) {
        this.metadataService = metadataService;
        this.workflowService = workflowService;
    }

    /**
     * Returns list of ids accessible by the specified user.
     *
     * <p>Method returns ids from db that share values in rbac_labels column with provided roles and
     * groups values from request.
     *
     * @param labels Roles and groups values from request.
     * @return List of user accessible ids if found. Otherwise, empty list.
     */
    public List<String> getUserIds(List<String> labels) {
        return !workflowService.getUserWorkflowIds(labels).isEmpty()
                ? workflowService.getUserWorkflowIds(labels)
                : Collections.emptyList();
    }

    /**
     * Checks if user has access to resource due to provided parameters.
     *
     * <p>Method determines if data should be returned from public or archive table and returns
     * true/false by getting data from db using provided parameters.
     *
     * @param args Request arguments.
     * @param labels Groups and roles list.
     * @param uri URI.
     * @return True if the user has access to the resource, false otherwise.
     */
    public boolean hasAccess(Object[] args, List<String> labels, String uri) {
        if (uri.contains(METADATA_TYPE) || uri.equals(START_WF)) {
            return metadataService.hasAccess(args, labels, uri);
        } else {
            return workflowService.hasAccess(args, labels);
        }
    }

    /**
     * Checks if resource exists in db due to provided parameters.
     *
     * <p>Method determines if data should be returned from public or archive table and returns
     * true/false if requested data were found.
     *
     * @param args Request arguments.
     * @param uri URI.
     * @return True if data exists in the db, false otherwise.
     */
    public boolean exists(Object[] args, String uri) {
        if (uri.contains(METADATA_TYPE) || uri.equals(START_WF)) {
            return metadataService.exists(args, uri);
        } else {
            return workflowService.exists(args);
        }
    }

    /**
     * Returns found ids in db from body of a request.
     *
     * @param ids Ids from request body.
     * @return List of ids present in db.
     */
    public List<String> getPresentBulkIds(List<String> ids) {
        return workflowService.getPresentIds(ids);
    }

    /**
     * Returns TaskDefs or WorkflowDefs accessible by the specified user.
     *
     * @param roles Roles and groups values list from request.
     * @param objectType Either "task" or "workflow".
     * @return List of task or workflow definitions.
     */
    public List<? extends BaseDef> getUserDefs(List<String> roles, String objectType) {
        final String taskType = "task";
        return objectType.equals(taskType)
                ? metadataService.getUserTaskDefs(roles)
                : metadataService.getUserWorkflowDefs(roles);
    }

    /**
     * Returns list of workflow summaries accessible by the specified user.
     *
     * @param roles Roles and groups values list from request.
     * @return List of workflow summaries accessible if found. Otherwise, throws 404.
     */
    public SearchResult<WorkflowSummary> getUserSummaries(List<String> roles) {
        SearchResult<WorkflowSummary> result =
                workflowService.getSummaries(workflowService.getSearchResultIds(roles));
        if (!result.getResults().isEmpty()) {
            return result;
        }
        throw new NotFoundException("No workflow summaries to be returned.");
    }

    /**
     * Returns list of workflows accessible by the specified user.
     *
     * @param roles Roles and groups values list from request.
     * @return List of workflows accessible if found. Otherwise, throws 404.
     */
    public SearchResult<Workflow> getUserWorkflows(List<String> roles) {
        SearchResult<Workflow> result =
                workflowService.getUserWorkflows(workflowService.getSearchResultIds(roles));
        if (!result.getResults().isEmpty()) {
            return result;
        }
        throw new NotFoundException("No workflows to be returned.");
    }
}
