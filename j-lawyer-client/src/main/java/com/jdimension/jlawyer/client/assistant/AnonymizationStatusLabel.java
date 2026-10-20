package com.jdimension.jlawyer.client.assistant;

import com.jdimension.jlawyer.ai.AiResponse;
import com.jdimension.jlawyer.ai.AnonymizationMapping;
import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.persistence.AssistantConfig;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.apache.log4j.Logger;

/**
 * Header label of the assistant dialogs showing the anonymization status of a custom prompt
 * that is executed with anonymization. A click shows the replacements made (placeholder and
 * original value), as returned by j-lawyer-ai for the current conversation.
 *
 * The label is only visible if anonymization is active. All public methods may be called from
 * any thread.
 */
public class AnonymizationStatusLabel extends JLabel {

    private static final Logger log = Logger.getLogger(AnonymizationStatusLabel.class.getName());

    private static final String ICON_OK = "/icons16/material/policy_20dp_97BF0D.png";
    private static final String ICON_ERROR = "/icons16/material/policy_20dp_DE313B.png";

    // "anonymizing (x/y)" as sent by j-lawyer-ai during entity detection
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("anonymizing\\s*\\((\\d+)\\s*/\\s*(\\d+)\\)");

    private AssistantConfig config = null;
    private volatile String conversationId = null;
    private boolean active = false;

    public AnonymizationStatusLabel() {
        super();
        this.setFont(this.getFont().deriveFont(this.getFont().getSize() - 2f));
        this.setForeground(new Color(153, 153, 153));
        this.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        this.setVisible(false);
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showMappings();
            }
        });
    }

    /**
     * @param config the assistant server the requests are sent to
     * @param active true if requests are anonymized; the label is hidden otherwise
     */
    public void setup(AssistantConfig config, boolean active) {
        this.config = config;
        this.active = active;
        this.conversationId = null;
        onEdt(() -> {
            this.setVisible(active);
            this.setCursor(Cursor.getDefaultCursor());
            this.setState(ICON_OK, "anonymisiert", "Personenbezogene Daten werden vor dem Versand an das KI-Modell durch Platzhalter ersetzt.");
        });
    }

    /**
     * @return true if requests are anonymized
     */
    public boolean isActive() {
        return active;
    }

    /**
     * To be called when an anonymized request is submitted.
     *
     * @param conversationId the conversation id sent with the request
     */
    public void requestStarted(String conversationId) {
        if (!active) {
            return;
        }
        this.conversationId = conversationId;
        onEdt(() -> this.setState(ICON_OK, "anonymisiere...", "Personenbezogene Daten werden erkannt und ersetzt."));
    }

    /**
     * Shows the progress of the entity detection while polling an asynchronous request. During
     * detection, j-lawyer-ai reports "anonymizing (x/y)" and a progress between 0 and 50%.
     *
     * @param res the current request status
     * @param progressBar the dialogs progress bar, may be null. It shows the detection progress
     * and is switched back to indeterminate afterwards.
     */
    public void updateProgress(AiResponse res, JProgressBar progressBar) {
        if (!active || res == null) {
            return;
        }
        String msg = res.getStatusMessage();
        Matcher m = msg != null ? PROGRESS_PATTERN.matcher(msg) : null;
        if (m != null && m.find()) {
            String done = m.group(1);
            String total = m.group(2);
            int value = Math.max(0, Math.min(100, Math.round(res.getProgress())));
            onEdt(() -> {
                this.setState(ICON_OK, "anonymisiere (" + done + "/" + total + ")", "Personenbezogene Daten werden erkannt und ersetzt - Abschnitt " + done + " von " + total + ".");
                if (progressBar != null) {
                    progressBar.setIndeterminate(false);
                    progressBar.setMinimum(0);
                    progressBar.setMaximum(100);
                    progressBar.setValue(value);
                }
            });
        } else {
            onEdt(() -> {
                if (this.getText() != null && this.getText().startsWith("anonymisiere")) {
                    this.setState(ICON_OK, "anonymisiert", "Personenbezogene Daten wurden durch Platzhalter ersetzt.");
                }
                if (progressBar != null && progressBar.isVisible() && !progressBar.isIndeterminate()) {
                    progressBar.setIndeterminate(true);
                }
            });
        }
    }

    /**
     * To be called when an anonymized request has finished. On success, the number of
     * replacements is retrieved and shown.
     *
     * @param error true if the request failed
     */
    public void requestFinished(boolean error) {
        if (!active) {
            return;
        }
        if (error) {
            onEdt(() -> {
                this.setState(ICON_ERROR, "Anonymisierung: Fehler", "Die Anfrage konnte nicht anonymisiert verarbeitet werden.");
                this.setCursor(this.conversationId != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            });
            return;
        }
        final String cid = this.conversationId;
        if (cid == null) {
            return;
        }
        new SwingWorker<List<AnonymizationMapping>, Void>() {
            @Override
            protected List<AnonymizationMapping> doInBackground() throws Exception {
                return loadMappings(cid);
            }

            @Override
            protected void done() {
                try {
                    List<AnonymizationMapping> mappings = get();
                    String text = mappings.size() == 1 ? "1 Angabe anonymisiert" : mappings.size() + " Angaben anonymisiert";
                    setState(ICON_OK, text, "Klicken, um die Ersetzungen anzuzeigen.");
                } catch (Exception ex) {
                    log.warn("Could not load anonymization mappings", ex);
                    setState(ICON_OK, "anonymisiert", "Klicken, um die Ersetzungen anzuzeigen.");
                }
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }.execute();
    }

    private List<AnonymizationMapping> loadMappings(String cid) throws Exception {
        ClientSettings settings = ClientSettings.getInstance();
        JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());
        return locator.lookupIntegrationServiceRemote().getAssistantAnonymizationMappings(this.config, cid);
    }

    private void showMappings() {
        final String cid = this.conversationId;
        if (!active || cid == null) {
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<List<AnonymizationMapping>, Void>() {
            @Override
            protected List<AnonymizationMapping> doInBackground() throws Exception {
                return loadMappings(cid);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                try {
                    List<AnonymizationMapping> mappings = get();
                    AnonymizationMappingsDialog dlg = new AnonymizationMappingsDialog(owner, mappings);
                    dlg.setLocationRelativeTo(owner);
                    dlg.setVisible(true);
                } catch (Exception ex) {
                    log.error("Could not load anonymization mappings", ex);
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(owner, "Die Ersetzungen konnten nicht geladen werden: " + cause.getMessage(), com.jdimension.jlawyer.client.utils.DesktopUtils.POPUP_TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void setState(String iconPath, String text, String toolTip) {
        java.net.URL url = getClass().getResource(iconPath);
        this.setIcon(url != null ? new ImageIcon(url) : null);
        this.setText(text);
        this.setToolTipText(toolTip);
        this.revalidate();
        this.repaint();
    }

    private static void onEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

}
