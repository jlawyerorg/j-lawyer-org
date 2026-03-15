package com.jdimension.jlawyer.services;

import com.jdimension.jlawyer.dropscan.DropscanActionRequest;
import com.jdimension.jlawyer.dropscan.DropscanApiClient;
import com.jdimension.jlawyer.dropscan.DropscanMailing;
import com.jdimension.jlawyer.dropscan.DropscanScanbox;
import com.jdimension.jlawyer.persistence.AppUserBean;
import com.jdimension.jlawyer.persistence.AppUserBeanFacadeLocal;
import com.jdimension.jlawyer.security.CryptoProvider;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import org.apache.log4j.Logger;

@Stateless
public class DropscanService implements DropscanServiceRemote, DropscanServiceLocal {

    private static final Logger log = Logger.getLogger(DropscanService.class.getName());
    private static final String EXCEPTION_DROPSCAN_INACTIVE = "Dropscan-Integration ist nicht aktiviert!";

    @Resource
    private SessionContext context;
    @EJB
    private AppUserBeanFacadeLocal userBeanFacade;
    @EJB
    private SystemManagementLocal sysMan;

    private DropscanApiClient getClientForCurrentUser() throws Exception {
        AppUserBean currentUser = this.sysMan.getUser(context.getCallerPrincipal().getName());
        if (!currentUser.isDropscanEnabled()) {
            throw new Exception(EXCEPTION_DROPSCAN_INACTIVE);
        }
        String decryptedToken = CryptoProvider.newCrypto().decrypt(currentUser.getDropscanApiToken());
        return new DropscanApiClient(decryptedToken);
    }

    private DropscanApiClient getClientForUser(String principalId) throws Exception {
        AppUserBean user = this.sysMan.getUser(principalId);
        if (!user.isDropscanEnabled()) {
            throw new Exception(EXCEPTION_DROPSCAN_INACTIVE);
        }
        String decryptedToken = CryptoProvider.newCrypto().decrypt(user.getDropscanApiToken());
        return new DropscanApiClient(decryptedToken);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public List<DropscanScanbox> getScanboxes() throws Exception {
        DropscanApiClient client = getClientForCurrentUser();
        return client.getScanboxes();
    }

    @Override
    @RolesAllowed({"loginRole"})
    public List<DropscanMailing> getMailings(String scanboxId, String status) throws Exception {
        DropscanApiClient client = getClientForCurrentUser();
        return client.getMailings(scanboxId, status, null);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public List<DropscanMailing> getAllMailings(String status) throws Exception {
        DropscanApiClient client = getClientForCurrentUser();
        List<DropscanScanbox> scanboxes = client.getScanboxes();
        List<DropscanMailing> allMailings = new ArrayList<>();
        for (DropscanScanbox box : scanboxes) {
            List<DropscanMailing> mailings = client.getMailings(String.valueOf(box.getId()), status, null);
            for (DropscanMailing m : mailings) {
                m.setScanboxNumber(box.getNumber());
            }
            allMailings.addAll(mailings);
        }
        return allMailings;
    }

    @Override
    @RolesAllowed({"loginRole"})
    public DropscanMailing getMailingDetails(String scanboxId, String mailingUuid) throws Exception {
        DropscanApiClient client = getClientForCurrentUser();
        return client.getMailingDetails(scanboxId, mailingUuid);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public byte[] getEnvelopeImage(String scanboxId, String mailingUuid) throws Exception {
        DropscanApiClient client = getClientForCurrentUser();
        return client.getEnvelopeImage(scanboxId, mailingUuid);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public String getMailingPlaintext(String scanboxId, String mailingUuid) throws Exception {
        DropscanApiClient client = getClientForCurrentUser();
        return client.getMailingPlaintext(scanboxId, mailingUuid);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public byte[] getMailingZip(String scanboxId, String mailingUuid) throws Exception {
        DropscanApiClient client = getClientForCurrentUser();
        return client.getMailingZip(scanboxId, mailingUuid);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public DropscanActionRequest requestScan(String scanboxId, String mailingUuid) throws Exception {
        DropscanApiClient client = getClientForCurrentUser();
        return client.requestScan(scanboxId, mailingUuid);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public DropscanActionRequest requestDestroy(String scanboxId, String mailingUuid) throws Exception {
        DropscanApiClient client = getClientForCurrentUser();
        return client.requestDestroy(scanboxId, mailingUuid);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public DropscanActionRequest requestForward(String scanboxId, String mailingUuid, String addressId, String date) throws Exception {
        DropscanApiClient client = getClientForCurrentUser();
        return client.requestForward(scanboxId, mailingUuid, addressId, date);
    }

    // Local interface methods for scheduled polling

    @Override
    public List<DropscanScanbox> getScanboxesForUser(String principalId) throws Exception {
        DropscanApiClient client = getClientForUser(principalId);
        return client.getScanboxes();
    }

    @Override
    public List<DropscanMailing> getMailingsForUser(String principalId, String scanboxId, String status) throws Exception {
        DropscanApiClient client = getClientForUser(principalId);
        List<DropscanMailing> mailings = client.getMailings(scanboxId, status, null);
        return mailings;
    }

    @Override
    public void pollAllUsers() {
//        try {
//            List<AppUserBean> allUsers = this.userBeanFacade.findAll();
//            for (AppUserBean user : allUsers) {
//                if (!user.isDropscanEnabled()) {
//                    continue;
//                }
//                try {
//                    DropscanApiClient client = getClientForUser(user.getPrincipalId());
//                    List<DropscanScanbox> scanboxes = client.getScanboxes();
//                    int newMailingCount = 0;
//                    for (DropscanScanbox box : scanboxes) {
//                        List<DropscanMailing> mailings = client.getMailings(String.valueOf(box.getId()), DropscanMailing.STATUS_RECEIVED, null);
//                        newMailingCount += mailings.size();
//                    }
//                    if (newMailingCount > 0) {
//                        log.info("Dropscan: " + newMailingCount + " neue Sendung(en) fuer Benutzer " + user.getPrincipalId());
//                    }
//                } catch (Exception ex) {
//                    log.error("Dropscan polling failed for user " + user.getPrincipalId(), ex);
//                }
//            }
//        } catch (Exception ex) {
//            log.error("Dropscan polling failed", ex);
//        }
    }
}
