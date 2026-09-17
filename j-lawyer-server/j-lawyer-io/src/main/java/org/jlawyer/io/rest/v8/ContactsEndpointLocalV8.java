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
import org.jlawyer.io.rest.v8.pojo.RestfulContactRelationRequestV8;

/**
 * Local business interface of the v8 contacts endpoint (richer, server-paginated overview than
 * v1, plus the relationships of a contact).
 *
 * @author jens
 */
@Local
public interface ContactsEndpointLocalV8 {

    Response listPage(int offset, int limit, String filter, String q);

    Response getRelations(String id);

    Response createRelation(String id, RestfulContactRelationRequestV8 body);

    Response updateRelation(String id, String relationId, RestfulContactRelationRequestV8 body);

    Response deleteRelation(String id, String relationId);

}
