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
package com.jdimension.jlawyer.server.services.settings;

/**
 * Role required to read or write a server setting through the generic settings API
 * (SystemManagement.getSetting / setSetting / hasSetting).
 *
 * @author jens
 */
public enum SettingRole {

    /** any authenticated user (loginRole) */
    LOGIN,
    /** administrators (adminRole or sysAdminRole) */
    ADMIN,
    /** system administrators (sysAdminRole) */
    SYSADMIN,
    /** nobody - the setting is maintained by the server only and not reachable through the generic API */
    SYSTEM
}
