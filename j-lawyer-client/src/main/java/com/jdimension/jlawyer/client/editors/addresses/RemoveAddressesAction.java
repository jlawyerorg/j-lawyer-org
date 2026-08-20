package com.jdimension.jlawyer.client.editors.addresses;

import com.jdimension.jlawyer.client.processing.ProgressIndicator;
import com.jdimension.jlawyer.client.processing.ProgressableAction;
import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.services.AddressServiceRemote;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public class RemoveAddressesAction extends ProgressableAction {

    private static final Logger log = Logger.getLogger(RemoveAddressesAction.class.getName());

    private final List<String> allIds;
    private final List<String> failedIds = new ArrayList<>();
    private final int batchSize;

    public RemoveAddressesAction(ProgressIndicator i, List<String> allIds, int batchSize) {
        super(i, false);
        this.allIds = allIds;
        this.batchSize = batchSize;
    }

    @Override
    public int getMax() {
        return allIds.size();
    }

    @Override
    public int getMin() {
        return 0;
    }

    @Override
    public boolean execute() throws Exception {
        ClientSettings settings = ClientSettings.getInstance();
        JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());
        AddressServiceRemote addressService = locator.lookupAddressServiceRemote();

        int total = allIds.size();
        for (int i = 0; i < total; i += batchSize) {
            if (isCancelled()) {
                break;
            }
            int end = Math.min(i + batchSize, total);
            List<String> batch = allIds.subList(i, end);
            List<String> batchFailed = addressService.removeAddresses(new ArrayList<>(batch));
            failedIds.addAll(batchFailed);
            for (int j = 0; j < batch.size(); j++) {
                progress("Lösche Adressen... (" + Math.min(i + j + 1, total) + "/" + total + ")");
            }
        }
        return true;
    }

    public List<String> getFailedIds() {
        return failedIds;
    }
}
