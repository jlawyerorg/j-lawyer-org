/*
 * Copyright (C) 2026 Jens Kutschke
 *
 * This file is part of j-lawyer.org.
 *
 * j-lawyer.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * j-lawyer.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with j-lawyer.org.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jlawyer.io.rest.v8.pojo;

import java.util.ArrayList;
import java.util.List;
import org.jlawyer.io.rest.v6.pojo.RestfulIdNameV6;

/**
 * The self-service profile of the currently authenticated user — the read model behind the web
 * equivalent of the desktop {@code UserProfileDialog}. It carries the read-only identity block
 * (principal, display name, abbreviation, primary group, e-mail), the editable per-user
 * {@link RestfulProfileSettingsV8 settings}, the group options needed by the "Neue Akten" tab, and
 * whether the server requires a strong password (so the client can validate a password change).
 */
public class RestfulProfileV8 {

    private String principalId;
    private String displayName;
    private String firstName;
    private String name;
    private String email;
    private String abbreviation;
    private String primaryGroupId;
    private String primaryGroupName;

    /** Whether the server enforces password complexity (mirrors the desktop's strong-password rule). */
    private boolean passwordComplexityRequired = true;

    private RestfulProfileSettingsV8 settings = new RestfulProfileSettingsV8();

    /** All groups, for the "berechtigte Gruppen" (allowed groups) selection. */
    private List<RestfulIdNameV6> allGroups = new ArrayList<>();
    /** The groups the user is a member of, for the "Standard-Eigentümergruppe" (owner group) choice. */
    private List<RestfulIdNameV6> memberGroups = new ArrayList<>();

    public RestfulProfileV8() {
    }

    public String getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(String principalId) {
        this.principalId = principalId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getPrimaryGroupId() {
        return primaryGroupId;
    }

    public void setPrimaryGroupId(String primaryGroupId) {
        this.primaryGroupId = primaryGroupId;
    }

    public String getPrimaryGroupName() {
        return primaryGroupName;
    }

    public void setPrimaryGroupName(String primaryGroupName) {
        this.primaryGroupName = primaryGroupName;
    }

    public boolean isPasswordComplexityRequired() {
        return passwordComplexityRequired;
    }

    public void setPasswordComplexityRequired(boolean passwordComplexityRequired) {
        this.passwordComplexityRequired = passwordComplexityRequired;
    }

    public RestfulProfileSettingsV8 getSettings() {
        return settings;
    }

    public void setSettings(RestfulProfileSettingsV8 settings) {
        this.settings = settings;
    }

    public List<RestfulIdNameV6> getAllGroups() {
        return allGroups;
    }

    public void setAllGroups(List<RestfulIdNameV6> allGroups) {
        this.allGroups = allGroups;
    }

    public List<RestfulIdNameV6> getMemberGroups() {
        return memberGroups;
    }

    public void setMemberGroups(List<RestfulIdNameV6> memberGroups) {
        this.memberGroups = memberGroups;
    }

}
