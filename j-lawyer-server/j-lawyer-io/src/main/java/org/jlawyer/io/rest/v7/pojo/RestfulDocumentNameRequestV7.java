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
package org.jlawyer.io.rest.v7.pojo;

/**
 * Request to compute a document file name for a case from a source file name, a
 * document date and a document name template.
 */
public class RestfulDocumentNameRequestV7 {

    protected String fileName = null;
    protected long date = 0L;
    protected String template = null;

    public RestfulDocumentNameRequestV7() {
    }

    /**
     * @return the original file name whose extension is preserved
     */
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the document date as epoch milliseconds; 0 means "use now"
     */
    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    /**
     * @return the id of the document name template to apply; empty/null means
     * "use the default template"
     */
    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

}
