package net.hypercubemc.universe_installer;
import com.formdev.flatlaf.FlatLightLaf;
import net.fabricmc.installer.Main;
import net.fabricmc.installer.util.MetaHandler;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;
import net.hypercubemc.universe_installer.layouts.VerticalLayout;
import org.json.JSONException;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Installer {
    InstallerMeta INSTALLER_META;
    List<InstallerMeta.Edition> EDITIONS;
    List<String> GAME_VERSIONS;
    String BASE_URL = "https://raw.githubusercontent.com/HyperCubeMC/Universe-Installer-Files/master/";

    JButton button;
    JComboBox<String> editionDropdown;
    JComboBox<String> versionDropdown;
    JButton installDirectoryPicker;
    JCheckBox useCustomLoaderCheckbox;
    JProgressBar progressBar;

    UniverseConfig config;
    public static Installer INSTANCE;
    public JFrame frame = new JFrame("Universe Installer");
    boolean finishedSuccessfulInstall = false;
    boolean useCustomLoader;

    public Installer() {

    }

    public static void main(String[] args) {
        System.out.println("Launching installer...");
        INSTANCE = new Installer();
        INSTANCE.config = new UniverseConfig();
        INSTANCE.start();
    }

    public void start() {
        FlatLightLaf.install();

        config.load();
        useCustomLoader  = config.getUseCustomLoader();
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.out.println("Failed to set UI theme!");
            e.printStackTrace();
        }

        Main.LOADER_META = new MetaHandler(Reference.getMetaServerEndpoint("v2/versions/loader"));
        try {
            Main.LOADER_META.load();
        } catch (Exception e) {
            System.out.println("Failed to fetch fabric version info from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "The installer was unable to fetch fabric version info from the server, please check your internet connection and try again later.", "Please check your internet connection!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        INSTALLER_META = new InstallerMeta(BASE_URL + "meta.json");
        try {
            INSTALLER_META.load();
        } catch (IOException e) {
            System.out.println("Failed to fetch installer metadata from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "The installer was unable to fetch metadata from the server, please check your internet connection and try again later.", "Please check your internet connection!", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (JSONException e) {
            System.out.println("Failed to fetch installer metadata from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Installer metadata parsing failed, please contact Justsnoopy30! \nError: " + e, "Metadata Parsing Failed!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        GAME_VERSIONS = INSTALLER_META.getGameVersions();
        EDITIONS = INSTALLER_META.getEditions();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(350,350);
        frame.setLocationRelativeTo(null); // Centers the window
        frame.setIconImage(new ImageIcon(Objects.requireNonNull(Utils.class.getClassLoader().getResource("universe_profile_icon.png"))).getImage()); // Credit to Pixxi#0001 for the icon

        JPanel topPanel = new JPanel(new VerticalLayout());

        JPanel editionPanel = new JPanel();

        JLabel editionDropdownLabel = new JLabel("Select Edition:");
        editionDropdownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        List<String> editionNames = new ArrayList<>();
        List<String> editionDisplayNames = new ArrayList<>();
        for (InstallerMeta.Edition edition : EDITIONS) {
            editionNames.add(edition.name);
            editionDisplayNames.add(edition.displayName);
        }
        String[] editionNameList = editionNames.toArray(new String[0]);
        config.setSelectedEditionName(config.getSelectedEditionName() == null ? config.getSelectedEditionName() : editionNameList[0]);
        String[] editionDisplayNameList = editionDisplayNames.toArray(new String[0]);
        config.setSelectedEditionDisplayName(config.getSelectedEditionDisplayName() == null ? editionDisplayNameList[0] : config.getSelectedEditionDisplayName());

        editionDropdown = new JComboBox<>(editionDisplayNameList);
        editionDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                config.setSelectedEditionName(editionNameList[editionDropdown.getSelectedIndex()]);
                config.setSelectedEditionDisplayName((String) e.getItem());
                if (config.getCustomInstallDir() == null) {
                    installDirectoryPicker.setText(getDefaultInstallDir().toFile().getName());
                }

                readyAll();
            }
        });

        editionPanel.add(editionDropdownLabel);
        editionPanel.add(editionDropdown);

        JPanel versionPanel = new JPanel();

        JLabel versionDropdownLabel = new JLabel("Select Game Version:");
        versionDropdownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        List<String> gameVersions = GAME_VERSIONS.subList(0, GAME_VERSIONS.size()); // Clone the list
        Collections.reverse(gameVersions); // Reverse the order of the list so that the latest version is on top and older versions downward
        String[] gameVersionList = gameVersions.toArray(new String[0]);
        config.setSelectedVersion(config.getSelectedVersion() == null ? gameVersionList[0] : config.getSelectedVersion());

        versionDropdown = new JComboBox<>(gameVersionList);
        versionDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                config.setSelectedVersion((String) e.getItem());

                readyAll();
            }
        });

        versionPanel.add(versionDropdownLabel);
        versionPanel.add(versionDropdown);

        JPanel installDirectoryPanel = new JPanel();

        JLabel installDirectoryPickerLabel = new JLabel("Select Install Directory:");
        installDirectoryPickerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        installDirectoryPicker = new JButton(getDefaultInstallDir().toFile().getName());
        installDirectoryPicker.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setFileHidingEnabled(false);
            int option = fileChooser.showOpenDialog(frame);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                config.setCustomInstallDir(file.toPath());
                installDirectoryPicker.setText(file.getName());

                readyAll();
            }
        });

        installDirectoryPanel.add(installDirectoryPickerLabel);
        installDirectoryPanel.add(installDirectoryPicker);

        useCustomLoaderCheckbox = new JCheckBox("Use Custom Loader (Recommended)", true);
        useCustomLoaderCheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
        useCustomLoaderCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
        useCustomLoaderCheckbox.addActionListener(e -> {
            useCustomLoader = useCustomLoaderCheckbox.isSelected();
            readyAll();
        });

        topPanel.add(editionPanel);
        topPanel.add(versionPanel);
        topPanel.add(installDirectoryPanel);
        topPanel.add(useCustomLoaderCheckbox);

        JPanel bottomPanel = new JPanel();

        progressBar = new JProgressBar();
        progressBar.setValue(0);
        progressBar.setMaximum(100);
        progressBar.setStringPainted(true);

        button = new JButton("Install");
        button.addActionListener(action -> {
            if (!EDITIONS.stream().filter(edition -> edition.name.equals(config.getSelectedEditionName())).findFirst().get().compatibleVersions.contains(config.getSelectedVersion())) {
                JOptionPane.showMessageDialog(frame, "The selected edition is not compatible with the chosen game version.", "Incompatible Edition", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Use Universe's custom fabric loader if "use custom loader" is set
            if (useCustomLoader) {
                Reference.metaServerUrl = "https://raw.githubusercontent.com/HyperCubeMC/Universe-Installer-Maven/master/";
                System.out.println("Using custom loader");
            } else {
                Reference.metaServerUrl = "https://meta.fabricmc.net/";
                System.out.println("Using fabric loader");
            }

            String loaderName = useCustomLoader ? "universe-fabric-loader" : "fabric-loader";

            try {
                URL customLoaderVersionUrl = new URL("https://raw.githubusercontent.com/HyperCubeMC/Universe-Installer-Maven/master/latest-loader");
                String loaderVersion = useCustomLoader ? Utils.readTextFile(customLoaderVersionUrl) : Main.LOADER_META.getLatestVersion(false).getVersion();
                VanillaLauncherIntegration.installToLauncher(getVanillaGameDir(), getInstallDir(), useCustomLoader ? config.getSelectedEditionDisplayName() : "Fabric Loader " + config.getSelectedVersion(), config.getSelectedVersion(), loaderName, loaderVersion, useCustomLoader ? VanillaLauncherIntegration.Icon.UNIVERSE : VanillaLauncherIntegration.Icon.FABRIC);
            } catch (IOException e) {
                System.out.println("Failed to install version and profile to vanilla launcher!");
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to install to vanilla launcher, please contact Justsnoopy30! \nError: " + e, "Failed to install to launcher", JOptionPane.ERROR_MESSAGE);
                return;
            }

            File storageDir = getStorageDirectory().toFile();
            if (!storageDir.exists() || !storageDir.isDirectory()) {
                storageDir.mkdir();
            }

            button.setText("Downloading...");
            progressBar.setValue(0);
            setInteractionEnabled(false);

            String zipName = config.getSelectedEditionName() + ".zip";

            String downloadURL = BASE_URL + config.getSelectedVersion() + "/" + zipName;

            File saveLocation = getStorageDirectory().resolve(zipName).toFile();

            final Downloader downloader = new Downloader(downloadURL, saveLocation);
            downloader.addPropertyChangeListener(event -> {
                if ("progress".equals(event.getPropertyName())) {
                    progressBar.setValue((Integer) event.getNewValue());
                } else if (event.getNewValue() == SwingWorker.StateValue.DONE) {
                    try {
                        downloader.get();
                    } catch (InterruptedException | ExecutionException e) {
                        System.out.println("Failed to download zip!");
                        e.getCause().printStackTrace();

                        String msg = String.format("An error occurred while attempting to download the required files, please check your internet connection and try again! \nError: %s",
                                e.getCause().toString());
                        JOptionPane.showMessageDialog(frame,
                                msg, "Download Failed!", JOptionPane.ERROR_MESSAGE, null);
                        readyAll();

                        return;
                    }

                    button.setText("Download completed!");

                    File installDir = getInstallDir().toFile();
                    if (!installDir.exists() || !installDir.isDirectory()) installDir.mkdir();

                    File modsFolder = getInstallDir().resolve(useCustomLoader ? "universe-reserved" : "mods").toFile();
                    File[] modsFolderContents = modsFolder.listFiles();
                    if (modsFolderContents != null) {
                        boolean isEmpty = modsFolderContents.length == 0;

                        if (!useCustomLoader && modsFolder.exists() && modsFolder.isDirectory() && !isEmpty) {
                            int result = JOptionPane.showConfirmDialog(frame,"An existing mods folder was found in the selected game directory. Do you want to delete all existing mods before installation to prevent version conflicts? (Unless you know exactly what you are doing and how to solve the conflicts, press yes)", "Mods Folder Detected",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.QUESTION_MESSAGE);
                            if (result == JOptionPane.YES_OPTION) {
                                deleteDirectory(modsFolder);
                            }
                        }
                    }

                    if (useCustomLoader) deleteDirectory(modsFolder);
                    if (!modsFolder.exists() || !modsFolder.isDirectory()) modsFolder.mkdir();

                    boolean installSuccess = installFromZip(saveLocation);
                    if (installSuccess) {
                        button.setText("Installation succeeded!");
                        finishedSuccessfulInstall = true;
                        editionDropdown.setEnabled(true);
                        versionDropdown.setEnabled(true);
                        installDirectoryPicker.setEnabled(true);
                        useCustomLoaderCheckbox.setEnabled(true);
                        config.save();
                    } else {
                        button.setText("Installation failed!");
                        System.out.println("Failed to install to mods folder!");
                        JOptionPane.showMessageDialog(frame, "Failed to install to mods folder, please make sure your game is closed and try again!", "Installation Failed!", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            downloader.execute();
        });

        bottomPanel.add(progressBar);
        bottomPanel.add(button);

        frame.getContentPane().add(topPanel, BorderLayout.NORTH);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        frame.setVisible(true);

        System.out.println("Launched!");
    }

    // Works up to 2GB because of long limitation
    class Downloader extends SwingWorker<Void, Void> {
        private final String url;
        private final File file;

        public Downloader(String url, File file) {
            this.url = url;
            this.file = file;
        }

        @Override
        protected Void doInBackground() throws Exception {
            URL url = new URL(this.url);
            HttpsURLConnection connection = (HttpsURLConnection) url
                    .openConnection();
            long filesize = connection.getContentLengthLong();
            if (filesize == -1) {
                throw new Exception("Content length must not be -1 (unknown)!");
            }
            long totalDataRead = 0;
            try (java.io.BufferedInputStream in = new java.io.BufferedInputStream(
                    connection.getInputStream())) {
                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                try (java.io.BufferedOutputStream bout = new BufferedOutputStream(
                        fos, 1024)) {
                    byte[] data = new byte[1024];
                    int i;
                    while ((i = in.read(data, 0, 1024)) >= 0) {
                        totalDataRead = totalDataRead + i;
                        bout.write(data, 0, i);
                        int percent = (int) ((totalDataRead * 100) / filesize);
                        setProgress(percent);
                    }
                }
            }
            return null;
        }
    }

    public boolean installFromZip(File zip) {
        try {
            int BUFFER_SIZE = 2048; // Buffer Size

            ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zip));

            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                String entryName = entry.getName();

                if (useCustomLoader && entryName.startsWith("mods/")) {
                    entryName = entryName.replace("mods/", "universe-reserved/");
                }

                File filePath = getInstallDir().resolve(entryName).toFile();
                if (!entry.isDirectory()) {
                    // if the entry is a file, extracts it
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
                    byte[] bytesIn = new byte[BUFFER_SIZE];
                    int read = 0;
                    while ((read = zipIn.read(bytesIn)) != -1) {
                        bos.write(bytesIn, 0, read);
                    }
                    bos.close();
                } else {
                    // if the entry is a directory, make the directory
                    filePath.mkdir();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
            return true;
        } catch (IOException e) {
            e.getCause().printStackTrace();
            return false;
        }
    }

    public Path getStorageDirectory() {
        return this.getAppDataDirectory().resolve(getStorageDirectoryName());
    }

    public Path getInstallDir() {
        return config.getCustomInstallDir() != null ? config.getCustomInstallDir() : getDefaultInstallDir();
    }

    public Path getAppDataDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win"))
            return new File(System.getenv("APPDATA")).toPath();
        else if (os.contains("mac"))
            return new File(System.getProperty("user.home") + "/Library/Application Support").toPath();
        else if (os.contains("nux"))
            return new File(System.getProperty("user.home")).toPath();
        else
            return new File(System.getProperty("user.dir")).toPath();
    }

    public String getStorageDirectoryName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac"))
            return "universe-installer";
        else
            return ".universe-installer";
    }

    public Path getDefaultInstallDir() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("mac"))
            return getAppDataDirectory().resolve("minecraft");
        else
            return getAppDataDirectory().resolve(".minecraft");
    }

    public Path getVanillaGameDir() {
        String os = System.getProperty("os.name").toLowerCase();

        return os.contains("mac") ? getAppDataDirectory().resolve("minecraft") : getAppDataDirectory().resolve(".minecraft");
    }

    public boolean deleteDirectory(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return dir.delete();
    }

    public void setInteractionEnabled(boolean enabled) {
        editionDropdown.setEnabled(enabled);
        versionDropdown.setEnabled(enabled);
        installDirectoryPicker.setEnabled(enabled);
        useCustomLoaderCheckbox.setEnabled(enabled);
        button.setEnabled(enabled);
    }

    public void readyAll() {
        finishedSuccessfulInstall = false;
        button.setText("Install");
        progressBar.setValue(0);
        setInteractionEnabled(true);
    }

}
