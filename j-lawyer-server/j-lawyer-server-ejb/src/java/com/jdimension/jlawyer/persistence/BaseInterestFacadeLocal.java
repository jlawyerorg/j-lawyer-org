/*
                    GNU AFFERO GENERAL PUBLIC LICENSE
                       Version 3, 19 November 2007

 Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.jdimension.jlawyer.persistence;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import javax.ejb.Local;

/**
 * Local EJB interface for BaseInterest entity operations.
 *
 * @author jens
 */
@Local
public interface BaseInterestFacadeLocal {

    void create(BaseInterest baseInterest);

    void edit(BaseInterest baseInterest);

    void remove(BaseInterest baseInterest);

    BaseInterest find(Object id);

    List<BaseInterest> findAll();

    List<BaseInterest> findRange(int[] range);

    int count();

    /**
     * Finds the applicable base interest rate for a given date.
     * Returns the rate valid on or before the specified date (the most recent rate).
     *
     * @param date The date for which to find the base interest rate
     * @return The base interest rate as BigDecimal (percentage, e.g., 3.62), or null if none found
     */
    BigDecimal findRateByDate(Date date);

    /**
     * Finds all base interest rate entries with validFrom dates between the given dates (inclusive).
     *
     * @param fromDate The start date (inclusive)
     * @param toDate The end date (inclusive)
     * @return List of BaseInterest entries in the date range, ordered by validFrom ascending
     */
    List<BaseInterest> findByDateRange(Date fromDate, Date toDate);

    /**
     * Removes all base interest rate entries from the database.
     */
    void removeAll();

}
