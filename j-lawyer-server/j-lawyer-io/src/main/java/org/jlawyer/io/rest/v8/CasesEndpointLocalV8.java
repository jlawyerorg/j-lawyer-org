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
import org.jlawyer.io.rest.v8.pojo.RestfulCaseLinkRequestV8;
import org.jlawyer.io.rest.v8.pojo.RestfulDocumentContentUpdateV8;
import org.jlawyer.io.rest.v8.pojo.RestfulDocumentMetadataPatchV8;
import org.jlawyer.io.rest.v8.pojo.RestfulDocumentMetadataV8;
import org.jlawyer.io.rest.v8.pojo.RestfulDocumentParentV8;

/**
 * Local business interface of the v8 cases list endpoint (richer overview than v1).
 *
 * @author jens
 */
@Local
public interface CasesEndpointLocalV8 {

    Response listCases();

    Response listActiveCases();

    Response listPage(int offset, int limit, String filter, String q);

    Response getHistory(String id);

    Response getCaseLinks(String id);

    Response createCaseLink(String id, RestfulCaseLinkRequestV8 body);

    Response updateCaseLink(String id, String linkId, RestfulCaseLinkRequestV8 body);

    Response deleteCaseLink(String id, String linkId);

    Response getCasesByTag(String tag, String value);

    Response getDocumentsByTag(String tag, String value);

    Response updateDocumentContent(String id, RestfulDocumentContentUpdateV8 body);

    Response getDocumentPreviewPdf(String id);

    Response getDocumentEmlPreview(String id);

    Response getDocumentBeaPreview(String id);

    Response getCaseDocuments(String id);

    Response getDocumentWithMetadata(String id);

    Response updateDocumentMetadata(String id, RestfulDocumentMetadataV8 body);

    Response updateDocumentsMetadata(RestfulDocumentMetadataPatchV8 body);

    Response setDocumentParent(String id, RestfulDocumentParentV8 body);

    Response getDocumentKeywordsForCase(String id);

    Response getDocumentMessages(String id);

    Response resolveDocumentCorrespondent(String id, String keyType, String key, String name, int direction);

}
