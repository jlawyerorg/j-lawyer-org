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
 * Server-wide case-number (Aktenzeichen) configuration. The {@code pattern} uses the tokens
 * understood by the case-number generator: {@code YY}/{@code YYYY} (year), {@code M}/{@code MM}
 * (month), {@code D}/{@code DD} (day), {@code nnn+} (per-context running index, min. 3 digits),
 * {@code NNNN+} (global running index, min. 4 digits), {@code RRR+} (random number, min. 3 digits),
 * {@code CCCC+} (random letter code, min. 4). The optional extension appends further parts (fixed
 * prefix/suffix, lawyer and group abbreviations) separated by the configured dividers.
 */
public class RestfulCaseNumberingV7 {

    private String pattern = "nnnnn/YY";
    private int startFrom = 1;
    private int increment = 1;

    private boolean extensionEnabled = false;
    private String dividerMain = "";
    private String dividerExt = "";
    private boolean prefixEnabled = false;
    private String prefix = "";
    private boolean suffixEnabled = false;
    private String suffix = "";
    private boolean lawyerAbbrevEnabled = false;
    private boolean groupAbbrevEnabled = false;

    public RestfulCaseNumberingV7() {
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public int getStartFrom() {
        return startFrom;
    }

    public void setStartFrom(int startFrom) {
        this.startFrom = startFrom;
    }

    public int getIncrement() {
        return increment;
    }

    public void setIncrement(int increment) {
        this.increment = increment;
    }

    public boolean isExtensionEnabled() {
        return extensionEnabled;
    }

    public void setExtensionEnabled(boolean extensionEnabled) {
        this.extensionEnabled = extensionEnabled;
    }

    public String getDividerMain() {
        return dividerMain;
    }

    public void setDividerMain(String dividerMain) {
        this.dividerMain = dividerMain;
    }

    public String getDividerExt() {
        return dividerExt;
    }

    public void setDividerExt(String dividerExt) {
        this.dividerExt = dividerExt;
    }

    public boolean isPrefixEnabled() {
        return prefixEnabled;
    }

    public void setPrefixEnabled(boolean prefixEnabled) {
        this.prefixEnabled = prefixEnabled;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isSuffixEnabled() {
        return suffixEnabled;
    }

    public void setSuffixEnabled(boolean suffixEnabled) {
        this.suffixEnabled = suffixEnabled;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public boolean isLawyerAbbrevEnabled() {
        return lawyerAbbrevEnabled;
    }

    public void setLawyerAbbrevEnabled(boolean lawyerAbbrevEnabled) {
        this.lawyerAbbrevEnabled = lawyerAbbrevEnabled;
    }

    public boolean isGroupAbbrevEnabled() {
        return groupAbbrevEnabled;
    }

    public void setGroupAbbrevEnabled(boolean groupAbbrevEnabled) {
        this.groupAbbrevEnabled = groupAbbrevEnabled;
    }

}
