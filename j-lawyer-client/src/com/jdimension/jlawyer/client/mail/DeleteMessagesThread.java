/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.mail;

import com.jdimension.jlawyer.client.utils.DesktopUtils;
import com.jdimension.jlawyer.email.CommonMailUtils;
import com.jdimension.jlawyer.persistence.MailboxSetup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;

/**
 * Thread for deleting email messages in the background without blocking the UI.
 * Moves messages to trash (IMAP) or marks them as deleted (POP3).
 */
public class DeleteMessagesThread implements Runnable {

    private static final Logger log = Logger.getLogger(DeleteMessagesThread.class.getName());

    private final List<MessageContainer> messagesToDelete;
    private final MailboxSetup mailboxSetup;
    private final HashMap<MailboxSetup, FolderContainer> trashFolders;
    private final HashMap<MailboxSetup, FolderContainer> inboxFolders;
    private final Runnable onComplete;

    public DeleteMessagesThread(List<MessageContainer> messages,
            MailboxSetup mailboxSetup,
            HashMap<MailboxSetup, FolderContainer> trashFolders,
            HashMap<MailboxSetup, FolderContainer> inboxFolders,
            Runnable onComplete) {
        this.messagesToDelete = messages;
        this.mailboxSetup = mailboxSetup;
        this.trashFolders = trashFolders;
        this.inboxFolders = inboxFolders;
        this.onComplete = onComplete;
    }

    @Override
    public void run() {
        if (messagesToDelete == null || messagesToDelete.isEmpty()) {
            invokeOnComplete();
            return;
        }

        Folder expungeFolder = null;
        boolean expungeClosed = true;
        Folder trashFolderCheck = null;

        try {
            // Get the folder from the first message
            MessageContainer firstMsg = messagesToDelete.get(0);
            expungeFolder = firstMsg.getMessage().getFolder();
            expungeClosed = !expungeFolder.isOpen();

            // Open expunge folder if needed
            if (expungeClosed) {
                expungeFolder.open(Folder.READ_WRITE);
            }

            // Get trash folder if available
            if (trashFolders.containsKey(mailboxSetup)) {
                trashFolderCheck = trashFolders.get(mailboxSetup).getFolder();
                if (!trashFolderCheck.isOpen()) {
                    trashFolderCheck.open(Folder.READ_WRITE);
                }
            }

            // Collect all messages
            ArrayList<Message> messages = new ArrayList<>();
            for (MessageContainer msgC : messagesToDelete) {
                messages.add(msgC.getMessage());
            }

            // Perform deletion
            if (EmailUtils.isIMAP(expungeFolder)) {
                deleteImapMessages(expungeFolder, messages, trashFolderCheck);
            } else {
                // POP3: just mark as deleted
                for (Message m : messages) {
                    m.setFlag(Flags.Flag.DELETED, true);
                }
            }

        } catch (Exception ex) {
            log.error("Error deleting messages", ex);
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "Fehler beim Löschen: " + ex.getMessage(),
                        DesktopUtils.POPUP_TITLE_ERROR,
                        JOptionPane.ERROR_MESSAGE);
            });
        } finally {
            // Close folders
            closeFolderSilently(trashFolderCheck);
            closePop3FolderIfNeeded(expungeFolder);
            invokeOnComplete();
        }
    }

    private void deleteImapMessages(Folder expungeFolder, ArrayList<Message> messages, Folder trashFolder) throws MessagingException {
        if (expungeFolder.getName().equalsIgnoreCase(CommonMailUtils.TRASH)) {
            // Already in trash - just mark as deleted, don't move again
            return;
        }

        // Ensure folder is open
        if (!expungeFolder.isOpen()) {
            expungeFolder.open(Folder.READ_WRITE);
        }

        Message[] msgArray = messages.toArray(new Message[0]);

        // Try to copy to trash
        try {
            if (trashFolder != null) {
                expungeFolder.copyMessages(msgArray, trashFolder);
            }
        } catch (Throwable t) {
            log.warn("Unable to copy messages to trash, retrying", t);
            // Retry with reopened trash folder
            if (trashFolders.containsKey(mailboxSetup)) {
                Folder retryTrash = trashFolders.get(mailboxSetup).getFolder();
                if (!retryTrash.isOpen()) {
                    retryTrash.open(Folder.READ_WRITE);
                }
                expungeFolder.copyMessages(msgArray, retryTrash);
            }
        }

        // Mark as deleted in source folder
        Flags deleteFlags = new Flags(Flag.DELETED);
        expungeFolder.setFlags(msgArray, deleteFlags, true);
    }

    private void closeFolderSilently(Folder folder) {
        if (folder != null) {
            try {
                EmailUtils.closeIfIMAP(folder);
            } catch (Throwable t) {
                log.error("Error closing folder", t);
            }
        }
    }

    private void closePop3FolderIfNeeded(Folder expungeFolder) {
        if (expungeFolder != null && !EmailUtils.isIMAP(expungeFolder)) {
            try {
                if (inboxFolders.containsKey(mailboxSetup)) {
                    inboxFolders.get(mailboxSetup).getFolder().close(true);
                }
            } catch (MessagingException ex) {
                log.error("Error closing POP3 folder", ex);
            }
        }
    }

    private void invokeOnComplete() {
        if (onComplete != null) {
            SwingUtilities.invokeLater(onComplete);
        }
    }
}
