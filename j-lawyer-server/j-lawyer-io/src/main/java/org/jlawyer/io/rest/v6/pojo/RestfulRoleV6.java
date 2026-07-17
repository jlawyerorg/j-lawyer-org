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
        ROLE_DESCRIPTIONS.put("loginRole", "Anmeldung am System");
        ROLE_DESCRIPTIONS.put("adminRole", "Administration (Nutzer & Einstellungen)");
        ROLE_DESCRIPTIONS.put("sysAdminRole", "Systemadministration (inkl. anderer Administratoren)");
        ROLE_DESCRIPTIONS.put("importRole", "Datenimport");
        ROLE_DESCRIPTIONS.put("createAddressRole", "Kontakte anlegen");
        ROLE_DESCRIPTIONS.put("readAddressRole", "Kontakte einsehen");
        ROLE_DESCRIPTIONS.put("writeAddressRole", "Kontakte bearbeiten");
        ROLE_DESCRIPTIONS.put("removeAddressRole", "Kontakte löschen");
        ROLE_DESCRIPTIONS.put("createArchiveFileRole", "Akten anlegen");
        ROLE_DESCRIPTIONS.put("readArchiveFileRole", "Akten einsehen");
        ROLE_DESCRIPTIONS.put("writeArchiveFileRole", "Akten bearbeiten");
        ROLE_DESCRIPTIONS.put("removeArchiveFileRole", "Akten löschen");
        ROLE_DESCRIPTIONS.put("createOptionGroupRole", "Wertelisten anlegen");
        ROLE_DESCRIPTIONS.put("writeOptionGroupRole", "Wertelisten bearbeiten");
        ROLE_DESCRIPTIONS.put("deleteOptionGroupRole", "Wertelisten löschen");
        ROLE_DESCRIPTIONS.put("commonReportRole", "Allgemeine Auswertungen");
        ROLE_DESCRIPTIONS.put("confidentialReportRole", "Vertrauliche Auswertungen");
        ROLE_DESCRIPTIONS.put("aiAgentRole", "KI-Agentenfunktionen");
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
