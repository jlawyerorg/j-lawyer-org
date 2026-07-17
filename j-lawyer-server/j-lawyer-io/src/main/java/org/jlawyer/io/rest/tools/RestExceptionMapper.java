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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Safety net that turns any exception which escapes a REST resource method into the uniform error
 * envelope (see {@link RestErrorResponses}) instead of a container HTML error page — so REST clients
 * always receive the exception detail on a genuine HTTP error status.
 *
 * <p>Endpoints that catch and convert their own exceptions (via {@link RestErrorResponses}) are
 * unaffected; this only handles the uncaught cases (e.g. response-serialization failures). A
 * {@link WebApplicationException} keeps its intended status/response (routing 404/405, security
 * 401/403, explicit statuses) untouched; everything else becomes a 500 with the envelope.</p>
 */
@Provider
public class RestExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger log = Logger.getLogger(RestExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable t) {
        if (t instanceof WebApplicationException) {
            // preserve routing/security/explicit responses as-is
            return ((WebApplicationException) t).getResponse();
        }
        log.error("unhandled REST exception", t);
        return RestErrorResponses.serverError(t);
    }
}
