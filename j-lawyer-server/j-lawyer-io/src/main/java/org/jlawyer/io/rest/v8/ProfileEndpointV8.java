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
package org.jlawyer.io.rest.v8;
import org.jlawyer.io.rest.tools.RestErrorResponses;

import com.jdimension.jlawyer.persistence.AppUserBean;
import com.jdimension.jlawyer.persistence.Group;
import com.jdimension.jlawyer.security.PasswordsUtil;
import com.jdimension.jlawyer.server.services.settings.ServerSettingsKeys;
import com.jdimension.jlawyer.server.services.settings.UserSettingsKeys;
import com.jdimension.jlawyer.services.SecurityServiceLocal;
import com.jdimension.jlawyer.services.SystemManagementLocal;
import com.jdimension.jlawyer.persistence.ServerSettingsBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.jboss.logging.Logger;
import org.jlawyer.io.rest.v6.pojo.RestfulIdNameV6;
import org.jlawyer.io.rest.v8.pojo.RestfulDashboardConfigV8;
import org.jlawyer.io.rest.v8.pojo.RestfulPasswordChangeV8;
import org.jlawyer.io.rest.v8.pojo.RestfulProfileSettingsV8;
import org.jlawyer.io.rest.v8.pojo.RestfulProfileV8;

/**
 * Self-service profile endpoints for the currently authenticated user — the REST equivalent of the
 * desktop {@code UserProfileDialog}. Unlike the {@code /v6/security} user administration (which
 * requires {@code adminRole} and operates on arbitrary users), everything here is scoped to the
 * <b>caller principal</b> and needs only {@code loginRole}, so any logged-in user can read and edit
 * their own profile.
 *
 * <p>It exposes the read-only identity block (abbreviation, primary group, e-mail), the editable
 * per-user settings (notifications / security / new-case defaults — shared with the desktop via the
 * {@code UserSettingsKeys} settings blob) and a set-new-password operation (no current-password
 * verification, matching the desktop dialog). Master data is intentionally not editable here (the
 * desktop profile dialog does not edit it either).</p>
 *
 * @author jens
 */
@Stateless
@Path("/v8/profile")
@Consumes({"application/json"})
@Produces({"application/json"})
@io.swagger.annotations.Api(tags = {"Profile"})
public class ProfileEndpointV8 implements ProfileEndpointLocalV8 {

    private static final Logger log = Logger.getLogger(ProfileEndpointV8.class.getName());
    private static final String LOOKUP_SYSMAN = "java:global/j-lawyer-server/j-lawyer-server-ejb/SystemManagement!com.jdimension.jlawyer.services.SystemManagementLocal";
    private static final String LOOKUP_SECSVC = "java:global/j-lawyer-server/j-lawyer-server-ejb/SecurityService!com.jdimension.jlawyer.services.SecurityServiceLocal";

    @Context
    private SecurityContext securityContext;

    /**
     * Returns the profile of the currently authenticated user: read-only identity, the editable
     * per-user settings, the group options for the "new cases" defaults and whether the server
     * enforces password complexity.
     *
     * @response 200 The caller's profile
     * @response 401 Not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the profile of the current user", response = RestfulProfileV8.class)
    public Response getProfile() {
        try {
            String principalId = securityContext.getUserPrincipal().getName();
            InitialContext ic = new InitialContext();
            SystemManagementLocal system = (SystemManagementLocal) ic.lookup(LOOKUP_SYSMAN);
            SecurityServiceLocal security = (SecurityServiceLocal) ic.lookup(LOOKUP_SECSVC);

            AppUserBean user = system.getUser(principalId);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            RestfulProfileV8 result = new RestfulProfileV8();
            result.setPrincipalId(user.getPrincipalId());
            result.setDisplayName(user.getDisplayName());
            result.setFirstName(user.getFirstName());
            result.setName(user.getName());
            result.setEmail(user.getEmail());
            result.setAbbreviation(user.getAbbreviation());
            Group primaryGroup = user.getPrimaryGroup();
            if (primaryGroup != null) {
                result.setPrimaryGroupId(primaryGroup.getId());
                result.setPrimaryGroupName(primaryGroup.getName());
            }

            result.setPasswordComplexityRequired(readBoolSetting(system, ServerSettingsKeys.SERVERCONF_SECURITY_FORCE_PASSWORDCOMPLEXITY, true));

            Properties settings = system.getUserSettings(user);
            result.setSettings(toSettings(settings));

            // groups: all groups (for allowed-groups selection), members-of (for owner group choice)
            Collection<Group> allGroups = security.getAllGroups();
            for (Group g : allGroups) {
                result.getAllGroups().add(new RestfulIdNameV6(g.getId(), g.getName()));
            }
            List<Group> memberGroups = security.getGroupsForUser(principalId);
            for (Group g : memberGroups) {
                result.getMemberGroups().add(new RestfulIdNameV6(g.getId(), g.getName()));
            }

            return Response.ok(result).build();
        } catch (Exception ex) {
            log.error("can not get profile", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Updates the editable per-user settings of the currently authenticated user (notifications,
     * security, new-case defaults). The values are merged into the caller's settings blob so keys
     * managed elsewhere (e.g. the avatar) are preserved.
     *
     * @param settings the settings to store
     * @response 200 The stored settings
     * @response 401 Not authenticated
     */
    @Override
    @PUT
    @Path("/settings")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Updates the per-user settings of the current user", response = RestfulProfileSettingsV8.class)
    public Response updateSettings(RestfulProfileSettingsV8 settings) {
        try {
            if (settings == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            String principalId = securityContext.getUserPrincipal().getName();
            InitialContext ic = new InitialContext();
            SystemManagementLocal system = (SystemManagementLocal) ic.lookup(LOOKUP_SYSMAN);

            AppUserBean user = system.getUser(principalId);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // merge onto the existing blob so keys we do not manage here are kept
            Properties props = system.getUserSettings(user);
            applySettings(props, settings);
            system.setUserSettings(user, props);

            return Response.ok(settings).build();
        } catch (Exception ex) {
            log.error("can not update profile settings", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Sets a new password for the currently authenticated user. No current password is required
     * (matching the desktop). When the server enforces password complexity, a password that is not
     * strong is rejected with 400.
     *
     * @param request the new password
     * @response 200 Password changed
     * @response 400 Missing password or complexity requirement not met
     * @response 401 Not authenticated
     */
    @Override
    @PUT
    @Path("/password")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Sets a new password for the current user")
    public Response changePassword(RestfulPasswordChangeV8 request) {
        try {
            if (request == null || request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                return error(Response.Status.BAD_REQUEST, "missing_password");
            }
            InitialContext ic = new InitialContext();
            SystemManagementLocal system = (SystemManagementLocal) ic.lookup(LOOKUP_SYSMAN);

            boolean complexityRequired = readBoolSetting(system, ServerSettingsKeys.SERVERCONF_SECURITY_FORCE_PASSWORDCOMPLEXITY, true);
            if (complexityRequired && PasswordsUtil.validatePasswordComplexity(request.getNewPassword()) != PasswordsUtil.COMPLEXITY_STRONG) {
                return error(Response.Status.BAD_REQUEST, "password_complexity");
            }

            // caller-scoped: the bean resolves the principal itself
            system.updatePassword(request.getNewPassword());

            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not change password", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns the web dashboard ("Mein Desktop") configuration of the current user — an opaque JSON
     * string owned by the web client (visible widgets + per-widget settings). Empty when nothing has
     * been stored yet, in which case the client applies its defaults.
     *
     * @response 200 The caller's dashboard config
     * @response 401 Not authenticated
     */
    @Override
    @GET
    @Path("/dashboard")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Returns the web dashboard config of the current user", response = RestfulDashboardConfigV8.class)
    public Response getDashboard() {
        try {
            String principalId = securityContext.getUserPrincipal().getName();
            InitialContext ic = new InitialContext();
            SystemManagementLocal system = (SystemManagementLocal) ic.lookup(LOOKUP_SYSMAN);

            AppUserBean user = system.getUser(principalId);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            Properties props = system.getUserSettings(user);
            String config = props.getProperty(UserSettingsKeys.CONF_DESKTOP_WEB_CONFIG, "");
            return Response.ok(new RestfulDashboardConfigV8(config)).build();
        } catch (Exception ex) {
            log.error("can not get dashboard config", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Stores the web dashboard configuration of the current user. The body is treated as an opaque
     * JSON object and persisted verbatim (the schema is owned by the web client). Rejects a
     * non-object or oversized payload with 400.
     *
     * @param request the dashboard config to store
     * @response 200 The stored config
     * @response 400 Malformed or oversized config
     * @response 401 Not authenticated
     */
    @Override
    @PUT
    @Path("/dashboard")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed({"loginRole"})
    @io.swagger.annotations.ApiOperation(value = "Stores the web dashboard config of the current user", response = RestfulDashboardConfigV8.class)
    public Response updateDashboard(RestfulDashboardConfigV8 request) {
        try {
            if (request == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            String config = request.getConfig() == null ? "" : request.getConfig().trim();
            // opaque-JSON sanity guard: allow empty (= clear) or a plain JSON object, bounded in size
            if (!config.isEmpty() && (config.length() > 16384 || !config.startsWith("{") || !config.endsWith("}"))) {
                return error(Response.Status.BAD_REQUEST, "invalid_config");
            }
            String principalId = securityContext.getUserPrincipal().getName();
            InitialContext ic = new InitialContext();
            SystemManagementLocal system = (SystemManagementLocal) ic.lookup(LOOKUP_SYSMAN);

            AppUserBean user = system.getUser(principalId);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            // merge onto the existing blob so unrelated keys are preserved
            Properties props = system.getUserSettings(user);
            props.setProperty(UserSettingsKeys.CONF_DESKTOP_WEB_CONFIG, config);
            system.setUserSettings(user, props);

            return Response.ok(new RestfulDashboardConfigV8(config)).build();
        } catch (Exception ex) {
            log.error("can not update dashboard config", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    // --- helpers ---

    private boolean readBoolSetting(SystemManagementLocal system, String key, boolean defaultValue) {
        ServerSettingsBean s = system.getSetting(key);
        if (s == null || s.getSettingValue() == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(s.getSettingValue().trim());
    }

    /** Reads a boolean per-user setting with the desktop's key semantics ("true"/"false"). */
    private boolean readBool(Properties props, String key, boolean defaultValue) {
        return Boolean.parseBoolean(props.getProperty(key, Boolean.toString(defaultValue)));
    }

    /** Maps the stored settings blob to the DTO, using the same keys and defaults as the desktop. */
    private RestfulProfileSettingsV8 toSettings(Properties props) {
        RestfulProfileSettingsV8 s = new RestfulProfileSettingsV8();
        s.setNotifyCalendarEntry(readBool(props, UserSettingsKeys.NOTIFICATION_EVENT_CALENDARENTRY, true));
        s.setNotifyCalendarEntryAuthored(readBool(props, UserSettingsKeys.NOTIFICATION_EVENT_CALENDARENTRY_AUTHORED, false));
        s.setNotifyCalendarEntryReminder(readBool(props, UserSettingsKeys.NOTIFICATION_EVENT_CALENDARENTRY_REMINDER, true));
        s.setNotifyInstantMessageMention(readBool(props, UserSettingsKeys.NOTIFICATION_EVENT_INSTANTMESSAGEMENTION, true));
        s.setNotifyInstantMessageMentionDone(readBool(props, UserSettingsKeys.NOTIFICATION_EVENT_INSTANTMESSAGEMENTION_DONE, true));
        s.setNotifyInvoiceDue(readBool(props, UserSettingsKeys.NOTIFICATION_EVENT_INVOICE_DUE, true));
        s.setNotifyScheduledDailyAgenda(readBool(props, UserSettingsKeys.NOTIFICATION_SCHEDULED_DAILY_AGENDA, true));
        s.setNotifyScheduledWeeklyDigest(readBool(props, UserSettingsKeys.NOTIFICATION_SCHEDULED_WEEKLY_DIGEST, true));
        s.setWarnUnknownSenders(readBool(props, UserSettingsKeys.CONF_MAIL_WARNSENDERUNKNOWN, true));
        s.setDefaultOwnerGroup(props.getProperty(UserSettingsKeys.CONF_CASE_DEFAULT_OWNERGROUP, ""));
        s.setDefaultAllowedGroups(splitArray(props.getProperty(UserSettingsKeys.CONF_CASE_DEFAULT_ALLOWEDGROUPS, "")));
        return s;
    }

    /** Writes the DTO onto the settings blob, matching the desktop's storage format. */
    private void applySettings(Properties props, RestfulProfileSettingsV8 s) {
        props.setProperty(UserSettingsKeys.NOTIFICATION_EVENT_CALENDARENTRY, Boolean.toString(s.isNotifyCalendarEntry()));
        props.setProperty(UserSettingsKeys.NOTIFICATION_EVENT_CALENDARENTRY_AUTHORED, Boolean.toString(s.isNotifyCalendarEntryAuthored()));
        props.setProperty(UserSettingsKeys.NOTIFICATION_EVENT_CALENDARENTRY_REMINDER, Boolean.toString(s.isNotifyCalendarEntryReminder()));
        props.setProperty(UserSettingsKeys.NOTIFICATION_EVENT_INSTANTMESSAGEMENTION, Boolean.toString(s.isNotifyInstantMessageMention()));
        props.setProperty(UserSettingsKeys.NOTIFICATION_EVENT_INSTANTMESSAGEMENTION_DONE, Boolean.toString(s.isNotifyInstantMessageMentionDone()));
        props.setProperty(UserSettingsKeys.NOTIFICATION_EVENT_INVOICE_DUE, Boolean.toString(s.isNotifyInvoiceDue()));
        props.setProperty(UserSettingsKeys.NOTIFICATION_SCHEDULED_DAILY_AGENDA, Boolean.toString(s.isNotifyScheduledDailyAgenda()));
        props.setProperty(UserSettingsKeys.NOTIFICATION_SCHEDULED_WEEKLY_DIGEST, Boolean.toString(s.isNotifyScheduledWeeklyDigest()));
        props.setProperty(UserSettingsKeys.CONF_MAIL_WARNSENDERUNKNOWN, Boolean.toString(s.isWarnUnknownSenders()));
        props.setProperty(UserSettingsKeys.CONF_CASE_DEFAULT_OWNERGROUP, s.getDefaultOwnerGroup() == null ? "" : s.getDefaultOwnerGroup());
        props.setProperty(UserSettingsKeys.CONF_CASE_DEFAULT_ALLOWEDGROUPS, joinArray(s.getDefaultAllowedGroups()));
    }

    /** Splits a "#####"-delimited array value (the desktop's {@code UserSettings} array format). */
    private List<String> splitArray(String value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            return result;
        }
        for (String part : value.split("#####")) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }

    /** Joins ids into the desktop's "#####"-delimited array value (delimiter after each element). */
    private String joinArray(List<String> values) {
        StringBuilder sb = new StringBuilder();
        if (values != null) {
            for (String v : values) {
                if (v != null && !v.isEmpty()) {
                    sb.append(v).append("#####");
                }
            }
        }
        return sb.toString();
    }

    private Response error(Response.Status status, String code) {
        return Response.status(status)
                .entity("{\"error\":\"" + code + "\"}")
                .type(MediaType.APPLICATION_JSON + ";charset=utf-8")
                .build();
    }
}
