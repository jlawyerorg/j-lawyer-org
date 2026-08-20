/*
 * Copyright (C) j-lawyer.org
 *
 * Licensed under the GNU Affero General Public License, version 3.
 * See the LICENSE file distributed with this project.
 */
package org.jlawyer.io.rest.v7.pojo;

/**
 * Request body for hiding/unhiding a mail folder: {@code hidden=true} hides it, {@code false} unhides it.
 */
public class RestfulSetHiddenRequestV7 {

    private boolean hidden;

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
