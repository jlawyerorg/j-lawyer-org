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

    private static final String EXCEPTION_SCANBOX_NOT_ALLOWED = "Scanbox für diesen Benutzer nicht freigegeben!";

    private AppUserBean getCurrentUser() throws Exception {
        return this.sysMan.getUser(context.getCallerPrincipal().getName());
    }

    private DropscanApiClient getClientForUser(AppUserBean user) throws Exception {
        if (user == null || !user.isDropscanEnabled()) {
            throw new Exception(EXCEPTION_DROPSCAN_INACTIVE);
        }
        String decryptedToken = CryptoProvider.newCrypto().decrypt(user.getDropscanApiToken());
        return new DropscanApiClient(decryptedToken);
    }

    /**
     * Filters the given scanboxes to those the user is allowed to see. An empty
     * allow-list on the user means no restriction (all scanboxes returned).
     */
    private List<DropscanScanbox> filterAllowed(List<DropscanScanbox> scanboxes, AppUserBean user) {
        List<DropscanScanbox> result = new ArrayList<>();
        for (DropscanScanbox box : scanboxes) {
            if (user.isScanboxAllowed(String.valueOf(box.getId()))) {
                result.add(box);
            }
        }
        return result;
    }

    private void assertScanboxAllowed(AppUserBean user, String scanboxId) throws Exception {
        if (!user.isScanboxAllowed(scanboxId)) {
            throw new Exception(EXCEPTION_SCANBOX_NOT_ALLOWED);
        }
    }

    @Override
    @RolesAllowed({"loginRole"})
    public List<DropscanScanbox> getScanboxes() throws Exception {
        AppUserBean user = getCurrentUser();
        DropscanApiClient client = getClientForUser(user);
        return filterAllowed(client.getScanboxes(), user);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public List<DropscanMailing> getMailings(String scanboxId, String status) throws Exception {
        AppUserBean user = getCurrentUser();
        assertScanboxAllowed(user, scanboxId);
        DropscanApiClient client = getClientForUser(user);
        return client.getMailings(scanboxId, status, null);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public List<DropscanMailing> getAllMailings(String status) throws Exception {
        AppUserBean user = getCurrentUser();
        DropscanApiClient client = getClientForUser(user);
        List<DropscanScanbox> scanboxes = filterAllowed(client.getScanboxes(), user);
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
        AppUserBean user = getCurrentUser();
        assertScanboxAllowed(user, scanboxId);
        DropscanApiClient client = getClientForUser(user);
        return client.getMailingDetails(scanboxId, mailingUuid);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public byte[] getEnvelopeImage(String scanboxId, String mailingUuid) throws Exception {
        AppUserBean user = getCurrentUser();
        assertScanboxAllowed(user, scanboxId);
        DropscanApiClient client = getClientForUser(user);
        return client.getEnvelopeImage(scanboxId, mailingUuid);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public byte[] getMailingPdf(String scanboxId, String mailingUuid) throws Exception {
        AppUserBean user = getCurrentUser();
        assertScanboxAllowed(user, scanboxId);
        DropscanApiClient client = getClientForUser(user);
        return client.getMailingPdf(scanboxId, mailingUuid);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public String getMailingPlaintext(String scanboxId, String mailingUuid) throws Exception {
        AppUserBean user = getCurrentUser();
        assertScanboxAllowed(user, scanboxId);
        DropscanApiClient client = getClientForUser(user);
        return client.getMailingPlaintext(scanboxId, mailingUuid);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public byte[] getMailingZip(String scanboxId, String mailingUuid) throws Exception {
        AppUserBean user = getCurrentUser();
        assertScanboxAllowed(user, scanboxId);
        DropscanApiClient client = getClientForUser(user);
        return client.getMailingZip(scanboxId, mailingUuid);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public DropscanActionRequest requestScan(String scanboxId, String mailingUuid) throws Exception {
        AppUserBean user = getCurrentUser();
        assertScanboxAllowed(user, scanboxId);
        DropscanApiClient client = getClientForUser(user);
        return client.requestScan(scanboxId, mailingUuid);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public DropscanActionRequest requestDestroy(String scanboxId, String mailingUuid) throws Exception {
        AppUserBean user = getCurrentUser();
        assertScanboxAllowed(user, scanboxId);
        DropscanApiClient client = getClientForUser(user);
        return client.requestDestroy(scanboxId, mailingUuid);
    }

    @Override
    @RolesAllowed({"loginRole"})
    public DropscanActionRequest requestForward(String scanboxId, String mailingUuid, String addressId, String date) throws Exception {
        AppUserBean user = getCurrentUser();
        assertScanboxAllowed(user, scanboxId);
        DropscanApiClient client = getClientForUser(user);
        return client.requestForward(scanboxId, mailingUuid, addressId, date);
    }

    // Local interface methods for scheduled polling

    @Override
    public List<DropscanScanbox> getScanboxesForUser(String principalId) throws Exception {
        AppUserBean user = this.sysMan.getUser(principalId);
        DropscanApiClient client = getClientForUser(user);
        return filterAllowed(client.getScanboxes(), user);
    }

    @Override
    public List<DropscanMailing> getMailingsForUser(String principalId, String scanboxId, String status) throws Exception {
        AppUserBean user = this.sysMan.getUser(principalId);
        assertScanboxAllowed(user, scanboxId);
        DropscanApiClient client = getClientForUser(user);
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
