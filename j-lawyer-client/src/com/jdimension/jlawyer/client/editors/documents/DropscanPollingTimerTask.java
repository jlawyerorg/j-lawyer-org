/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.editors.documents;

import com.jdimension.jlawyer.client.events.DropscanStatusEvent;
import com.jdimension.jlawyer.client.events.EventBroker;
import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.dropscan.DropscanMailing;
import com.jdimension.jlawyer.services.DropscanServiceRemote;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 *
 * @author jens
 */
public class DropscanPollingTimerTask extends java.util.TimerTask {

    private static final Logger log = Logger.getLogger(DropscanPollingTimerTask.class.getName());
    private static Set<String> lastMailingUuids = new HashSet<>();

    private volatile boolean stopped = false;

    public void stop() {
        stopped = true;
    }

    @Override
    public void run() {
        if (stopped) return;
        synchronized (this) {
            try {
                ClientSettings settings = ClientSettings.getInstance();
                JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());

                if (stopped) return;
                DropscanServiceRemote ds = locator.lookupDropscanServiceRemote();
                List<DropscanMailing> mailings = ds.getAllMailings(null);
                if (mailings == null) {
                    mailings = new ArrayList<>();
                }

                if (stopped) return;
                Set<String> currentUuids = new HashSet<>();
                for (DropscanMailing m : mailings) {
                    currentUuids.add(m.getUuid());
                }

                if (!currentUuids.equals(lastMailingUuids)) {
                    if (stopped) return;
                    EventBroker eb = EventBroker.getInstance();
                    eb.publishEvent(new DropscanStatusEvent(mailings));
                }
                lastMailingUuids = currentUuids;

            } catch (Throwable ex) {
                log.debug("Dropscan polling skipped", ex);
            }
        }
    }
}
