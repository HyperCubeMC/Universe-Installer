package net.hypercubemc.universe_installer;

import mjson.Json;
import net.fabricmc.installer.client.ProfileInstaller;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;
import org.json.JSONObject;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class VanillaLauncherIntegration {
    public static boolean installToLauncher(Path vanillaGameDir, Path instanceDir, String editionName, String profileName, String gameVersion, String loaderName, String loaderVersion, String jvmArgs, Icon icon) throws IOException {
        String versionId = String.format("%s-%s-%s", loaderName, loaderVersion, gameVersion);

        ProfileInstaller.LauncherType launcherType = System.getProperty("os.name").contains("Windows") ? getLauncherType(vanillaGameDir) : /* Return standalone if we aren't on Windows.*/ ProfileInstaller.LauncherType.WIN32;
        if (launcherType == null) {
            // The installation has been canceled via closing the window, most likely.
            return false;
        }
        installVersion(vanillaGameDir, gameVersion, loaderName, loaderVersion, launcherType);
        installProfile(vanillaGameDir, instanceDir, editionName, jvmArgs, profileName, versionId, icon, launcherType);
        return true;
    }

    public static void installVersion(Path mcDir, String gameVersion, String loaderName, String loaderVersion, ProfileInstaller.LauncherType launcherType) throws IOException {
        System.out.println("Installing " + gameVersion + " with loader " + loaderVersion + " to launcher " + launcherType);
        String versionId = String.format("%s-%s-%s", loaderName, loaderVersion, gameVersion);
        Path versionsDir = mcDir.resolve("versions");
        Path profileDir = versionsDir.resolve(versionId);
        Path profileJsonPath = profileDir.resolve(versionId + ".json");
        if (!Files.exists(profileDir)) {
            Files.createDirectories(profileDir);
        }

        Path dummyJar = profileDir.resolve(versionId + ".jar");
        Files.deleteIfExists(dummyJar);
        Files.createFile(dummyJar);
        URL profileUrl = new URL(Reference.getMetaServerEndpoint(String.format("v2/versions/loader/%s/%s/profile/json", gameVersion, loaderVersion)));
        Json profileJson = Json.read(profileUrl);
        if (loaderName.equals("universe-fabric-loader")) {
            editVersionJson(profileJson);
        }
        Utils.writeToFile(profileJsonPath, profileJson.toString());
    }

    private static void editVersionJson(Json profileJson) {
        Json.Factory factory = Json.factory();
        Map<String, Json> json = profileJson.asJsonMap();
        // Replace fabric-loader-etc with universe-fabric-loader-etc
        json.compute("id", (ignored, existing) -> factory.string("universe-" + existing.asString()));
        // Replace loader maven url and name
        for (Json entry : json.get("libraries").asJsonList()) {
            final String id = "net.fabricmc:fabric-loader:";
            String name = entry.asJsonMap().get("name").asString();
            if (name.startsWith("net.fabricmc:fabric-loader:")) {
                entry.asJsonMap().put("name", factory.string("net.hypercubemc:universe-loader:" + name.substring(id.length())));
                entry.asJsonMap().put("url", factory.string(Installer.INSTANCE.MAVEN_URL));
            }
        }
    }

    private static void installProfile(Path mcDir, Path instanceDir, String editionName, String jvmArgs, String profileName, String versionId, Icon icon, ProfileInstaller.LauncherType launcherType) throws IOException {
        Path launcherProfiles = mcDir.resolve(launcherType.profileJsonName);
        if (!Files.exists(launcherProfiles)) {
            System.out.println("Could not find launcher_profiles");
            return;
        }

        System.out.println("Creating profile");

        JSONObject jsonObject = new JSONObject(Utils.readString(launcherProfiles));

        JSONObject profiles = jsonObject.getJSONObject("profiles");

        String foundProfileName = profileName;

        for (Iterator<String> it = profiles.keys(); it.hasNext();) {
            String key = it.next();

            JSONObject foundProfile = profiles.getJSONObject(key);
            if (foundProfile.has("javaArgs") && parseUniverseEdition(foundProfile.getString("javaArgs")).equals(editionName) && foundProfile.has("lastVersionId") && foundProfile.getString("lastVersionId").endsWith(versionId)) {
                foundProfileName = key;
            }
        }

        // If the profile already exists, use it instead of making a new one so that user's settings are kept (e.g icon)
        JSONObject profile = profiles.has(foundProfileName) ? editProfile(profiles.getJSONObject(foundProfileName), profileName, instanceDir, versionId, icon, editionName, jvmArgs) : createProfile(profileName, instanceDir, versionId, icon, editionName, jvmArgs);

        profiles.put(foundProfileName, profile);
        jsonObject.put("profiles", profiles);

        Utils.writeToFile(launcherProfiles, jsonObject.toString(4));
    }

    public static JSONObject editProfile(JSONObject profile, String profileName, Path instanceDir, String versionId, Icon icon, String editionName, String jvmArgs) {
        profile.put("name", profileName);
        profile.put("gameDir", instanceDir.toString());
        if (profile.has("javaArgs")) {
            String[] args = profile.getString("javaArgs").split(" ");
            int editionIndex = findUniverseEditionIndex(args);
            if (editionIndex != -1) args[editionIndex] = "";
            profile.put("javaArgs", Arrays.stream(args).filter(arg -> arg.length() != 0).collect(Collectors.joining(" ")) + " -DUniverseEdition=" + editionName);
        } else {
            profile.put("javaArgs", jvmArgs + " -DUniverseEdition=" + editionName);
        }
        profile.put("lastUsed", Utils.ISO_8601.format(new Date())); // Update timestamp to bring to top of profile list
        profile.put("lastVersionId", versionId);
        return profile;
    }

    private static JSONObject createProfile(String name, Path instanceDir, String versionId, Icon icon, String editionName, String jvmArgs) {
        JSONObject profile = new JSONObject();
        profile.put("name", name);
        profile.put("type", "custom");
        profile.put("created", Utils.ISO_8601.format(new Date()));
        profile.put("gameDir", instanceDir.toString());
        profile.put("javaArgs", jvmArgs + " -DUniverseEdition=" + editionName);
        profile.put("lastUsed", Utils.ISO_8601.format(new Date()));
        profile.put("lastVersionId", versionId);
        profile.put("icon", getProfileIcon(icon));
        return profile;
    }

    private static String getProfileIcon(Icon icon) {
        if (icon == Icon.FABRIC) {
            return Utils.getProfileIcon();
        }

        try {
            InputStream is = Utils.class.getClassLoader().getResourceAsStream("universe_profile_icon.png");

            String var4;
            try {
                byte[] ret = new byte[4096];
                int offset = 0;

                int len;
                while((len = is.read(ret, offset, ret.length - offset)) != -1) {
                    offset += len;
                    if (offset == ret.length) {
                        ret = Arrays.copyOf(ret, ret.length * 2);
                    }
                }

                var4 = "data:image/png;base64," + Base64.getEncoder().encodeToString(Arrays.copyOf(ret, offset));
            } catch (Throwable var6) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                    }
                }

                throw var6;
            }

            if (is != null) {
                is.close();
            }

            return var4;
        } catch (IOException var7) {
            var7.printStackTrace();
            return "TNT";
        }
    }


    private static ProfileInstaller.LauncherType showLauncherTypeSelection() {
        String[] options = new String[]{Utils.BUNDLE.getString("prompt.launcher.type.xbox"), Utils.BUNDLE.getString("prompt.launcher.type.win32")};
        int result = JOptionPane.showOptionDialog(null, Utils.BUNDLE.getString("prompt.launcher.type.body"), Utils.BUNDLE.getString("installer.title"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (result == JOptionPane.CLOSED_OPTION) {
            return null;
        } else {
            return result == JOptionPane.YES_OPTION ? ProfileInstaller.LauncherType.MICROSOFT_STORE : ProfileInstaller.LauncherType.WIN32;
        }
    }

    public static ProfileInstaller.LauncherType getLauncherType(Path vanillaGameDir) {
        ProfileInstaller.LauncherType launcherType;
        List<ProfileInstaller.LauncherType> types = getInstalledLauncherTypes(vanillaGameDir);
        if (types.size() == 0) {
            // Default to WIN32, since nothing will happen anyway
            System.out.println("No launchers found, profile installation will not take place!");
            launcherType = ProfileInstaller.LauncherType.WIN32;
        } else if (types.size() == 1) {
            System.out.println("Found only one launcher (" + types.get(0) + "), will proceed with that!");
            launcherType = types.get(0);
        } else {
            System.out.println("Multiple launchers found, showing selection screen!");
            launcherType = showLauncherTypeSelection();
            if (launcherType == null) {
                System.out.println(Utils.BUNDLE.getString("prompt.ready.install"));
                launcherType = ProfileInstaller.LauncherType.WIN32;
            }
        }

        return launcherType;
    }

    public static List<ProfileInstaller.LauncherType> getInstalledLauncherTypes(Path mcDir) {
        return Arrays.stream(ProfileInstaller.LauncherType.values()).filter((launcherType) -> Files.exists(mcDir.resolve(launcherType.profileJsonName))).collect(Collectors.toList());
    }

    public static String parseUniverseEdition(String javaArgs) {
        String[] args = javaArgs.split(" ");
        int editionIndex = findUniverseEditionIndex(args);
        if (editionIndex == -1) return "";

        return args[editionIndex].split("=")[1];
    }
    public static int findUniverseEditionIndex(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String[] propertyValueList = args[i].split("=");
            String property = propertyValueList[0];
            if (property.equals("-DUniverseEdition") && propertyValueList.length == 2) {
                return i;
            }
        }
        return -1;
    }

    public enum Icon {
        UNIVERSE,
        FABRIC
    }
}
