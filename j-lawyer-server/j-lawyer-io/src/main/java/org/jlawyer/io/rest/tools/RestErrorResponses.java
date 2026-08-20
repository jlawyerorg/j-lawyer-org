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
package org.jlawyer.io.rest.tools;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Builds the uniform error payload that REST endpoints return on failure. The message is passed
 * through <b>unmodified</b> (including the cause chain): the REST layer sits on the same EJB layer as
 * the Swing client, which already surfaces these exception messages to authenticated users — so
 * returning them to REST clients is no additional disclosure, just parity.
 *
 * <p>The status stays a genuine HTTP error (500), so status-code-driven clients (the generated
 * mobile Dio client, the Angular {@code HttpClient}, curl, RESTEasy proxies) keep treating the call
 * as failed and never deserialize this body into a success type — the payload is purely additive and
 * backward compatible. It is intentionally shaped differently from any 2xx response:
 * {@code {"status":500,"error":"<ExceptionType>","message":"<message + cause chain>"}}.</p>
 */
public final class RestErrorResponses {

    private static final String JSON = MediaType.APPLICATION_JSON + ";charset=utf-8";
    private static final int MAX_CAUSES = 10;

    private RestErrorResponses() {
    }

    /** A 500 response carrying the (unsanitized) exception detail as a JSON error envelope. */
    public static Response serverError(Throwable t) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(body(t))
                .type(JSON)
                .build();
    }

    /** The JSON error envelope for the given throwable, with its full cause chain. */
    public static String body(Throwable t) {
        if (t == null) {
            return "{\"status\":500,\"error\":\"Error\",\"message\":\"\"}";
        }
        String type = t.getClass().getSimpleName();
        StringBuilder msg = new StringBuilder();
        Throwable c = t;
        int guard = 0;
        while (c != null && guard++ < MAX_CAUSES) {
            if (msg.length() > 0) {
                msg.append(" | caused by ");
            }
            msg.append(c.getClass().getSimpleName());
            if (c.getMessage() != null) {
                msg.append(": ").append(c.getMessage());
            }
            c = c.getCause();
        }
        return "{\"status\":500,\"error\":\"" + esc(type) + "\",\"message\":\"" + esc(msg.toString()) + "\"}";
    }

    /** Minimal JSON string escaping (quotes, backslash, control characters). */
    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }
}
