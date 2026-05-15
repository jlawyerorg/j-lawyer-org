/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 */
package com.jdimension.jlawyer.client.utils;

import com.jdimension.jlawyer.client.settings.ClientSettings;
import java.io.File;
import javax.swing.JFileChooser;

/**
 *
 * @author jens
 */
public final class FileChooserUtils {

    private FileChooserUtils() {
    }

    public static JFileChooser createFileChooser() {
        return createFileChooser(ClientSettings.CONF_FILECHOOSER_LASTDIR);
    }

    public static JFileChooser createFileChooser(String lastDirectoryKey) {
        File startDirectory = getStartDirectory(lastDirectoryKey);
        if (startDirectory == null) {
            return new JFileChooser();
        }
        return new JFileChooser(startDirectory);
    }

    public static String getStartDirectoryPath(String lastDirectoryKey) {
        File startDirectory = getStartDirectory(lastDirectoryKey);
        if (startDirectory == null) {
            File currentDirectory = new JFileChooser().getCurrentDirectory();
            if (currentDirectory != null) {
                return currentDirectory.getAbsolutePath();
            }
            return new File(".").getAbsolutePath();
        }
        return startDirectory.getAbsolutePath();
    }

    public static void configureStartDirectory(JFileChooser chooser) {
        configureStartDirectory(chooser, ClientSettings.CONF_FILECHOOSER_LASTDIR);
    }

    public static void configureStartDirectory(JFileChooser chooser, String lastDirectoryKey) {
        File startDirectory = getStartDirectory(lastDirectoryKey);
        if (startDirectory != null) {
            chooser.setCurrentDirectory(startDirectory);
        }
    }

    public static void rememberDirectory(JFileChooser chooser) {
        rememberDirectory(ClientSettings.CONF_FILECHOOSER_LASTDIR, chooser);
    }

    public static void rememberDirectory(String lastDirectoryKey, JFileChooser chooser) {
        File directory = getSelectedDirectory(chooser);
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }

        ClientSettings settings = ClientSettings.getInstance();
        if (lastDirectoryKey != null) {
            settings.setConfiguration(lastDirectoryKey, directory.getAbsolutePath());
        }
        if (isSharedLastDirectoryKey(lastDirectoryKey)) {
            settings.setConfiguration(ClientSettings.CONF_FILECHOOSER_LASTDIR, directory.getAbsolutePath());
        }
    }

    private static File getStartDirectory(String lastDirectoryKey) {
        ClientSettings settings = ClientSettings.getInstance();
        File startDirectory = getConfiguredDirectory(settings, lastDirectoryKey);
        if (startDirectory == null && isSharedLastDirectoryKey(lastDirectoryKey)) {
            startDirectory = getConfiguredDirectory(settings, ClientSettings.CONF_FILECHOOSER_LASTDIR);
        }
        if (startDirectory == null) {
            startDirectory = getFallbackDirectory();
        }
        return startDirectory;
    }

    private static File getConfiguredDirectory(ClientSettings settings, String key) {
        if (key == null) {
            return null;
        }
        String path = settings.getConfiguration(key, null);
        if (path == null || "".equals(path.trim())) {
            return null;
        }

        File directory = new File(path);
        return directory.exists() && directory.isDirectory() ? directory : null;
    }

    private static File getSelectedDirectory(JFileChooser chooser) {
        if (chooser == null) {
            return null;
        }

        File selectedFile = chooser.getSelectedFile();
        if (chooser.getFileSelectionMode() == JFileChooser.DIRECTORIES_ONLY) {
            if (selectedFile != null && selectedFile.exists() && selectedFile.isDirectory()) {
                return selectedFile;
            }
            return chooser.getCurrentDirectory();
        }
        if (selectedFile == null) {
            File[] selectedFiles = chooser.getSelectedFiles();
            if (selectedFiles != null && selectedFiles.length > 0) {
                selectedFile = selectedFiles[0];
            }
        }
        if (selectedFile == null) {
            selectedFile = chooser.getCurrentDirectory();
        }

        if (selectedFile == null) {
            return null;
        }
        return selectedFile.isDirectory() ? selectedFile : selectedFile.getParentFile();
    }

    private static boolean isSharedLastDirectoryKey(String lastDirectoryKey) {
        return lastDirectoryKey == null || ClientSettings.CONF_FILECHOOSER_LASTDIR.equals(lastDirectoryKey);
    }

    private static File getFallbackDirectory() {
        File home = new File(System.getProperty("user.home", "."));
        if (home.exists() && home.isDirectory()) {
            return home;
        }

        File currentDirectory = new File(".").getAbsoluteFile();
        return currentDirectory.exists() && currentDirectory.isDirectory() ? currentDirectory : null;
    }
}
