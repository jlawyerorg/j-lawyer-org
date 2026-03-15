package com.jdimension.jlawyer.services;

import com.jdimension.jlawyer.dropscan.DropscanActionRequest;
import com.jdimension.jlawyer.dropscan.DropscanMailing;
import com.jdimension.jlawyer.dropscan.DropscanScanbox;
import java.util.List;
import javax.ejb.Remote;

/**
 * Remote interface for the Dropscan integration service.
 * Provides methods to interact with the Dropscan REST API
 * for managing digital mailbox scanning. Each method operates
 * using the calling user's configured Dropscan API token.
 */
@Remote
public interface DropscanServiceRemote {

    /**
     * Retrieves all scanboxes for the current user's Dropscan account.
     *
     * @return list of scanboxes associated with the user's API token
     * @throws Exception if the API call fails or no token is configured
     */
    List<DropscanScanbox> getScanboxes() throws Exception;

    /**
     * Retrieves mailings from a specific scanbox, optionally filtered by status.
     *
     * @param scanboxId the scanbox ID to query
     * @param status the mailing status to filter by, or null for all statuses
     * @return list of mailings matching the filters
     * @throws Exception if the API call fails
     */
    List<DropscanMailing> getMailings(String scanboxId, String status) throws Exception;

    /**
     * Retrieves mailings from all scanboxes of the user's account,
     * optionally filtered by status.
     *
     * @param status the mailing status to filter by, or null for all statuses
     * @return list of mailings from all scanboxes
     * @throws Exception if the API call fails
     */
    List<DropscanMailing> getAllMailings(String status) throws Exception;

    /**
     * Retrieves detailed information about a specific mailing.
     *
     * @param scanboxId the scanbox ID
     * @param mailingUuid the mailing UUID
     * @return the mailing details
     * @throws Exception if the API call fails
     */
    DropscanMailing getMailingDetails(String scanboxId, String mailingUuid) throws Exception;

    /**
     * Retrieves the envelope image for a specific mailing.
     *
     * @param scanboxId the scanbox ID
     * @param mailingUuid the mailing UUID
     * @return the envelope JPEG image as byte array
     * @throws Exception if the API call fails
     */
    byte[] getEnvelopeImage(String scanboxId, String mailingUuid) throws Exception;

    /**
     * Retrieves the OCR plaintext for a specific mailing.
     *
     * @param scanboxId the scanbox ID
     * @param mailingUuid the mailing UUID
     * @return the OCR plaintext, or empty string if unavailable
     * @throws Exception if the API call fails
     */
    String getMailingPlaintext(String scanboxId, String mailingUuid) throws Exception;

    /**
     * Downloads the ZIP containing all scanned documents for a mailing.
     *
     * @param scanboxId the scanbox ID
     * @param mailingUuid the mailing UUID
     * @return the ZIP file as byte array
     * @throws Exception if the API call fails
     */
    byte[] getMailingZip(String scanboxId, String mailingUuid) throws Exception;

    /**
     * Requests a scan action for a received mailing.
     *
     * @param scanboxId the scanbox ID
     * @param mailingUuid the mailing UUID
     * @return the action request details
     * @throws Exception if the API call fails
     */
    DropscanActionRequest requestScan(String scanboxId, String mailingUuid) throws Exception;

    /**
     * Requests destruction of a mailing's physical documents.
     *
     * @param scanboxId the scanbox ID
     * @param mailingUuid the mailing UUID
     * @return the action request details
     * @throws Exception if the API call fails
     */
    DropscanActionRequest requestDestroy(String scanboxId, String mailingUuid) throws Exception;

    /**
     * Requests forwarding of a mailing to a physical address.
     *
     * @param scanboxId the scanbox ID
     * @param mailingUuid the mailing UUID
     * @param addressId the forwarding address ID
     * @param date the requested forwarding date (yyyy-MM-dd)
     * @return the action request details
     * @throws Exception if the API call fails
     */
    DropscanActionRequest requestForward(String scanboxId, String mailingUuid, String addressId, String date) throws Exception;
}
