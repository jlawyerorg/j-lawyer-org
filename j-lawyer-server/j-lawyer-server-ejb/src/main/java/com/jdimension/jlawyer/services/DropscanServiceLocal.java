package com.jdimension.jlawyer.services;

import com.jdimension.jlawyer.dropscan.DropscanActionRequest;
import com.jdimension.jlawyer.dropscan.DropscanMailing;
import com.jdimension.jlawyer.dropscan.DropscanScanbox;
import java.util.List;
import javax.ejb.Local;

@Local
public interface DropscanServiceLocal {

    List<DropscanScanbox> getScanboxesForUser(String principalId) throws Exception;

    /**
     * Discovers the raw (unfiltered) list of scanboxes for a Dropscan account, used while configuring
     * a user's integration ("Test / Scanboxen ermitteln"). If {@code apiToken} is non-empty it is used
     * directly; otherwise the stored, encrypted token of the user identified by {@code principalId} is
     * decrypted and used (falling back to the calling principal's own token when {@code principalId} is
     * empty). Unlike {@link #getScanboxes()} the result is not filtered by the user's scanbox allow-list.
     *
     * @param apiToken a plaintext Dropscan API token to test, or {@code null}/empty to use a stored token
     * @param principalId the user whose stored token should be used when {@code apiToken} is empty
     * @return the scanboxes available for the resolved token
     * @throws Exception on connection failure or when no token can be resolved
     */
    List<DropscanScanbox> discoverScanboxes(String apiToken, String principalId) throws Exception;

    List<DropscanMailing> getMailingsForUser(String principalId, String scanboxId, String status) throws Exception;

    void pollAllUsers();

    // --- user-facing operations (scoped to the authenticated caller) ---
    // Already implemented on DropscanService (remote) and read the caller principal from the
    // session context; exposed on the local interface so the /v8/dropscan REST endpoint can call
    // them. All require loginRole (enforced on the bean).

    List<DropscanScanbox> getScanboxes() throws Exception;

    List<DropscanMailing> getMailings(String scanboxId, String status) throws Exception;

    List<DropscanMailing> getAllMailings(String status) throws Exception;

    DropscanMailing getMailingDetails(String scanboxId, String mailingUuid) throws Exception;

    byte[] getEnvelopeImage(String scanboxId, String mailingUuid) throws Exception;

    byte[] getMailingPdf(String scanboxId, String mailingUuid) throws Exception;

    String getMailingPlaintext(String scanboxId, String mailingUuid) throws Exception;

    byte[] getMailingZip(String scanboxId, String mailingUuid) throws Exception;

    DropscanActionRequest requestScan(String scanboxId, String mailingUuid) throws Exception;

    DropscanActionRequest requestDestroy(String scanboxId, String mailingUuid) throws Exception;

    DropscanActionRequest requestForward(String scanboxId, String mailingUuid, String addressId, String date) throws Exception;
}
