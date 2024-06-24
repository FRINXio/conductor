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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RbacHttpFilter implements Filter {

    private UserType user;

    private final RbacProperties properties;

    private List<String> roles;

    private HttpServletRequest request;

    private boolean testingUser;

    public RbacHttpFilter(RbacProperties properties) {
        this.properties = properties;
    }

    /**
     * Intercepts HTTP requests and filters them based on certain criteria.
     *
     * <p>If request is of type "healthcheck", no validation is done. Otherwise, method stores all
     * groups and roles values from request, validates headers by checking if headers names contains
     * "from" and creates UserType object due to provided header values in properties file and in
     * request.
     *
     * <p>If validation criteria are met, method proceeds with filtering. Otherwise, sends 401
     * error.
     *
     * @param servletRequest ServletRequest object representing the HTTP request.
     * @param servletResponse ServletResponse object representing the HTTP response
     * @param filterChain FilterChain object to proceed with the filter chain.
     * @throws IOException IOException if an input or output error occurs while filtering the
     *     request or response.
     * @throws ServletException ServletException if the request could not be handled.
     */
    @Override
    public void doFilter(
            ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        final String healthCheck = "health";
        if (request.getRequestURI().contains(healthCheck)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        setRolesList(request);
        this.request = request;

        if (validateHeaders(Collections.list(request.getHeaderNames()))) {
            if (!testingUser) {
                user = createUser(getAdminRoles(), roles);
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
        }
    }

    /**
     * Checks if headers names contains "from".
     *
     * @param headers List of headers names.
     * @return True if found, false otherwise.
     */
    private boolean validateHeaders(List<String> headers) {
        final String fromHeader = "from";
        return headers.stream().anyMatch(fromHeader::equals);
    }

    /**
     * Creates a user object based on the provided roles and groups, considering administrative
     * access.
     *
     * <p>If roles and groups from request contain same values as set in properties file, UserType
     * object is created as admin. Otherwise, UserType object is created as user.
     *
     * @param adminRoles Roles and groups set in properties file
     * @param roles Roles and groups from the request
     * @return UserType object representing the user with appropriate roles and administrative
     *     status.
     */
    private UserType createUser(List<String> adminRoles, List<String> roles) {
        if (adminRoles.stream().anyMatch(roles::contains)) {
            return new UserType(adminRoles, true);
        } else {
            if (!roles.isEmpty()) {
                return new UserType(roles, false);
            } else {
                return new UserType(Collections.emptyList(), false);
            }
        }
    }

    /**
     * Stores roles and groups from request into collection.
     *
     * <p>By using getHeadersList(), method gets all values from specified headers and stores them
     * into collection.
     *
     * @param request HttpServletRequest object
     */
    private void setRolesList(HttpServletRequest request) {
        final String requestRoles = "x-auth-user-roles";
        final String requestGroups = "x-auth-user-groups";
        roles =
                Stream.of(
                                getHeadersList(request.getHeader(requestRoles)),
                                getHeadersList(request.getHeader(requestGroups)))
                        .flatMap(List::stream)
                        .toList();
    }

    /**
     * Returns list of all values present in specified request header.
     *
     * <p>Method updates received value from request header if present and returns all values from
     * request header in list.
     *
     * @param request Name of the request header
     * @return Collection of values from specified header if header is present. Otherwise, returns
     *     empty list.
     */
    private List<String> getHeadersList(String request) {
        if (request != null && !request.isEmpty()) {
            return Arrays.stream(request.split(",\\s*")).map(String::trim).toList();
        }
        return Collections.emptyList();
    }

    /**
     * Returns admin roles and groups as admin roles list.
     *
     * @return List of admin roles and groups
     */
    private List<String> getAdminRoles() {
        List<String> adminRoles =
                new ArrayList<>(
                        Optional.ofNullable(properties.getAdminRoles())
                                .orElse(Collections.emptyList()));
        List<String> adminGroups =
                Optional.ofNullable(properties.getAdminGroups()).orElse(Collections.emptyList());
        adminRoles.addAll(adminGroups);
        return adminRoles;
    }

    public UserType getUser() {
        return user;
    }

    public void setUser(UserType user) {
        this.user = user;
    }

    public List<String> getRoles() {
        return roles;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setTestingUser(boolean testingUser) {
        this.testingUser = testingUser;
    }
}
