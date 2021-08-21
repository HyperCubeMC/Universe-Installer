package net.hypercubemc.universe_installer;

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

        Installer installer = new Installer();
        propertiesPath = installer.getStorageDirectory().resolve("universe.properties");
        properties = new Properties();
    }

    public boolean getUseCustomLoader() {
        return useCustomLoader;
    }

    public void setUseCustomLoader(boolean useCustomLoader) {
        this.useCustomLoader = useCustomLoader;
    }

    public String getSelectedEditionName() {
        return selectedEditionName;
    }

    public String getSelectedEditionDisplayName() {
        return selectedEditionDisplayName;
    }

    public String getSelectedVersion() {
        return selectedVersion;
    }

    public Path getCustomInstallDir() {
        return customInstallDir;
    }

    public void setSelectedEditionName(String s) {
        selectedEditionName = s;
    }

    public void setSelectedEditionDisplayName(String s) {
        selectedEditionDisplayName = s;
    }

    public void setCustomInstallDir(Path p) {
        customInstallDir = p;
    }

    public void setSelectedVersion(String s) {
        selectedVersion = s;
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
            e.printStackTrace();
        }
    }

    public void load() {
        try {
            properties.load(new FileReader(propertiesPath.toFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setSelectedEditionDisplayName(properties.getProperty("selected-edition-display-name"));
        setSelectedEditionName(properties.getProperty("selected-edition-name"));
        setSelectedVersion(properties.getProperty("selected-version"));
        setCustomInstallDir(Paths.get(properties.getProperty("custom-install-dir")));
        setUseCustomLoader(Boolean.parseBoolean(properties.getProperty("use-custom-loader")));
    }
}
