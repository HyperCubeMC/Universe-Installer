package net.hypercubemc.universe_installer;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import net.fabricmc.installer.Main;
import net.fabricmc.installer.util.MetaHandler;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;
import net.hypercubemc.universe_installer.layouts.VerticalLayout;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.Depth;
import org.json.JSONException;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;

public class Installer {
    InstallerMeta INSTALLER_META;
    List<InstallerMeta.Edition> EDITIONS;
    List<String> GAME_VERSIONS;
    String META_URL = "https://raw.githubusercontent.com/HyperCubeMC/Universe-Installer-Files/master/meta.json";
    String REPO_URL = "https://github.com/HyperCubeMC/Universe-Installer-Files.git";

    InstallerMeta.Edition selectedEdition;
    String selectedVersion;

    JButton button;
    JComboBox<String> editionDropdown;
    JComboBox<String> versionDropdown;
    JButton installDirectoryPicker;
    JCheckBox useCustomLoaderCheckbox;
    JProgressBar progressBar;

    UniverseConfig config;
    public static Installer INSTANCE;
    public JFrame frame;
    boolean finishedSuccessfulInstall = false;

    public Installer() {

    }

    public static void main(String[] args) {
        System.out.println("Launching installer...");
        INSTANCE = new Installer();
        INSTANCE.config = new UniverseConfig();
        INSTANCE.start();
    }

    public void start() {
        boolean dark = DarkModeDetector.isDarkMode();
        System.setProperty("apple.awt.application.appearance", "system");
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }

        // JGit now depends on Java 11+
        if (Float.parseFloat(System.getProperty("java.specification.version")) < 11) {
            JLabel label = new JLabel();
            Font font = label.getFont();

            String style = "font-family:" + font.getFamily() + ";" + "font-weight:" + (font.isBold() ? "bold" : "normal") + ";" +
                    "font-size:" + font.getSize() + "pt;";

            JEditorPane message = new JEditorPane("text/html", "<html><body style=\"" + style + "\">"
                    + "This program requires Java 11 or above to run. Please install an updated Java release from <a href=\"https://adoptium.net/\">https://adoptium.net</a>"
                    + "</body></html>");

            message.addHyperlinkListener(e -> {
                if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (IOException | URISyntaxException ignored) {}
                }
            });
            message.setEditable(false);
            message.setBackground(label.getBackground());
            JOptionPane.showMessageDialog(null, message, "Outdated Java", JOptionPane.ERROR_MESSAGE);
            return;
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

        INSTALLER_META = new InstallerMeta(META_URL);
        try {
            INSTALLER_META.load();
        } catch (IOException e) {
            System.out.println("Failed to fetch installer metadata from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "The installer was unable to fetch metadata from the server, please check your internet connection and try again later.", "Please check your internet connection!", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (JSONException e) {
            System.out.println("Failed to parse installer metadata!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Installer metadata parsing failed, please contact Justsnoopy30! \nError: " + e, "Metadata Parsing Failed!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        GAME_VERSIONS = INSTALLER_META.getGameVersions();
        EDITIONS = INSTALLER_META.getEditions();

        frame = new JFrame("Universe Installer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(350,350);
        frame.setLocationRelativeTo(null); // Centers the window
        frame.setIconImage(new ImageIcon(Objects.requireNonNull(Utils.class.getClassLoader().getResource("universe_profile_icon.png"))).getImage()); // Credit to Pixxi#0001 for the icon

        config.load();

        JPanel topPanel = new JPanel(new VerticalLayout());

        JPanel editionPanel = new JPanel();

        JLabel editionDropdownLabel = new JLabel("Select Edition:");
        editionDropdownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        selectedEdition = EDITIONS.get(0);

        String[] editionDisplayNameList = EDITIONS.stream().map(edition -> edition.displayName).toArray(String[]::new);
        editionDropdown = new JComboBox<>(editionDisplayNameList);
        editionDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedEdition = EDITIONS.get(editionDropdown.getSelectedIndex());
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
        selectedVersion = gameVersionList[0];

        versionDropdown = new JComboBox<>(gameVersionList);
        versionDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedVersion = (String) e.getItem();

                readyAll();
            }
        });

        versionPanel.add(versionDropdownLabel);
        versionPanel.add(versionDropdown);

        JPanel installDirectoryPanel = new JPanel();

        JLabel installDirectoryPickerLabel = new JLabel("Select Install Directory:");
        installDirectoryPickerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        installDirectoryPicker = new JButton(config.getCustomInstallDir() != null ? config.getCustomInstallDir().toFile().getName() : getDefaultInstallDir().toFile().getName());
        installDirectoryPicker.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setFileHidingEnabled(false);
            int option = fileChooser.showOpenDialog(frame);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (file.toPath().equals(getDefaultInstallDir())) config.setCustomInstallDir(null);
                else config.setCustomInstallDir(file.toPath());
                installDirectoryPicker.setText(file.getName());

                readyAll();
            }
        });

        installDirectoryPanel.add(installDirectoryPickerLabel);
        installDirectoryPanel.add(installDirectoryPicker);

        useCustomLoaderCheckbox = new JCheckBox("Use Custom Loader (Recommended)", config.shouldUseCustomLoader());
        useCustomLoaderCheckbox.setToolTipText("If you do not know what this does, leave it checked.");
        useCustomLoaderCheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
        useCustomLoaderCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
        useCustomLoaderCheckbox.addActionListener(e -> {
        config.setUseCustomLoader(useCustomLoaderCheckbox.isSelected());
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
            if (!selectedEdition.compatibleVersions.contains(selectedVersion)) {
                JOptionPane.showMessageDialog(frame, "The selected edition is not compatible with the chosen game version.", "Incompatible Edition", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean useCustomLoader = config.shouldUseCustomLoader();

            String loaderName = useCustomLoader ? "universe-fabric-loader" : "fabric-loader";

            try {
                URL customLoaderVersionUrl = new URL("https://raw.githubusercontent.com/HyperCubeMC/Universe-Installer-Maven/master/latest-loader");
                String loaderVersion = useCustomLoader ? Utils.readTextFile(customLoaderVersionUrl) : Main.LOADER_META.getLatestVersion(false).getVersion();
                boolean success = VanillaLauncherIntegration.installToLauncher(getVanillaGameDir(), getInstallDir(), useCustomLoader ? selectedEdition.displayName : "Fabric Loader " + selectedVersion, selectedVersion, loaderName, loaderVersion, useCustomLoader ? VanillaLauncherIntegration.Icon.UNIVERSE : VanillaLauncherIntegration.Icon.FABRIC);
                if (!success) {
                    System.out.println("Failed to install to launcher, canceling!");
                    return;
                }
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

            Downloader downloader = new Downloader(REPO_URL, getStorageDirectory().resolve("repo"));
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
                    if (!installDir.exists() || !installDir.isDirectory()) installDir.mkdirs();

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

                    boolean installSuccess = installFromPack(getStorageDirectory().resolve("repo").resolve(selectedVersion).resolve(selectedEdition.name).toFile());
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
        private final String repo;
        private final Path path;

        public Downloader(String repo, Path path) {
            this.repo = repo;
            this.path = path;
        }

        @Override
        protected Void doInBackground() throws Exception {
            ProgressMonitor progressMonitor = new ProgressMonitor() {
                int totalWork = 0;
                int workDone = 0;

                boolean receivingCompleted = false;

                @Override
                public void start(int totalTasks) {
                    System.out.println("Starting work on tasks");
                }

                @Override
                public void beginTask(String title, int totalWork) {
                    System.out.println("Start " + title + ": " + totalWork);
                    // This is the first task endTask is called for, and the only one needed for download progress
                    if (title.startsWith("Receiving")) this.totalWork = totalWork;
                    workDone = 0;
                }

                @Override
                public void update(int completed) {
                    if (receivingCompleted) return;

                    workDone += completed;
                    if (workDone < totalWork) setProgress((int) ((double) workDone * 100 / totalWork));
                }

                @Override
                public void endTask() {
                    workDone = 0;
                    if (!receivingCompleted) receivingCompleted = true;
                    System.out.println("Done");
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }
            };

            if (repositoryExists(path.toFile())) {
                Git.open(path.toFile()).pull()
                        .setProgressMonitor(progressMonitor)
                        .call();
            } else {
                if (path.toFile().exists()) deleteDirectory(path.toFile());

                Git.cloneRepository()
                        .setURI(repo)
                        .setDirectory(path.toFile())
                        .setProgressMonitor(progressMonitor)
                        .setDepth(new Depth(1))
                        .call();
            }
            setProgress(100);

            return null;
        }
    }

    public boolean installFromPack(File pack) {
        try {
            File[] files = pack.listFiles();

            if (files == null) {
                return false;
            }

            installFiles(files);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void installFiles(File[] files) throws IOException {
        for (File entry : files) {
            String entryPath = getStorageDirectory().resolve("repo").resolve(selectedVersion).resolve(selectedEdition.name).relativize(entry.toPath()).toString();

            if (config.shouldUseCustomLoader() && entryPath.startsWith("mods" + File.separator)) {
                entryPath = entryPath.replace("mods" + File.separator, "universe-reserved" + File.separator);
            }

            File filePath = getInstallDir().resolve(entryPath).toFile();
            if (!entry.isDirectory()) {
                Files.copy(entry.toPath(), filePath.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                // if the entry is a directory, make the directory
                filePath.mkdir();
                File[] subFiles = entry.listFiles();
                if (subFiles != null) installFiles(subFiles);
            }
        }
    }

    public boolean repositoryExists(File directory) {
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        repositoryBuilder.findGitDir(directory);

        return repositoryBuilder.getGitDir() != null;
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

    public Path getStorageDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        String storageDirName = os.contains("mac") ? "universe-installer" : ".universe-installer";

        return getAppDataDirectory().resolve(storageDirName);
    }

    public Path getDefaultInstallDir() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("mac") && selectedEdition.defaultInstallDir.startsWith(".")) {
            return getAppDataDirectory().resolve(selectedEdition.defaultInstallDir.replaceAll("/", Matcher.quoteReplacement(File.separator)).substring(1));
        } else {
            return getAppDataDirectory().resolve(selectedEdition.defaultInstallDir.replaceAll("/", Matcher.quoteReplacement(File.separator)));
        }
    }

    public Path getInstallDir() {
        return config.getCustomInstallDir() != null ? config.getCustomInstallDir() : getDefaultInstallDir();
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
