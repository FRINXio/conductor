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

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConfigurationProperties(prefix = "conductor.rbac.admin")
public class RbacProperties {

    private List<String> adminRoles = List.of("network-admin", "owner", "admin");

    private List<String> adminGroups = List.of("group-admin", "group-owner");

    public List<String> getAdminRoles() {
        return adminRoles;
    }

    public List<String> getAdminGroups() {
        return adminGroups;
    }

    public void setAdminRoles(List<String> adminRoles) {
        this.adminRoles = adminRoles;
    }

    public void setAdminGroups(List<String> adminGroups) {
        this.adminGroups = adminGroups;
    }
}
