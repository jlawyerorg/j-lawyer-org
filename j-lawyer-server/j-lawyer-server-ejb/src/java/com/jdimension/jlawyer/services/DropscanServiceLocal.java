package com.jdimension.jlawyer.services;

import com.jdimension.jlawyer.dropscan.DropscanMailing;
import com.jdimension.jlawyer.dropscan.DropscanScanbox;
import java.util.List;
import javax.ejb.Local;

@Local
public interface DropscanServiceLocal {

    List<DropscanScanbox> getScanboxesForUser(String principalId) throws Exception;

    List<DropscanMailing> getMailingsForUser(String principalId, String scanboxId, String status) throws Exception;

    void pollAllUsers();
}
