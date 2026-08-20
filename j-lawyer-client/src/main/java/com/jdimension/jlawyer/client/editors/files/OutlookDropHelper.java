package com.jdimension.jlawyer.client.editors.files;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * Handles drag and drop of emails from Microsoft Outlook. Outlook provides
 * emails via Windows-native OLE formats (FileGroupDescriptorW / FileContents)
 * rather than Java's standard javaFileListFlavor.
 */
public class OutlookDropHelper {

    private static final Logger log = Logger.getLogger(OutlookDropHelper.class.getName());

    // FILEDESCRIPTORW: 72 bytes header + 520 bytes filename (260 WCHAR) = 592
    private static final int FILE_DESCRIPTOR_W_SIZE = 592;
    // FILEDESCRIPTORA: 72 bytes header + 260 bytes filename (260 CHAR) = 332
    private static final int FILE_DESCRIPTOR_A_SIZE = 332;
    // cFileName offset in both FILEDESCRIPTORW and FILEDESCRIPTORA
    private static final int FILE_NAME_OFFSET = 72;
    private static final int MAX_FILE_NAME_LENGTH = 260;

    /**
     * Checks if the given DataFlavors indicate an Outlook email drop
     * (FileGroupDescriptorW or FileGroupDescriptor present).
     */
    public static boolean isOutlookDrop(DataFlavor[] flavors) {
        return findFlavorByName(flavors, "FileGroupDescriptorW") != null
                || findFlavorByName(flavors, "FileGroupDescriptor") != null;
    }

    /**
     * Extracts files from an Outlook drag-and-drop transferable and saves them
     * as temporary files that can be processed by UploadDocumentsAction.
     *
     * @param transferable the drop transferable
     * @return list of temporary files, or empty list if extraction fails
     */
    public static List<File> extractOutlookFiles(Transferable transferable) {
        List<File> tempFiles = new ArrayList<>();
        DataFlavor[] flavors = transferable.getTransferDataFlavors();

        try {
            // Try Unicode (W) descriptor first, fall back to ANSI
            DataFlavor descriptorFlavor = findFlavorByName(flavors, "FileGroupDescriptorW");
            boolean unicode = true;
            if (descriptorFlavor == null) {
                descriptorFlavor = findFlavorByName(flavors, "FileGroupDescriptor");
                unicode = false;
            }

            if (descriptorFlavor == null) {
                log.error("No FileGroupDescriptor flavor found in Outlook drop");
                return tempFiles;
            }

            // Parse filenames from the descriptor
            byte[] descriptorData = readTransferBytes(transferable, descriptorFlavor);
            List<String> fileNames = unicode
                    ? parseFileGroupDescriptorW(descriptorData)
                    : parseFileGroupDescriptorA(descriptorData);

            if (fileNames.isEmpty()) {
                log.error("No filenames found in FileGroupDescriptor");
                return tempFiles;
            }

            log.info("Outlook drop: " + fileNames.size() + " file(s) detected");

            // Collect all FileContents flavors
            List<DataFlavor> contentFlavors = new ArrayList<>();
            for (DataFlavor f : flavors) {
                String name = f.getHumanPresentableName();
                if (name != null && name.startsWith("FileContents")) {
                    contentFlavors.add(f);
                }
            }

            if (contentFlavors.isEmpty()) {
                log.error("No FileContents flavor found in Outlook drop");
                return tempFiles;
            }

            if (contentFlavors.size() >= fileNames.size()) {
                // One content flavor per file
                for (int i = 0; i < fileNames.size(); i++) {
                    File tempFile = extractSingleFile(transferable, contentFlavors.get(i), fileNames.get(i));
                    if (tempFile != null) {
                        tempFiles.add(tempFile);
                    }
                }
            } else {
                // Only one FileContents flavor available - extract first file
                if (fileNames.size() > 1) {
                    log.warn("Outlook drop: " + fileNames.size() + " files in descriptor but only "
                            + contentFlavors.size() + " FileContents flavor(s) - extracting first file only");
                }
                File tempFile = extractSingleFile(transferable, contentFlavors.get(0), fileNames.get(0));
                if (tempFile != null) {
                    tempFiles.add(tempFile);
                }
            }

        } catch (Exception ex) {
            log.error("Error extracting Outlook drop data", ex);
        }

        return tempFiles;
    }

    private static File extractSingleFile(Transferable transferable, DataFlavor contentFlavor, String fileName) {
        try {
            byte[] contentData = readTransferBytes(transferable, contentFlavor);
            if (contentData != null && contentData.length > 0) {
                return createTempFile(fileName, contentData);
            }
        } catch (Exception ex) {
            log.error("Error extracting file: " + fileName, ex);
        }
        return null;
    }

    static List<String> parseFileGroupDescriptorW(byte[] data) {
        List<String> names = new ArrayList<>();
        if (data == null || data.length < 4) {
            return names;
        }

        int count = readInt32LE(data, 0);
        int offset = 4;

        for (int i = 0; i < count; i++) {
            if (offset + FILE_DESCRIPTOR_W_SIZE > data.length) {
                break;
            }

            int nameStart = offset + FILE_NAME_OFFSET;
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < MAX_FILE_NAME_LENGTH; j++) {
                int idx = nameStart + j * 2;
                if (idx + 1 >= data.length) {
                    break;
                }
                char c = (char) ((data[idx] & 0xFF) | ((data[idx + 1] & 0xFF) << 8));
                if (c == 0) {
                    break;
                }
                sb.append(c);
            }
            if (sb.length() > 0) {
                names.add(sb.toString());
            }
            offset += FILE_DESCRIPTOR_W_SIZE;
        }
        return names;
    }

    static List<String> parseFileGroupDescriptorA(byte[] data) {
        List<String> names = new ArrayList<>();
        if (data == null || data.length < 4) {
            return names;
        }

        int count = readInt32LE(data, 0);
        int offset = 4;

        for (int i = 0; i < count; i++) {
            if (offset + FILE_DESCRIPTOR_A_SIZE > data.length) {
                break;
            }

            int nameStart = offset + FILE_NAME_OFFSET;
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < MAX_FILE_NAME_LENGTH; j++) {
                int idx = nameStart + j;
                if (idx >= data.length) {
                    break;
                }
                char c = (char) (data[idx] & 0xFF);
                if (c == 0) {
                    break;
                }
                sb.append(c);
            }
            if (sb.length() > 0) {
                names.add(sb.toString());
            }
            offset += FILE_DESCRIPTOR_A_SIZE;
        }
        return names;
    }

    private static DataFlavor findFlavorByName(DataFlavor[] flavors, String name) {
        if (flavors == null) {
            return null;
        }
        for (DataFlavor f : flavors) {
            if (name.equalsIgnoreCase(f.getHumanPresentableName())) {
                return f;
            }
        }
        return null;
    }

    private static byte[] readTransferBytes(Transferable transferable, DataFlavor flavor) throws Exception {
        Object data = transferable.getTransferData(flavor);
        if (data instanceof InputStream) {
            return readAllBytes((InputStream) data);
        } else if (data instanceof byte[]) {
            return (byte[]) data;
        } else {
            log.error("Unexpected transfer data type for " + flavor.getHumanPresentableName()
                    + ": " + (data != null ? data.getClass().getName() : "null"));
            return null;
        }
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        is.close();
        return baos.toByteArray();
    }

    private static int readInt32LE(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }

    private static File createTempFile(String fileName, byte[] data) {
        try {
            String ext = ".tmp";
            String baseName = fileName;
            int dotIdx = fileName.lastIndexOf('.');
            if (dotIdx >= 0) {
                ext = fileName.substring(dotIdx);
                baseName = fileName.substring(0, dotIdx);
            }

            // Sanitize for file system but preserve umlauts
            baseName = baseName.replaceAll("[^\\w äöüÄÖÜß.\\-]", "_");
            if (baseName.length() < 3) {
                baseName = baseName + "_outlook";
            }

            File tempFile = File.createTempFile(baseName + "_", ext);
            tempFile.deleteOnExit();

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(data);
            }

            log.info("Outlook drop: created temp file " + tempFile.getAbsolutePath() + " for " + fileName);
            return tempFile;
        } catch (IOException ex) {
            log.error("Error creating temp file for: " + fileName, ex);
            return null;
        }
    }
}
