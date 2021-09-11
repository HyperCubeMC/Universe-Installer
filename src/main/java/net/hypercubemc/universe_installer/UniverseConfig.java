package net.hypercubemc.universe_installer;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class UniverseConfig {
    private String selectedEditionName;
    private String selectedEditionDisplayName;
    private String selectedVersion;
    private Path customInstallDir;
    private boolean useCustomLoader;

    private final Path propertiesPath;
    private final Properties properties;


    public UniverseConfig() {
        selectedEditionName = null;
        selectedEditionDisplayName = null;
        selectedVersion = null;
        customInstallDir = null;
        useCustomLoader = true;
        propertiesPath = Installer.INSTANCE.getStorageDirectory().resolve("universe.properties");
        properties = new Properties();
    }

    @Nullable
    public boolean getUseCustomLoader() {
        return this.useCustomLoader;
    }

    public void setUseCustomLoader(boolean useCustomLoader) {
        this.useCustomLoader = useCustomLoader;
    }
    public String getSelectedEditionName() {
        return this.selectedEditionName;
    }

    public String getSelectedEditionDisplayName() {
        return this.selectedEditionDisplayName;
    }

    public String getSelectedVersion() {
        return this.selectedVersion;
    }

    public Path getCustomInstallDir() {
        return this.customInstallDir;
    }

    public void setSelectedEditionName(String selectedEditionName) {
        this.selectedEditionName = selectedEditionName;
    }

    public void setSelectedEditionDisplayName(String selectedEditionDisplayName) {
        this.selectedEditionDisplayName = selectedEditionDisplayName;
    }

    public void setCustomInstallDir(Path customInstallDir) {
        this.customInstallDir = customInstallDir;
    }

    public void setSelectedVersion(String selectedVersion) {
        this.selectedVersion = selectedVersion;
    }

    public void write() {
        properties.setProperty("selected-edition-name", selectedEditionName);
        properties.setProperty("selected-edition-display-name", selectedEditionDisplayName);
        properties.setProperty("custom-install-dir", customInstallDir.toString());
        properties.setProperty("selected-version", selectedVersion);
        properties.setProperty("use-custom-loader", String.valueOf(useCustomLoader));

        try {
            properties.store(new FileWriter(propertiesPath.toFile()), null);
        } catch (IOException e) {
            Installer.INSTANCE.button.setText("Installation failed!");
            System.out.println("Failed to install to mods folder!");
            JOptionPane.showMessageDialog(Installer.INSTANCE.frame, "Failed to install to mods folder, please make sure your game is closed and try again!", "Installation Failed!", JOptionPane.ERROR_MESSAGE);

        }
    }

    public void load() {
        try {
            properties.load(new FileReader(propertiesPath.toFile()));
            setSelectedEditionDisplayName(properties.getProperty("selected-edition-display-name"));
            setSelectedEditionName(properties.getProperty("selected-edition-name"));
            setSelectedVersion(properties.getProperty("selected-version"));
            setCustomInstallDir(Paths.get(properties.getProperty("custom-install-dir")));
            setUseCustomLoader(Boolean.parseBoolean(properties.getProperty("use-custom-loader")));
        } catch (IOException e) {
            // Do nothing here, we dont need anything
        }
    }
}
