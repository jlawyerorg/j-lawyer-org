package org.jlawyer.plugins.calculation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Serializable data carrier for flexible invoice table layout.
 * Contains per-position placeholder data (BELP_*), per-tax-rate data
 * (BEL_UST_SATZ/BEL_UST_BETRAG), and a formatted net total string.
 */
public class FlexibleInvoiceTableData implements Serializable {

    private ArrayList<HashMap<String, String>> positions = new ArrayList<>();
    private ArrayList<HashMap<String, String>> taxRates = new ArrayList<>();
    private String netTotal = "";

    public FlexibleInvoiceTableData() {
    }

    public ArrayList<HashMap<String, String>> getPositions() {
        return positions;
    }

    public void setPositions(ArrayList<HashMap<String, String>> positions) {
        this.positions = positions;
    }

    public ArrayList<HashMap<String, String>> getTaxRates() {
        return taxRates;
    }

    public void setTaxRates(ArrayList<HashMap<String, String>> taxRates) {
        this.taxRates = taxRates;
    }

    public String getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(String netTotal) {
        this.netTotal = netTotal;
    }
}
