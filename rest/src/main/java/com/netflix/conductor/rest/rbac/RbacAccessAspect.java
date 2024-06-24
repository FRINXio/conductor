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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.netflix.conductor.common.metadata.BaseDef;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.model.BulkResponse;
import com.netflix.conductor.core.exception.NotFoundException;

@Aspect
@Component
public class RbacAccessAspect {

    private final RbacDbHandler handler;

    private final RbacHttpFilter filter;

    private Object[] arguments;

    public RbacAccessAspect(RbacDbHandler handler, RbacHttpFilter filter) {
        this.handler = handler;
        this.filter = filter;
    }

    @Pointcut("@annotation(com.netflix.conductor.rest.rbac.annotations.RbacBulkAccess)")
    public void rbacBulkAccess() {}

    /**
     * Executes methods with admin access control.
     *
     * <p>Proceeds only in situations when user is admin. Otherwise, throws 403.
     *
     * @param joinPoint The proceeding join point, enabling interception of the method call.
     * @return The result of the intercepted method call.
     * @throws Throwable Throws any throwable exception that occurs during method execution.
     */
    @Around("@annotation(com.netflix.conductor.rest.rbac.annotations.RbacAdminAccess)")
    public Object triggerAdminMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        if (filter.getUser().isAdmin()) {
            return joinPoint.proceed();
        } else {
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Returns BulkResponse object based on workflow IDs, incorporating role-based access control
     * (RBAC).
     *
     * <p>When user is not admin, method checks first if roles and groups in header of request are
     * present. Ids in request body are checked for presence in db and existing ones checked for
     * groups and roles. Ids, that matched are set as a new request body parameter and method
     * proceeds. After returning response from endpoint, BulkResponse object is modified with failed
     * ids such as ids are missing or user is not eligible to trigger ids and updated BulkResponse
     * object is returned.
     *
     * @param joinPoint The proceeding join point, enabling interception of the method call.
     * @param workflowIds A list of workflow IDs for which bulk response is requested.
     * @return The bulk response object.
     * @throws Throwable Throws any throwable exception that occurs during method execution.
     */
    @Around("rbacBulkAccess() && args(workflowIds,..))")
    public Object getBulkResponse(ProceedingJoinPoint joinPoint, List<String> workflowIds)
            throws Throwable {

        if (filter.getUser().isAdmin()) {
            return joinPoint.proceed();
        } else if (!filter.getUser().getRoles().isEmpty()) {
            List<String> presentIds = handler.getPresentBulkIds(workflowIds);
            List<String> ids = handler.getUserIds(filter.getRoles());

            if (!ids.isEmpty()) {
                List<String> userIds = workflowIds.stream().filter(ids::contains).toList();
                Object[] newArgs = Arrays.copyOf(joinPoint.getArgs(), joinPoint.getArgs().length);
                newArgs[0] = userIds;
                BulkResponse response = (BulkResponse) joinPoint.proceed(newArgs);
                return modifyBulkResponse(workflowIds, presentIds, ids, response);
            }
        }
        throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
    }

    /**
     * A method to retrieve an object based on the method signature, incorporating role-based access
     * control (RBAC).
     *
     * <p>When user is not admin, method checks first if roles and groups in header of request are
     * present. Method first stores arguments from join point and URI of request.
     *
     * <p>When no arguments were provided in the request, user triggered endpoint to get all task or
     * workflow definitions. In this case method returns definitions directly via getDefinitions()
     * method.
     *
     * <p>When URI contains "search", db handler returns all workflow summaries accessible by
     * specified user and when URI contains "v2", db handler returns all workflows.
     *
     * <p>If previous criteria were not met, method checks if given arguments exists in the db and
     * if user is eligible and afterward proceeds to return expected object. Otherwise, 404 is
     * thrown when data were not found in db or 403 is thrown when user has no access.
     *
     * @param joinPoint The proceeding join point, enabling interception of the method call.
     * @return The retrieved object based variables.
     * @throws Throwable Throws any throwable exception that occurs during method execution.
     */
    @Around("@annotation(com.netflix.conductor.rest.rbac.annotations.RbacPathVarObject)")
    public Object getPathVarObject(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = filter.getRequest();

        final String search = "search";
        final String searchV2 = "v2";

        if (filter.getUser().isAdmin()) {
            return joinPoint.proceed();
        } else if (!filter.getUser().getRoles().isEmpty()) {
            String objectType = getUriObjectType(request.getRequestURI());
            arguments = joinPoint.getArgs();

            if (arguments.length == 0) {
                return getDefinitions(objectType);
            }

            String type = getType(arguments, request);
            if (type.contains(search)) {
                if (type.contains(searchV2)) {
                    return handler.getUserWorkflows(filter.getRoles());
                }
                return handler.getUserSummaries(filter.getRoles());
            }

            boolean hasAccess = handler.hasAccess(arguments, filter.getRoles(), type);
            boolean exists = handler.exists(arguments, type);

            if (exists) {
                if (hasAccess) {
                    return joinPoint.proceed();
                }
            } else {
                throw new NotFoundException(
                        "No such %s with param %s found.", objectType, arguments[0]);
            }
        }
        throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
    }

    /**
     * Determines the type of request based on the provided arguments and HTTP request.
     *
     * <p>If the first argument is an instance of {@link StartWorkflowRequest}, method sets name and
     * version as arguments. It then returns the string "startWf" to indicate a start workflow
     * request.
     *
     * <p>Otherwise, it returns the URI of the HTTP request.
     *
     * @param args The arguments provided to the method.
     * @param request The HTTP servlet request associated with the method call.
     * @return The type of request, either "startWf" for starting a workflow or the URI of the HTTP
     *     request.
     */
    private String getType(Object[] args, HttpServletRequest request) {
        if (args[0] instanceof StartWorkflowRequest wfRequest) {
            final String startWf = "startWf";
            arguments = new Object[] {wfRequest.getName(), wfRequest.getVersion()};
            return startWf;
        }
        return request.getRequestURI();
    }

    /**
     * Returns list of TaskDefs or WorkflowDefs accessible by user or empty list.
     *
     * <p>Method uses db handler to get definitions due to provided groups and roles and URI.
     *
     * @param objectType The URI of the request.
     * @return List of either TaskDefs or WorkflowDefs accessible by user. Otherwise, if no
     *     definitions were found, returns empty list.
     */
    private List<? extends BaseDef> getDefinitions(String objectType) {
        List<? extends BaseDef> definitions = handler.getUserDefs(filter.getRoles(), objectType);
        if (!definitions.isEmpty()) {
            return definitions;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Helper to determine if request targets task or workflow data.
     *
     * @param uri URI of the request.
     * @return Either "task" or "workflow" due to URI.
     */
    private String getUriObjectType(String uri) {
        final String task = "task";
        final String workflow = "workflow";
        return uri.contains(task) ? task : workflow;
    }

    /**
     * Modifies returned BulkResponse object.
     *
     * <p>Method modifies BulkResponse object by appending failed ids in situation when ids from
     * request were not found in the db or user is not eligible to work with.
     *
     * @param requestIds All ids from original request body.
     * @param presentIds Ids from original request body which were found in the db.
     * @param userIds Ids that user can work with.
     * @param response BulkResponse object.
     * @return Modifier BulkResponse object.
     */
    private BulkResponse modifyBulkResponse(
            List<String> requestIds,
            List<String> presentIds,
            List<String> userIds,
            BulkResponse response) {
        List<String> notAccessibleIds =
                new ArrayList<>(requestIds.stream().filter(id -> !userIds.contains(id)).toList());

        if (!notAccessibleIds.isEmpty()) {
            List<String> notFoundIds =
                    notAccessibleIds.stream().filter(id -> !presentIds.contains(id)).toList();
            if (!notFoundIds.isEmpty()) {
                notAccessibleIds.removeAll(notFoundIds);
                notFoundIds.forEach(
                        id ->
                                response.appendFailedResponse(
                                        id, "No such workflow found by id: " + id));
            }
            notAccessibleIds.forEach(
                    id ->
                            response.appendFailedResponse(
                                    id,
                                    "User is not authorized to trigger workflow with id: " + id));
        }
        return response;
    }
}
