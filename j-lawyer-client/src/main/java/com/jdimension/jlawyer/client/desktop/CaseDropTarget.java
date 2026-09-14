/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.desktop;

/**
 * A desktop entry referring to a case, onto which files can be dropped to
 * upload them as documents of that case.
 *
 * @see CaseEntryDropHandler
 */
public interface CaseDropTarget {

    /**
     * @return id of the case the entry refers to, or null if there is none
     */
    String getDropCaseId();

    /**
     * @return false if documents must not be added to the case (e.g. it is
     * archived)
     */
    boolean isDropAllowed();

    /**
     * Shows or hides the drop indicator while files are dragged over the
     * entry.
     *
     * @param visible true to show the indicator
     */
    void setDropIndicatorVisible(boolean visible);
}
