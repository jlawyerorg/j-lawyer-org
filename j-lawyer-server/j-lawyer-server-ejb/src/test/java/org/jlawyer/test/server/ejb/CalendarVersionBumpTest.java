package org.jlawyer.test.server.ejb;

import com.jdimension.jlawyer.persistence.ArchiveFileReviewsBean;
import com.jdimension.jlawyer.persistence.ArchiveFileReviewsBeanFacade;
import com.jdimension.jlawyer.services.SingletonServiceLocal;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.EntityManager;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Every write of a calendar entry changes the calendar version, which the desktop due list and the
 * calendar overview poll. Writes from the REST API (CasesEndpointV6 add / update, CalendarEndpointV8
 * remove) go through CalendarService and thus through ArchiveFileReviewsBeanFacade, like every other
 * service, so checking the facade covers them.
 */
public class CalendarVersionBumpTest {

    private final AtomicInteger bumps = new AtomicInteger();

    private ArchiveFileReviewsBeanFacade facade() throws Exception {
        EntityManager em = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> "merge".equals(method.getName()) ? args[0] : null);
        SingletonServiceLocal singleton = (SingletonServiceLocal) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{SingletonServiceLocal.class},
                (proxy, method, args) -> {
                    if ("bumpCalendarVersion".equals(method.getName())) {
                        return (long) bumps.incrementAndGet();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        ArchiveFileReviewsBeanFacade facade = new ArchiveFileReviewsBeanFacade();
        set(facade, "em", em);
        set(facade, "singletonService", singleton);
        return facade;
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    public void createEditAndRemoveChangeTheVersion() throws Exception {
        ArchiveFileReviewsBeanFacade facade = facade();
        ArchiveFileReviewsBean review = new ArchiveFileReviewsBean();

        facade.create(review);
        assertEquals(1, bumps.get());

        facade.edit(review);
        assertEquals(2, bumps.get());

        facade.remove(review);
        assertEquals(3, bumps.get());
    }
}
