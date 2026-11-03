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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes who may read and write a server setting key constant in {@link ServerSettingsKeys}
 * through the generic settings API. Read by {@link ServerSettingsAccessCatalog}.
 *
 * @author jens
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SettingAccess {

    /**
     * @return the role required to read the setting
     */
    SettingRole read();

    /**
     * @return the role required to write the setting
     */
    SettingRole write();

    /**
     * @return true if the value is a secret (password, token, key). Secrets are write-only: the
     * generic API never returns their value, only whether a value is set.
     */
    boolean secret() default false;

    /**
     * @return true if the constant is a key prefix; the access rules then apply to every key
     * starting with it
     */
    boolean prefix() default false;
}
