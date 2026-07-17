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

import com.jdimension.jlawyer.persistence.BankStatementsCSVConfig;

/**
 * A CSV bank-statement import profile (Kontoauszug-Import): how a bank's CSV export is parsed —
 * delimiter, decimal format/locale, header/footer line counts, and the zero-based column index of
 * each field.
 */
public class RestfulBankStatementCsvConfigV7 {

    private String id = null;
    private String configurationName = "";
    private String delimiter = ";";
    private String decimalFormat = "#,##0.00";
    private String decimalSeparator = ",";
    private String decimalGroupingCharacter = ".";
    private boolean decimalGrouping = true;
    private String locale = "de_DE";
    private int headerLines = 1;
    private int footerLines = 0;
    private int columnDate = 0;
    private int columnName = 0;
    private int columnBookingType = 0;
    private int columnIban = 0;
    private int columnPurpose = 0;
    private int columnAmount = 0;
    private int columnCurrency = 0;

    public RestfulBankStatementCsvConfigV7() {
    }

    public static RestfulBankStatementCsvConfigV7 fromEntity(BankStatementsCSVConfig c) {
        RestfulBankStatementCsvConfigV7 r = new RestfulBankStatementCsvConfigV7();
        r.setId(c.getId());
        r.setConfigurationName(c.getConfigurationName());
        r.setDelimiter(c.getDelimiter());
        r.setDecimalFormat(c.getDecimalFormat());
        r.setDecimalSeparator(c.getDecimalSeparator());
        r.setDecimalGroupingCharacter(c.getDecimalGroupingCharacter());
        r.setDecimalGrouping(c.isDecimalGrouping());
        r.setLocale(c.getLocale());
        r.setHeaderLines(c.getHeaderLines());
        r.setFooterLines(c.getFooterLines());
        r.setColumnDate(c.getColumnDate());
        r.setColumnName(c.getColumnName());
        r.setColumnBookingType(c.getColumnBookingType());
        r.setColumnIban(c.getColumnIban());
        r.setColumnPurpose(c.getColumnPurpose());
        r.setColumnAmount(c.getColumnAmount());
        r.setColumnCurrency(c.getColumnCurrency());
        return r;
    }

    public BankStatementsCSVConfig toEntity() {
        BankStatementsCSVConfig c = new BankStatementsCSVConfig();
        c.setId(this.id);
        c.setConfigurationName(this.configurationName);
        c.setDelimiter(this.delimiter);
        c.setDecimalFormat(this.decimalFormat);
        c.setDecimalSeparator(this.decimalSeparator);
        c.setDecimalGroupingCharacter(this.decimalGroupingCharacter);
        c.setDecimalGrouping(this.decimalGrouping);
        c.setLocale(this.locale);
        c.setHeaderLines(this.headerLines);
        c.setFooterLines(this.footerLines);
        c.setColumnDate(this.columnDate);
        c.setColumnName(this.columnName);
        c.setColumnBookingType(this.columnBookingType);
        c.setColumnIban(this.columnIban);
        c.setColumnPurpose(this.columnPurpose);
        c.setColumnAmount(this.columnAmount);
        c.setColumnCurrency(this.columnCurrency);
        return c;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getConfigurationName() { return configurationName; }
    public void setConfigurationName(String configurationName) { this.configurationName = configurationName; }
    public String getDelimiter() { return delimiter; }
    public void setDelimiter(String delimiter) { this.delimiter = delimiter; }
    public String getDecimalFormat() { return decimalFormat; }
    public void setDecimalFormat(String decimalFormat) { this.decimalFormat = decimalFormat; }
    public String getDecimalSeparator() { return decimalSeparator; }
    public void setDecimalSeparator(String decimalSeparator) { this.decimalSeparator = decimalSeparator; }
    public String getDecimalGroupingCharacter() { return decimalGroupingCharacter; }
    public void setDecimalGroupingCharacter(String decimalGroupingCharacter) { this.decimalGroupingCharacter = decimalGroupingCharacter; }
    public boolean isDecimalGrouping() { return decimalGrouping; }
    public void setDecimalGrouping(boolean decimalGrouping) { this.decimalGrouping = decimalGrouping; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public int getHeaderLines() { return headerLines; }
    public void setHeaderLines(int headerLines) { this.headerLines = headerLines; }
    public int getFooterLines() { return footerLines; }
    public void setFooterLines(int footerLines) { this.footerLines = footerLines; }
    public int getColumnDate() { return columnDate; }
    public void setColumnDate(int columnDate) { this.columnDate = columnDate; }
    public int getColumnName() { return columnName; }
    public void setColumnName(int columnName) { this.columnName = columnName; }
    public int getColumnBookingType() { return columnBookingType; }
    public void setColumnBookingType(int columnBookingType) { this.columnBookingType = columnBookingType; }
    public int getColumnIban() { return columnIban; }
    public void setColumnIban(int columnIban) { this.columnIban = columnIban; }
    public int getColumnPurpose() { return columnPurpose; }
    public void setColumnPurpose(int columnPurpose) { this.columnPurpose = columnPurpose; }
    public int getColumnAmount() { return columnAmount; }
    public void setColumnAmount(int columnAmount) { this.columnAmount = columnAmount; }
    public int getColumnCurrency() { return columnCurrency; }
    public void setColumnCurrency(int columnCurrency) { this.columnCurrency = columnCurrency; }

}
