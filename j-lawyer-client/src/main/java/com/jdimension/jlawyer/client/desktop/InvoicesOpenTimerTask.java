/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.desktop;

import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.persistence.Invoice;
import com.jdimension.jlawyer.persistence.InvoiceStatusSummary;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import java.math.BigDecimal;
import java.util.List;
import java.util.TimerTask;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;

/**
 * Timer task that fetches open invoice summary data from the server and updates
 * the InvoicesOpenPanel with aggregated statistics (counts and totals) for
 * invoices not yet due and overdue invoices.
 *
 * @author jens
 */
public class InvoicesOpenTimerTask extends TimerTask {

    private static final Logger log = Logger.getLogger(InvoicesOpenTimerTask.class.getName());

    private final InvoicesOpenPanel panel;
    private volatile boolean stopped = false;

    public InvoicesOpenTimerTask(InvoicesOpenPanel panel) {
        this.panel = panel;
    }

    public void stop() {
        this.stopped = true;
    }

    @Override
    public void run() {
        if (stopped) {
            return;
        }

        try {
            ClientSettings settings = ClientSettings.getInstance();
            JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());

            List<InvoiceStatusSummary> summaries = locator.lookupInvoiceServiceRemote().getInvoicesSummaryByStatus(
                    Invoice.STATUS_OPEN,
                    Invoice.STATUS_OPEN_REMINDER1,
                    Invoice.STATUS_OPEN_REMINDER2,
                    Invoice.STATUS_OPEN_REMINDER3,
                    Invoice.STATUS_OPEN_NONENFORCEABLE);

            long notDueCount = 0;
            BigDecimal notDueTotal = BigDecimal.ZERO;
            long overdueCount = 0;
            BigDecimal overdueTotal = BigDecimal.ZERO;

            for (InvoiceStatusSummary s : summaries) {
                if (s.getStatus() == Invoice.STATUS_OPEN) {
                    // Not-yet-due portion of STATUS_OPEN
                    notDueCount += s.getCount();
                    notDueTotal = notDueTotal.add(s.getTotalGross());
                } else {
                    // Overdue: negative STATUS_OPEN marker from server, plus REMINDER1-3, NONENFORCEABLE
                    overdueCount += s.getCount();
                    overdueTotal = overdueTotal.add(s.getTotalGross());
                }
            }

            final long fNotDueCount = notDueCount;
            final BigDecimal fNotDueTotal = notDueTotal;
            final long fOverdueCount = overdueCount;
            final BigDecimal fOverdueTotal = overdueTotal;

            SwingUtilities.invokeLater(() -> {
                if (!stopped) {
                    panel.updateData(fNotDueCount, fNotDueTotal, fOverdueCount, fOverdueTotal);
                }
            });

        } catch (Exception ex) {
            log.error("Error loading open invoices for desktop panel", ex);
        }
    }
}
