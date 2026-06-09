package org.jlawyer.io.rest.v6.pojo;

import com.jdimension.jlawyer.persistence.AppRoleBean;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jens
 */
public class RestfulRoleV6 {

    private static final Map<String, String> ROLE_DESCRIPTIONS = new HashMap<>();

    static {
        ROLE_DESCRIPTIONS.put("loginRole", "Permission to log in to the system");
        ROLE_DESCRIPTIONS.put("adminRole", "Administrative privileges for user and system management");
        ROLE_DESCRIPTIONS.put("sysAdminRole", "System administrator privileges, can manage other administrators");
        ROLE_DESCRIPTIONS.put("importRole", "Permission to import data");
        ROLE_DESCRIPTIONS.put("createAddressRole", "Permission to create contacts");
        ROLE_DESCRIPTIONS.put("readAddressRole", "Permission to read contacts");
        ROLE_DESCRIPTIONS.put("writeAddressRole", "Permission to modify contacts");
        ROLE_DESCRIPTIONS.put("removeAddressRole", "Permission to delete contacts");
        ROLE_DESCRIPTIONS.put("createArchiveFileRole", "Permission to create cases");
        ROLE_DESCRIPTIONS.put("readArchiveFileRole", "Permission to read cases");
        ROLE_DESCRIPTIONS.put("writeArchiveFileRole", "Permission to modify cases");
        ROLE_DESCRIPTIONS.put("removeArchiveFileRole", "Permission to delete cases");
        ROLE_DESCRIPTIONS.put("createOptionGroupRole", "Permission to create option groups");
        ROLE_DESCRIPTIONS.put("writeOptionGroupRole", "Permission to modify option groups");
        ROLE_DESCRIPTIONS.put("deleteOptionGroupRole", "Permission to delete option groups");
        ROLE_DESCRIPTIONS.put("commonReportRole", "Permission to access common reports");
        ROLE_DESCRIPTIONS.put("confidentialReportRole", "Permission to access confidential reports");
        ROLE_DESCRIPTIONS.put("aiAgentRole", "Permission to use AI agent integrations");
    }

    private String role;
    private String description;

    public RestfulRoleV6() {
    }

    public static RestfulRoleV6 fromAppRoleBean(AppRoleBean arb) {
        RestfulRoleV6 r = new RestfulRoleV6();
        r.setRole(arb.getRole());
        r.setDescription(ROLE_DESCRIPTIONS.getOrDefault(arb.getRole(), ""));
        return r;
    }

    public static RestfulRoleV6 fromRoleName(String roleName) {
        RestfulRoleV6 r = new RestfulRoleV6();
        r.setRole(roleName);
        r.setDescription(ROLE_DESCRIPTIONS.getOrDefault(roleName, ""));
        return r;
    }

    public static Map<String, String> getAllRoleDescriptions() {
        return ROLE_DESCRIPTIONS;
    }

    /**
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * @param role the role to set
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

}
