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

import com.jdimension.jlawyer.ai.AiRequestLog;

/**
 * One entry of an assistant server's request history (RestfulAiRequestLogV8): when a request ran,
 * of which type, and how many tokens it consumed. The timestamp is epoch milliseconds.
 */
public class RestfulAiRequestLogV8 {

    private long timestamp;
    private String requestType;
    private int tokensUsed;

    public RestfulAiRequestLogV8() {
    }

    public static RestfulAiRequestLogV8 fromLog(AiRequestLog l) {
        RestfulAiRequestLogV8 dto = new RestfulAiRequestLogV8();
        dto.timestamp = l.getTimestamp() != null ? l.getTimestamp().getTime() : 0L;
        dto.requestType = l.getRequestType();
        dto.tokensUsed = l.getTokensUsed();
        return dto;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public int getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(int tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

}
