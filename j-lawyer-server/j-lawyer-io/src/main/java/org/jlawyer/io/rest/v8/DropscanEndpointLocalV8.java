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
package org.jlawyer.io.rest.v8;

import javax.ejb.Local;
import javax.ws.rs.core.Response;

@Local
public interface DropscanEndpointLocalV8 {

    Response getScanboxes();

    Response getMailings(String scanboxId, String status);

    Response getMailingPdf(String scanboxId, String uuid);

    Response getEnvelopeImage(String scanboxId, String uuid);

    Response requestScan(String scanboxId, String uuid);

    Response requestDestroy(String scanboxId, String uuid);

    Response caseSuggestions(String scanboxId, String uuid);

    Response discoverScanboxes(org.jlawyer.io.rest.v8.pojo.RestfulDropscanDiscoverRequestV8 request);

}
