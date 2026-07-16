/*
 * Copyright (C) 2026 Jens Kutschke
 *
 * This file is part of j-lawyer.org.
 *
 * j-lawyer.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * j-lawyer.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with j-lawyer.org.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jlawyer.io.rest.v7.pojo;

/**
 * The firm master data ("Kanzleidaten") — company address, contact details, tax identifiers and bank
 * accounts, stored as server settings under the {@code profile.company.*} keys. Mirrors the desktop
 * ProfileDialog. Empty strings represent unset values.
 */
public class RestfulFirmProfileV7 {

    private String companyName = "";
    private String street = "";
    private String street2 = "";
    private String zipCode = "";
    private String city = "";
    private String country = "";
    private String phone = "";
    private String fax = "";
    private String mobile = "";
    private String email = "";
    private String website = "";
    private String taxId = "";
    private String vatId = "";
    private String bank = "";
    private String bic = "";
    private String iban = "";
    private String escrowBank = "";
    private String escrowBic = "";
    private String escrowIban = "";

    public RestfulFirmProfileV7() {
    }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getStreet2() { return street2; }
    public void setStreet2(String street2) { this.street2 = street2; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getFax() { return fax; }
    public void setFax(String fax) { this.fax = fax; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public String getVatId() { return vatId; }
    public void setVatId(String vatId) { this.vatId = vatId; }

    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }

    public String getBic() { return bic; }
    public void setBic(String bic) { this.bic = bic; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getEscrowBank() { return escrowBank; }
    public void setEscrowBank(String escrowBank) { this.escrowBank = escrowBank; }

    public String getEscrowBic() { return escrowBic; }
    public void setEscrowBic(String escrowBic) { this.escrowBic = escrowBic; }

    public String getEscrowIban() { return escrowIban; }
    public void setEscrowIban(String escrowIban) { this.escrowIban = escrowIban; }

}
