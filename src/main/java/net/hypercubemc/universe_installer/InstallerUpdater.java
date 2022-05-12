package net.hypercubemc.universe_installer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class InstallerUpdater {
    private String releaseInfoUrl;
    private String latestVersion;
    private String downloadURL;
    private String pageURL;
    private String changelog;

    public InstallerUpdater(String url) {
        this.releaseInfoUrl = url;
    }

    public void fetchReleaseInfo() throws IOException, JSONException {
        JSONObject json = readJsonFromUrl(this.releaseInfoUrl);
        latestVersion = json.getString("tag_name");
        downloadURL = findObjectWithPropertyValue(json.getJSONArray("assets"), "content_type", "application/x-java-archive").getString("browser_download_url");
        pageURL = json.getString("html_url");
        changelog = json.getString("body");
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    public String getPageURL() {
        return pageURL;
    }

    public String getChangelog() {
        return changelog;
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

    private static JSONObject findObjectWithPropertyValue(JSONArray jsonArray, String property, String value) {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject arrayEntry = jsonArray.getJSONObject(i);
            if (arrayEntry.getString(property).equals(value)) return arrayEntry;
        }
        return null;
    }
}
