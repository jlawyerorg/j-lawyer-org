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
 * The system mailbox (SMTP) used by the server to send administrative and business notifications —
 * the web equivalent of the desktop "Systempostfach" dialog. Backed by the {@code MONITOR_SMTP*}
 * server settings. The password is write-only: never returned (only {@link #passwordSet} indicates
 * one is stored) and applied on write only when non-empty.
 */
public class RestfulSystemMailboxV7 {

    private String smtpServer;
    private String smtpPort;
    private String smtpUser;
    private String password;
    private boolean passwordSet;
    private String senderEmail;
    private String senderName;
    private String recipient;
    private boolean ssl;
    private boolean startTls;

    public RestfulSystemMailboxV7() {
    }

    public String getSmtpServer() { return smtpServer; }
    public void setSmtpServer(String smtpServer) { this.smtpServer = smtpServer; }
    public String getSmtpPort() { return smtpPort; }
    public void setSmtpPort(String smtpPort) { this.smtpPort = smtpPort; }
    public String getSmtpUser() { return smtpUser; }
    public void setSmtpUser(String smtpUser) { this.smtpUser = smtpUser; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isPasswordSet() { return passwordSet; }
    public void setPasswordSet(boolean passwordSet) { this.passwordSet = passwordSet; }
    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public boolean isSsl() { return ssl; }
    public void setSsl(boolean ssl) { this.ssl = ssl; }
    public boolean isStartTls() { return startTls; }
    public void setStartTls(boolean startTls) { this.startTls = startTls; }

}
