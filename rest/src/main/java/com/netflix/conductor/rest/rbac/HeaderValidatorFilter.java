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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

@Component
@EnableConfigurationProperties(RbacProperties.class)
public class HeaderValidatorFilter implements Filter {

    private UserType user;

    private final RbacProperties properties;

    @Autowired
    public HeaderValidatorFilter(RbacProperties properties) {
        this.properties = properties;
    }

    @Override
    public void doFilter(
            ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (validateHeaders(Collections.list(request.getHeaderNames()))) {
            user = createUser(getAdminRolesAndGroups(), getRolesAndGroupsList(request));
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private boolean validateHeaders(List<String> headers) {
        return headers.stream().anyMatch("from"::equals);
    }

    private UserType createUser(List<String> adminRolesAndGroups, List<String> rolesAndGroups) {
        if (adminRolesAndGroups.stream().anyMatch(rolesAndGroups::contains)) {
            return new UserType(adminRolesAndGroups, true);
        } else {
            if (!rolesAndGroups.isEmpty()) {
                return new UserType(rolesAndGroups, false);
            } else {
                throw new HttpServerErrorException(HttpStatus.UNAUTHORIZED);
            }
        }
    }

    private List<String> getRolesAndGroupsList(HttpServletRequest request) {
        return Stream.of(
                        getHeadersList(request.getHeader("x-auth-user-roles")),
                        getHeadersList(request.getHeader("x-auth-user-groups")))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<String> getHeadersList(String request) {
        if (request != null && !request.isEmpty()) {
            return Arrays.stream(request.split(",\\s*"))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private List<String> getAdminRolesAndGroups() {
        String[] adminRoles = Optional.ofNullable(properties.getAdminRoles()).orElse(new String[0]);
        String[] adminGroups =
                Optional.ofNullable(properties.getAdminGroups()).orElse(new String[0]);

        return Stream.concat(Arrays.stream(adminRoles), Arrays.stream(adminGroups))
                .collect(Collectors.toList());
    }

    public UserType getUser() {
        return user;
    }

    public void setUser(UserType user) {
        this.user = user;
    }
}
