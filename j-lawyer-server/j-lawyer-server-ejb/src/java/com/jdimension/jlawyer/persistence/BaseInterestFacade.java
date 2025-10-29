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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Facade for BaseInterest entity, providing CRUD operations and
 * date-based base interest rate lookups.
 *
 * @author jens
 */
@Stateless
public class BaseInterestFacade extends AbstractFacade<BaseInterest> implements BaseInterestFacadeLocal {

    @PersistenceContext(unitName = "j-lawyer-server-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public BaseInterestFacade() {
        super(BaseInterest.class);
    }

    @Override
    public BigDecimal findRateByDate(Date date) {
        try {
            List<BaseInterest> results = em.createNamedQuery("BaseInterest.findByDate")
                    .setParameter("searchDate", date)
                    .setMaxResults(1)
                    .getResultList();

            if (results != null && !results.isEmpty()) {
                return results.get(0).getRate();
            }

            return null;
        } catch (Exception ex) {
            // Log exception if needed
            return null;
        }
    }

    @Override
    public List<BaseInterest> findByDateRange(Date fromDate, Date toDate) {
        try {
            return em.createQuery(
                    "SELECT b FROM BaseInterest b WHERE b.validFrom >= :fromDate AND b.validFrom <= :toDate ORDER BY b.validFrom ASC",
                    BaseInterest.class)
                    .setParameter("fromDate", fromDate)
                    .setParameter("toDate", toDate)
                    .getResultList();
        } catch (Exception ex) {
            // Log exception if needed
            return new ArrayList<>();
        }
    }

    @Override
    public void removeAll() {
        em.createQuery("DELETE FROM BaseInterest").executeUpdate();
    }

}
