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
package org.jlawyer.io.rest.v8.pojo;

import com.jdimension.jlawyer.persistence.AddressBean;

/**
 * A richer contact overview than v1 (adds title, salutation, street number, country, mobile,
 * e-mail and website) so a contact-list UI can render columns and distinguish people from
 * companies without a per-row detail fetch — OpenSpec change {@code add-web-client}.
 *
 * @author jens
 */
public class RestfulContactOverviewV8 {

    private String id;
    private String externalId;
    private String title;
    private String salutation;
    private String firstName;
    private String name;
    private String company;
    private String department;
    private String street;
    private String streetNumber;
    private String zipCode;
    private String city;
    private String country;
    private String phone;
    private String mobile;
    private String email;
    private String website;

    public RestfulContactOverviewV8() {
    }

    /** Maps the relevant subset of an {@link AddressBean} to a contact overview. */
    public static RestfulContactOverviewV8 fromAddressBean(AddressBean a) {
        RestfulContactOverviewV8 c = new RestfulContactOverviewV8();
        c.setId(a.getId());
        c.setExternalId(a.getExternalId1());
        c.setTitle(a.getTitle());
        c.setSalutation(a.getSalutation());
        c.setFirstName(a.getFirstName());
        c.setName(a.getName());
        c.setCompany(a.getCompany());
        c.setDepartment(a.getDepartment());
        c.setStreet(a.getStreet());
        c.setStreetNumber(a.getStreetNumber());
        c.setZipCode(a.getZipCode());
        c.setCity(a.getCity());
        c.setCountry(a.getCountry());
        c.setPhone(a.getPhone());
        c.setMobile(a.getMobile());
        c.setEmail(a.getEmail());
        c.setWebsite(a.getWebsite());
        return c;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSalutation() {
        return salutation;
    }

    public void setSalutation(String salutation) {
        this.salutation = salutation;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getStreetNumber() {
        return streetNumber;
    }

    public void setStreetNumber(String streetNumber) {
        this.streetNumber = streetNumber;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}
