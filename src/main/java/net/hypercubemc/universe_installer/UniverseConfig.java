package net.hypercubemc.universe_installer;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class UniverseConfig {
    private Path customInstallDir;
    private boolean useCustomLoader;

    private final Path propertiesPath;
    private final Properties properties;


    public UniverseConfig() {
        customInstallDir = null;
        useCustomLoader = true;
        propertiesPath = Installer.INSTANCE.getStorageDirectory().resolve("config").resolve("universe.properties");
        properties = new Properties();
    }

    public boolean shouldUseCustomLoader() {
        return this.useCustomLoader;
    }

    public void setUseCustomLoader(boolean useCustomLoader) {
        this.useCustomLoader = useCustomLoader;
    }

    public Path getCustomInstallDir() {
        return this.customInstallDir;
    }

    public void setCustomInstallDir(Path customInstallDir) {
        this.customInstallDir = customInstallDir;
    }

    public void save() {
        File configDir = propertiesPath.getParent().toFile();
        if (!configDir.exists() || !configDir.isDirectory()) configDir.mkdir();

        if (customInstallDir != null) properties.setProperty("custom-install-dir", customInstallDir.toString());
        else properties.remove("custom-install-dir");
        if (!useCustomLoader) properties.setProperty("use-custom-loader", String.valueOf(false));
        else properties.remove("use-custom-loader");

        try {
            properties.store(new FileWriter(propertiesPath.toFile()), null);
        } catch (IOException e) {
            Installer.INSTANCE.button.setText("Config saving failed!");
            System.out.println("Failed to save config!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(Installer.INSTANCE.frame, "Failed to save config! \nError: " + e.getMessage(), "Config not saved!", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void load() {
        if (!Files.exists(propertiesPath)) return;
        try {
            properties.load(new FileReader(propertiesPath.toFile()));
        } catch (IOException e) {
            System.out.println("Failed to read config file!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(Installer.INSTANCE.frame, "Failed to read config file! \nError: " + e.getMessage(), "Failed to read config!", JOptionPane.ERROR_MESSAGE);
        }

        try {
            if (properties.getProperty("custom-install-dir") != null) setCustomInstallDir(Paths.get(properties.getProperty("custom-install-dir")));
        } catch (InvalidPathException e) {
            System.out.println("Invalid custom install directory in config file! Config value: " + properties.getProperty("custom-install-dir"));
            System.out.println("Resetting to default...");
            setCustomInstallDir(null);
            JOptionPane.showMessageDialog(Installer.INSTANCE.frame, "Invalid custom install directory in config file! \nConfig value: " + properties.getProperty("custom-install-dir") + "\nThe install directory has been reset to default.", "Invalid install directory config!", JOptionPane.ERROR_MESSAGE);
        }

        if (properties.getProperty("use-custom-loader") != null) setUseCustomLoader(Boolean.parseBoolean(properties.getProperty("use-custom-loader")));
    }
}
