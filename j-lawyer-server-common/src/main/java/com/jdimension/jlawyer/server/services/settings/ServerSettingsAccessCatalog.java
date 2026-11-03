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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the access rules of server setting keys from the {@link SettingAccess} annotations on
 * the constants in {@link ServerSettingsKeys}. A key is resolved by exact match first, then by the
 * longest matching prefix constant; keys without rules can be read and written by any
 * authenticated user.
 *
 * @author jens
 */
public final class ServerSettingsAccessCatalog {

    /** access rules for keys without an annotated constant */
    public static final Rule DEFAULT_RULE = new Rule(null, SettingRole.LOGIN, SettingRole.LOGIN, false, false);

    private static Map<String, Rule> exactRules = null;
    private static List<Rule> prefixRules = null;

    private ServerSettingsAccessCatalog() {
    }

    /**
     * @param key a server setting key
     * @return the access rules for the key, never null
     */
    public static Rule resolve(String key) {
        if (key == null) {
            return DEFAULT_RULE;
        }
        init();
        Rule exact = exactRules.get(key);
        if (exact != null) {
            return exact;
        }
        // prefixRules is sorted by descending prefix length, so the first match is the longest
        for (Rule r : prefixRules) {
            if (key.startsWith(r.getKey())) {
                return r;
            }
        }
        return DEFAULT_RULE;
    }

    /**
     * @param key a server setting key
     * @return true if the key holds a secret
     */
    public static boolean isSecret(String key) {
        return resolve(key).isSecret();
    }

    private static synchronized void init() {
        if (exactRules != null) {
            return;
        }
        Map<String, Rule> exact = new HashMap<>();
        List<Rule> prefixes = new ArrayList<>();
        for (Field f : getKeyConstants()) {
            SettingAccess access = f.getAnnotation(SettingAccess.class);
            if (access == null) {
                continue;
            }
            String key;
            try {
                key = (String) f.get(null);
            } catch (IllegalAccessException ex) {
                continue;
            }
            Rule r = new Rule(key, access.read(), access.write(), access.secret(), access.prefix());
            if (access.prefix()) {
                prefixes.add(r);
            } else {
                exact.put(key, r);
            }
        }
        prefixes.sort((r1, r2) -> Integer.compare(r2.getKey().length(), r1.getKey().length()));
        prefixRules = Collections.unmodifiableList(prefixes);
        exactRules = Collections.unmodifiableMap(exact);
    }

    /**
     * @return all key constants (public static final String fields) declared in
     * {@link ServerSettingsKeys}
     */
    static List<Field> getKeyConstants() {
        List<Field> fields = new ArrayList<>();
        for (Field f : ServerSettingsKeys.class.getDeclaredFields()) {
            int mod = f.getModifiers();
            if (Modifier.isPublic(mod) && Modifier.isStatic(mod) && Modifier.isFinal(mod) && f.getType() == String.class) {
                fields.add(f);
            }
        }
        return fields;
    }

    /**
     * Access rules of a setting key or key prefix.
     */
    public static final class Rule {

        private final String key;
        private final SettingRole read;
        private final SettingRole write;
        private final boolean secret;
        private final boolean prefix;

        Rule(String key, SettingRole read, SettingRole write, boolean secret, boolean prefix) {
            this.key = key;
            this.read = read;
            this.write = write;
            this.secret = secret;
            this.prefix = prefix;
        }

        /**
         * @return the key or key prefix, null for the default rule
         */
        public String getKey() {
            return key;
        }

        /**
         * @return the role required to read
         */
        public SettingRole getRead() {
            return read;
        }

        /**
         * @return the role required to write
         */
        public SettingRole getWrite() {
            return write;
        }

        /**
         * @return true if the value is a secret
         */
        public boolean isSecret() {
            return secret;
        }

        /**
         * @return true if the rule applies to a key prefix
         */
        public boolean isPrefix() {
            return prefix;
        }
    }
}
