package com.jdimension.jlawyer.client.editors.files;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DropMode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Modal dialog presenting a light-table style, drag-and-drop reorderable list
 * of PDF documents (preselected for merge). Shows a thumbnail + filename with
 * tooltips and scroll support. Returns the new order on accept.
 */
public class PdfReorderLightTableDialog extends JDialog {

    private final DefaultListModel<File> model = new DefaultListModel<>();
    private final JList<File> list = new JList<>(model);
    private boolean accepted = false;

    public PdfReorderLightTableDialog(Window owner, List<File> files) {
        super(owner, "Übersicht: Reihenfolge festlegen", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Build model
        for (File f : files) {
            model.addElement(f);
        }

        // Configure list
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(-1); // wrap as needed
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellWidth(220);
        list.setFixedCellHeight(320);
        list.setCellRenderer(new ThumbnailRenderer());
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new ListReorderTransferHandler());

        // Tooltips: show file name
        list.setToolTipText("");
        list.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0) {
                    File f = model.getElementAt(index);
                    list.setToolTipText(f.getName());
                } else {
                    list.setToolTipText(null);
                }
                hoverIndex = index;
                list.repaint();
            }
        });
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverIndex = -1;
                list.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index < 0) return;
                File f = model.get(index);
                java.awt.Rectangle b = list.getCellBounds(index, index);
                int relX = e.getX() - b.x;
                int w = b.width;
                int arrowZone = 30;
                if (relX <= arrowZone) {
                    // left arrow
                    int cur = pageIndexMap.getOrDefault(f.getAbsolutePath(), 0);
                    int max = pageCountMap.getOrDefault(f.getAbsolutePath(), 1);
                    if (cur > 0) {
                        pageIndexMap.put(f.getAbsolutePath(), cur - 1);
                        list.repaint(b);
                    }
                } else if (relX >= w - arrowZone) {
                    // right arrow
                    int cur = pageIndexMap.getOrDefault(f.getAbsolutePath(), 0);
                    int max = pageCountMap.getOrDefault(f.getAbsolutePath(), 1);
                    if (cur < max - 1) {
                        pageIndexMap.put(f.getAbsolutePath(), cur + 1);
                        list.repaint(b);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        // size relative to owner; updated again after pack
        Dimension baseSize = (owner != null ? owner.getSize() : java.awt.Toolkit.getDefaultToolkit().getScreenSize());
        int approxW = Math.max(600, (int) (baseSize.width * 0.9));
        int approxH = Math.max(400, (int) (baseSize.height * 0.9));
        scroll.setPreferredSize(new Dimension(approxW - 40, approxH - 120));

        // Buttons
        JButton btnAccept = new JButton("Übernehmen");
        btnAccept.addActionListener(e -> {
            accepted = true;
            dispose();
        });
        JButton btnCancel = new JButton("Abbrechen");
        btnCancel.addActionListener(e -> {
            accepted = false;
            dispose();
        });

        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
        south.add(Box.createHorizontalGlue());
        south.add(btnCancel);
        south.add(Box.createHorizontalStrut(8));
        south.add(btnAccept);

        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(south, BorderLayout.SOUTH);

        pack();
        // make dialog nearly as large as the owner
        setSize(approxW, approxH);
        setLocationRelativeTo(owner);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public List<File> getOrderedFiles() {
        List<File> out = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            out.add(model.get(i));
        }
        return out;
    }

    private int hoverIndex = -1;
    private final HashMap<String, Integer> pageIndexMap = new HashMap<>();
    private final HashMap<String, Integer> pageCountMap = new HashMap<>();
    private final HashMap<String, ImageIcon> cache = new HashMap<>();

    private class ThumbnailRenderer implements ListCellRenderer<File> {
        private final JPanel panel = new JPanel();
        private final JLabel lblThumb = new JLabel();
        private final JLabel lblName = new JLabel();
        private final JLabel lblLeft = new JLabel("◄");
        private final JLabel lblRight = new JLabel("►");

        public ThumbnailRenderer() {
            panel.setLayout(new BorderLayout(4, 4));
            lblThumb.setHorizontalAlignment(JLabel.CENTER);
            lblThumb.setCursor(new java.awt.Cursor(Cursor.HAND_CURSOR));
            lblName.setHorizontalAlignment(JLabel.CENTER);
            // arrows left/right around thumbnail
            lblLeft.setHorizontalAlignment(JLabel.CENTER);
            lblRight.setHorizontalAlignment(JLabel.CENTER);
            lblLeft.setPreferredSize(new Dimension(24, 24));
            lblRight.setPreferredSize(new Dimension(24, 24));
            lblLeft.setVisible(false);
            lblRight.setVisible(false);

            JPanel center = new JPanel(new BorderLayout());
            center.add(lblLeft, BorderLayout.WEST);
            center.add(lblThumb, BorderLayout.CENTER);
            center.add(lblRight, BorderLayout.EAST);
            panel.add(center, BorderLayout.CENTER);
            panel.add(lblName, BorderLayout.SOUTH);
        }

        @Override
        public java.awt.Component getListCellRendererComponent(JList<? extends File> list, File value, int index, boolean isSelected, boolean cellHasFocus) {
            lblName.setText(value.getName());
            int curPage = pageIndexMap.getOrDefault(value.getAbsolutePath(), 0);
            lblThumb.setIcon(getThumbnail(value, curPage));

            if (isSelected) {
                panel.setBackground(list.getSelectionBackground());
                lblName.setForeground(list.getSelectionForeground());
            } else {
                panel.setBackground(list.getBackground());
                lblName.setForeground(list.getForeground());
            }

            panel.setOpaque(true);
            boolean showArrows = (index == hoverIndex) && pageCountMap.getOrDefault(value.getAbsolutePath(), 1) > 1;
            lblLeft.setVisible(showArrows);
            lblRight.setVisible(showArrows);
            return panel;
        }

        private ImageIcon getThumbnail(File file, int pageIndex) {
            String key = file.getAbsolutePath() + "#" + pageIndex;
            ImageIcon cachedIcon = cache.get(key);
            if (cachedIcon != null) return cachedIcon;
            try (PDDocument document = PDDocument.load(file)) {
                if (!pageCountMap.containsKey(file.getAbsolutePath())) {
                    pageCountMap.put(file.getAbsolutePath(), document.getNumberOfPages());
                }
                PDFRenderer renderer = new PDFRenderer(document);
                BufferedImage img = renderer.renderImageWithDPI(Math.max(0, Math.min(pageIndex, document.getNumberOfPages()-1)), 72f);
                int targetH = 270; // 50% larger than original 180
                int targetW = Math.max(120, (int) ((double) img.getWidth() / img.getHeight() * targetH));
                java.awt.Image scaled = img.getScaledInstance(targetW, targetH, java.awt.Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(scaled);
                cache.put(key, icon);
                return icon;
            } catch (IOException ex) {
                return new ImageIcon();
            }
        }
    }

    private class ListReorderTransferHandler extends TransferHandler {
        private int fromIndex = -1;

        @Override
        public int getSourceActions(javax.swing.JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(javax.swing.JComponent c) {
            fromIndex = list.getSelectedIndex();
            File f = list.getSelectedValue();
            return new FileTransferable(f);
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) || support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;
            try {
                int toIndex;
                if (support.getDropLocation() instanceof javax.swing.JList.DropLocation) {
                    toIndex = ((javax.swing.JList.DropLocation) support.getDropLocation()).getIndex();
                } else {
                    toIndex = model.size();
                }
                if (fromIndex < 0 || fromIndex >= model.size()) return false;
                File moved = model.get(fromIndex);
                if (toIndex > fromIndex) toIndex--; // account for removal
                model.remove(fromIndex);
                model.add(toIndex, moved);
                list.setSelectedIndex(toIndex);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    private static class FileTransferable implements Transferable {
        private final File file;
        private final List<File> list;

        FileTransferable(File f) {
            this.file = f;
            this.list = new ArrayList<>();
            this.list.add(f);
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(DataFlavor.javaFileListFlavor) || flavor.equals(DataFlavor.stringFlavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) {
            if (flavor.equals(DataFlavor.javaFileListFlavor)) return list;
            if (flavor.equals(DataFlavor.stringFlavor)) return file.getAbsolutePath();
            return null;
        }
    }
}
