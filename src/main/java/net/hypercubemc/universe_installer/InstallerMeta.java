package net.hypercubemc.universe_installer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class InstallerMeta {
    private final String metaUrl;
    private String defaultJvmArgs;
    private final List<String> gameVersions = new ArrayList<>();
    private final List<InstallerMeta.Edition> editions = new ArrayList<>();

    public InstallerMeta(String url) {
        this.metaUrl = url;
    }

    public void load() throws IOException, JSONException {
        JSONObject json = readJsonFromUrl(this.metaUrl);
        defaultJvmArgs = json.getString("default_jvm_args");
        json.getJSONArray("game_versions").toList().forEach(element -> gameVersions.add(element.toString()));
        json.getJSONArray("editions").forEach(object -> editions.add(new Edition((JSONObject) object)));
    }

    public String getDefaultJvmArgs() {
        return defaultJvmArgs;
    }

    public List<String> getGameVersions() {
        return gameVersions;
    }

    public List<InstallerMeta.Edition> getEditions() {
        return editions;
    }

    public static String readAll(Reader reader) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        int codePoint;
        while ((codePoint = reader.read()) != -1) {
            stringBuilder.append((char) codePoint);
        }
        return stringBuilder.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(url).openStream(), StandardCharsets.UTF_8));
        return new JSONObject(readAll(bufferedReader));
    }

    public static class Edition {
        String name;
        String displayName;
        String defaultInstallDir;
        List<String> compatibleVersions = new ArrayList<>();
        List<String> clearDirectories = new ArrayList<>();
        String jvmArgsOverride;

        public Edition(JSONObject jsonObject) {
            this.name = jsonObject.getString("name");
            this.displayName = jsonObject.getString("display_name");
            this.defaultInstallDir = jsonObject.getString("default_install_dir");

            for (int i = 0; i < jsonObject.getJSONArray("compatible_versions").toList().size(); i++){
                compatibleVersions.add(jsonObject.getJSONArray("compatible_versions").toList().get(i).toString());
            }

            for (int i = 0; i < jsonObject.getJSONArray("clear_directories").toList().size(); i++){
                clearDirectories.add(jsonObject.getJSONArray("clear_directories").toList().get(i).toString());
            }

            jvmArgsOverride = jsonObject.getString("jvm_args_override");
        }
    }
}
