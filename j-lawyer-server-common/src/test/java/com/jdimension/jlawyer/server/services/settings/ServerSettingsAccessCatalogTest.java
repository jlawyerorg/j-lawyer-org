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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the access rules of server setting keys.
 *
 * @author jens
 */
public class ServerSettingsAccessCatalogTest {

    @Test
    public void everyKeyConstantIsAnnotated() {
        List<String> missing = new ArrayList<>();
        for (Field f : ServerSettingsAccessCatalog.getKeyConstants()) {
            if (f.getAnnotation(SettingAccess.class) == null) {
                missing.add(f.getName());
            }
        }
        Assert.assertTrue("Settings-Keys ohne @SettingAccess: " + missing, missing.isEmpty());
    }

    @Test
    public void noDuplicateKeys() throws Exception {
        Map<String, String> seen = new HashMap<>();
        List<String> duplicates = new ArrayList<>();
        for (Field f : ServerSettingsAccessCatalog.getKeyConstants()) {
            String key = (String) f.get(null);
            String previous = seen.put(key, f.getName());
            if (previous != null) {
                duplicates.add(previous + "/" + f.getName() + "=" + key);
            }
        }
        Assert.assertTrue("doppelte Settings-Keys: " + duplicates, duplicates.isEmpty());
    }

    @Test
    public void exactKeyIsResolved() {
        ServerSettingsAccessCatalog.Rule r = ServerSettingsAccessCatalog.resolve(ServerSettingsKeys.SERVERCONF_BACKUP_DBPWD);
        Assert.assertEquals(SettingRole.SYSADMIN, r.getRead());
        Assert.assertEquals(SettingRole.SYSADMIN, r.getWrite());
        Assert.assertTrue(r.isSecret());
        Assert.assertFalse(r.isPrefix());
    }

    @Test
    public void prefixKeyIsResolved() {
        ServerSettingsAccessCatalog.Rule r = ServerSettingsAccessCatalog.resolve(ServerSettingsKeys.DATA_CUSTOMFIELD_ADDRESS_PREFIX + "2");
        Assert.assertEquals(SettingRole.LOGIN, r.getRead());
        Assert.assertEquals(SettingRole.ADMIN, r.getWrite());
        Assert.assertTrue(r.isPrefix());
    }

    @Test
    public void longestPrefixWins() {
        // data.customfields.archivefile.involved. is longer than data.customfields.archivefile.
        ServerSettingsAccessCatalog.Rule r = ServerSettingsAccessCatalog.resolve(ServerSettingsKeys.DATA_CUSTOMFIELD_ARCHIVEFILE_INVOLVED_PREFIX + "1");
        Assert.assertEquals(ServerSettingsKeys.DATA_CUSTOMFIELD_ARCHIVEFILE_INVOLVED_PREFIX, r.getKey());
    }

    @Test
    public void unknownKeyGetsDefault() {
        ServerSettingsAccessCatalog.Rule r = ServerSettingsAccessCatalog.resolve("plugins.someform.somesetting");
        Assert.assertSame(ServerSettingsAccessCatalog.DEFAULT_RULE, r);
        Assert.assertEquals(SettingRole.LOGIN, r.getRead());
        Assert.assertEquals(SettingRole.LOGIN, r.getWrite());
        Assert.assertFalse(r.isSecret());
        Assert.assertSame(ServerSettingsAccessCatalog.DEFAULT_RULE, ServerSettingsAccessCatalog.resolve(null));
    }

    @Test
    public void passwordKeysAreSecrets() {
        String[] secrets = {
            ServerSettingsKeys.SERVERCONF_BACKUP_DBPWD,
            ServerSettingsKeys.SERVERCONF_BACKUP_ENCRYPTPWD,
            ServerSettingsKeys.SERVERCONF_MONITOR_SMTPPASSWORD,
            ServerSettingsKeys.SERVERCONF_CLOUDSYNC_ADDRESSBOOK_PWD,
            ServerSettingsKeys.SERVERCONF_EPOSTVENDORID_ENCRYPTED,
            ServerSettingsKeys.SERVERCONF_OFFICE_SECRET
        };
        for (String key : secrets) {
            Assert.assertTrue(key + " muss Secret sein", ServerSettingsAccessCatalog.isSecret(key));
        }
    }

    @Test
    public void serverExecutedSettingsRequireSysAdmin() {
        String[] keys = {
            ServerSettingsKeys.SERVERCONF_SCANNER_OCRCMD,
            ServerSettingsKeys.SERVERCONF_SCANNER_SERVERDIR,
            ServerSettingsKeys.SERVERCONF_STIRLINGPDF_ENDPOINT,
            ServerSettingsKeys.SERVERCONF_BEAENDPOINT,
            ServerSettingsKeys.SERVERCONF_BACKUP_SYNCTARGET
        };
        for (String key : keys) {
            Assert.assertEquals(key, SettingRole.SYSADMIN, ServerSettingsAccessCatalog.resolve(key).getWrite());
        }
    }

    @Test
    public void systemKeysAreNotWritable() {
        Assert.assertEquals(SettingRole.SYSTEM, ServerSettingsAccessCatalog.resolve(ServerSettingsKeys.SERVERCONF_USAGELIMIT_MAXUSERS).getWrite());
        Assert.assertEquals(SettingRole.SYSTEM, ServerSettingsAccessCatalog.resolve(ServerSettingsKeys.SERVERCONF_INSTALLATION_ID).getWrite());
        Assert.assertEquals(SettingRole.SYSTEM, ServerSettingsAccessCatalog.resolve(ServerSettingsKeys.EDA_PREFIX + "XYZ.lastused").getRead());
    }
}
