/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package org.jlawyer.invoicing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import org.mustangproject.BankDetails;
import org.mustangproject.Contact;
import org.mustangproject.Invoice;
import org.mustangproject.Item;
import org.mustangproject.Product;
import org.mustangproject.TradeParty;
import org.mustangproject.ZUGFeRD.Profiles;
import org.mustangproject.ZUGFeRD.ZUGFeRD2PullProvider;

/**
 *
 * @author jens
 */
public class JLawyerInvoicing {

    public static void main(String[] args) {
        System.out.println("Hello World!");
        
        Invoice i = new Invoice().setDueDate(new Date()).setIssueDate(new Date()).setDeliveryDate(new Date())
				.setSender(new TradeParty("Test company","teststr","55232","teststadt","DE").addTaxID("DE4711").addVATID("DE0815").setContact(new Contact("Hans Test","+49123456789","test@example.org")).addBankDetails(new BankDetails("DE12500105170648489890","COBADEFXXX")))
				.setRecipient(new TradeParty("Franz Müller", "teststr.12", "55232", "Entenhausen", "DE"))
				.setReferenceNumber("991-01484-64")//leitweg-id
				.setNumber("123").addItem(new Item(new Product("Testprodukt", "", "H87", BigDecimal.ZERO), /*price*/ new BigDecimal("1.0"),  /*qty*/ new BigDecimal("1.0")));

		ZUGFeRD2PullProvider zf2p = new ZUGFeRD2PullProvider();
		zf2p.setProfile(Profiles.getByName("XRechnung"));
		zf2p.generateXML(i);
		String theXML = new String(zf2p.getXML());
			try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("xrechnung.xml"));
			writer.write(theXML);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
    }
}
