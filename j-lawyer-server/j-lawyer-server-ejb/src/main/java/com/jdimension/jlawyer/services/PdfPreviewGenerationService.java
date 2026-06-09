/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.services;

import com.jdimension.jlawyer.documents.DocumentPreview;
import com.jdimension.jlawyer.documents.PreviewGenerator;
import com.jdimension.jlawyer.events.PdfPreviewGenerationRequest;
import com.jdimension.jlawyer.persistence.ArchiveFileDocumentsBeanFacadeLocal;
import com.jdimension.jlawyer.persistence.ServerSettingsBean;
import com.jdimension.jlawyer.persistence.ServerSettingsBeanFacadeLocal;
import com.jdimension.jlawyer.server.services.settings.ServerSettingsKeys;
import com.jdimension.jlawyer.server.utils.ServerStringUtils;
import com.jdimension.jlawyer.stirlingpdf.StirlingPdfAPI;
import java.util.concurrent.TimeUnit;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.ObservesAsync;
import org.apache.log4j.Logger;
import org.jboss.ejb3.annotation.TransactionTimeout;

/**
 * Service for asynchronous PDF preview generation.
 * Observes PdfPreviewGenerationRequest events and generates PDF previews
 * without blocking the main document upload/update operations.
 *
 * @author jens
 */
@Singleton
@Startup
public class PdfPreviewGenerationService {

    private static final Logger log = Logger.getLogger(PdfPreviewGenerationService.class.getName());

    @EJB
    private ArchiveFileDocumentsBeanFacadeLocal archiveFileDocumentsFacade;

    @EJB
    private ServerSettingsBeanFacadeLocal settingsFacade;

    @TransactionTimeout(value = 10, unit = TimeUnit.MINUTES)
    public void onPdfPreviewRequest(@ObservesAsync PdfPreviewGenerationRequest request) {
        log.info("Processing async PDF preview generation for document: " + request.getDocumentId());

        try {
            ServerSettingsBean sb = this.settingsFacade.find(ServerSettingsKeys.SERVERCONF_STIRLINGPDF_ENDPOINT);
            StirlingPdfAPI pdfApi = null;
            if (sb != null && !ServerStringUtils.isEmpty(sb.getSettingValue())) {
                pdfApi = new StirlingPdfAPI(sb.getSettingValue(), 5000, 120000);
            }

            if (pdfApi == null) {
                log.debug("Stirling-PDF not configured, skipping PDF preview generation for document: " + request.getDocumentId());
                return;
            }

            PreviewGenerator pg = new PreviewGenerator(this.archiveFileDocumentsFacade, pdfApi);

            if (request.isUpdate()) {
                DocumentPreview pdfPreview = pg.updatePreview(
                        request.getArchiveFileId(),
                        request.getDocumentId(),
                        request.getFileName(),
                        DocumentPreview.TYPE_PDF
                );
                log.debug("Updated PDF preview for document: " + request.getDocumentId());
            } else {
                DocumentPreview pdfPreview = pg.createPreview(
                        request.getArchiveFileId(),
                        request.getDocumentId(),
                        request.getFileName(),
                        DocumentPreview.TYPE_PDF
                );
                log.debug("Created PDF preview for document: " + request.getDocumentId());
            }

        } catch (Throwable t) {
            log.error("Error during async PDF preview generation for document: " + request.getDocumentId(), t);
        }
    }
}
