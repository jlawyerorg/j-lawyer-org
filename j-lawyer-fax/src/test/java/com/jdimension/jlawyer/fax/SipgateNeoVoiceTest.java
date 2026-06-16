/*
 * Copyright (C) 2026 j-lawyer.org
 *
 * Unit tests for the sipgate neo (PBX) voice mapping in SipgateAPI.
 * Pure parsing/join logic, no network access.
 */
package com.jdimension.jlawyer.fax;

import com.jdimension.jlawyer.sip.SipUtils;
import java.util.List;
import java.util.Map;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SipgateNeoVoiceTest {

    // device e4 is member of c1 and c2; e9 only of c2.
    private static final String CHANNELS
            = "{\"items\":["
            + "{\"id\":\"c1\",\"name\":\"Channel One\",\"users\":[{\"id\":\"w0\",\"deviceIds\":[\"e4\"]}]},"
            + "{\"id\":\"c2\",\"name\":\"Channel Two\",\"users\":[{\"id\":\"w0\",\"deviceIds\":[\"e4\",\"e9\"]}]}"
            + "]}";

    // top-level array; a block carries routing:null and a nested numbers[] with the real routing.
    private static final String PHONE_NUMBERS
            = "[{\"type\":\"BLOCK\",\"e164Number\":\"+49000\",\"routing\":null,\"numbers\":["
            + "{\"e164Number\":\"+491\",\"routing\":{\"targetType\":\"USER\",\"targetId\":\"c1\"}},"
            + "{\"e164Number\":\"+492\",\"routing\":{\"targetType\":\"ACD\",\"targetId\":\"c2\"}},"
            + "{\"e164Number\":\"+493\",\"routing\":null}"
            + "]}]";

    // e4 = REGISTER phone, y2 = mobile (skip), e9 = desktop app.
    private static final String DEVICES
            = "{\"items\":["
            + "{\"id\":\"e4\",\"alias\":\"Phone A\",\"type\":\"REGISTER\",\"owner\":\"w0\"},"
            + "{\"id\":\"y2\",\"alias\":\"Mobile\",\"type\":\"MOBILE\",\"owner\":\"w0\"},"
            + "{\"id\":\"e9\",\"alias\":\"App\",\"type\":\"DESKTOP_APP\",\"owner\":\"w0\"}"
            + "]}";

    @Test
    public void parseChannelNumbers_flattensNestedBlocksRecursively() throws Exception {
        Map<String, List<String>> result = SipgateAPI.parseChannelNumbers(PHONE_NUMBERS);

        assertTrue("number from nested block must be resolved to its channel",
                result.get("c1").contains("+491"));
    }

    @Test
    public void parseChannelNumbers_countsUserAndAcdRouting() throws Exception {
        Map<String, List<String>> result = SipgateAPI.parseChannelNumbers(PHONE_NUMBERS);

        assertTrue("USER routing must map to a channel", result.containsKey("c1"));
        assertTrue("ACD routing must map to a channel", result.containsKey("c2"));
    }

    @Test
    public void parseChannelNumbers_ignoresNumbersWithoutRouting() throws Exception {
        Map<String, List<String>> result = SipgateAPI.parseChannelNumbers(PHONE_NUMBERS);

        assertEquals("only routed numbers must be present", 2, result.size());
    }

    @Test
    public void parseDeviceChannels_mapsDeviceToAllItsChannels() throws Exception {
        Map<String, List<String>> result = SipgateAPI.parseDeviceChannels(CHANNELS);

        List<String> channelsOfE4 = result.get("e4");
        assertTrue("e4 must be in c1", channelsOfE4.contains("c1"));
        assertTrue("e4 must be in c2", channelsOfE4.contains("c2"));
    }

    @Test
    public void buildNeoVoiceUris_createsOneUriPerDeviceNumberCombination() throws Exception {
        List<SipUri> uris = SipgateAPI.buildNeoVoiceUris(CHANNELS, PHONE_NUMBERS, DEVICES);

        long e4Count = uris.stream().filter(u -> "e4".equals(u.getUri())).count();
        assertEquals("e4 in c1+c2 must yield one entry per (device, number)", 2, e4Count);
    }

    @Test
    public void buildNeoVoiceUris_setsVoiceServiceAndCallerNumberAndAlias() throws Exception {
        List<SipUri> uris = SipgateAPI.buildNeoVoiceUris(CHANNELS, PHONE_NUMBERS, DEVICES);

        SipUri e4 = uris.stream().filter(u -> "e4".equals(u.getUri())).findFirst().get();
        assertTrue("voice URI must carry TOS_VOICE", e4.getTypeOfService().contains(SipUtils.TOS_VOICE));
        assertTrue("outgoing number must be one of the channel numbers",
                e4.getOutgoingNumber().equals("+491") || e4.getOutgoingNumber().equals("+492"));
        assertEquals("description must be the device alias", "Phone A", e4.getDescription());
    }

    @Test
    public void buildNeoVoiceUris_skipsMobileDevices() throws Exception {
        List<SipUri> uris = SipgateAPI.buildNeoVoiceUris(CHANNELS, PHONE_NUMBERS, DEVICES);

        boolean hasMobile = uris.stream().anyMatch(u -> u.getUri() != null && u.getUri().startsWith("y"));
        assertFalse("mobile (yN) devices must not appear as voice URIs", hasMobile);
    }

    @Test
    public void buildNeoVoiceUris_includesDeviceWithoutChannelAndWithoutNumber() throws Exception {
        String channels = "{\"items\":[]}";
        String devices = "{\"items\":[{\"id\":\"e5\",\"alias\":\"Lonely\",\"type\":\"REGISTER\",\"owner\":\"w0\"}]}";

        List<SipUri> uris = SipgateAPI.buildNeoVoiceUris(channels, "[]", devices);

        assertEquals("device without channel still appears once", 1, uris.size());
        assertNull("device without channel has no outgoing number", uris.get(0).getOutgoingNumber());
        assertTrue("still a voice URI", uris.get(0).getTypeOfService().contains(SipUtils.TOS_VOICE));
    }

    @Test
    public void buildNeoCallBody_stripsLeadingPlusFromTargetAndCallerId() throws Exception {
        String body = SipgateAPI.buildNeoCallBody("e4", "+4915799912345", "+4928559388690");

        JsonObject json = (JsonObject) Jsoner.deserialize(body);
        assertEquals("e4", json.getString("deviceId"));
        assertEquals("POST /v2/calls expects E.164 without leading +",
                "4915799912345", json.getString("targetNumber"));
        assertEquals("callerId must also drop the leading +",
                "4928559388690", json.getString("callerId"));
    }

    @Test
    public void buildNeoCallBody_omitsCallerIdWhenEmpty() throws Exception {
        String body = SipgateAPI.buildNeoCallBody("e4", "+4915799912345", "");

        JsonObject json = (JsonObject) Jsoner.deserialize(body);
        assertFalse("callerId must be omitted when empty", json.containsKey("callerId"));
    }
}
