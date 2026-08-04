package net.opmasterleo.packetuxui.nms.item;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

/**
 * Apply / read custom head textures for packet menus.
 */
public final class SkullTextures {

    private SkullTextures() {
    }

    public static void applyBase64(SkullMeta skull, String base64) {
        if (skull == null || base64 == null || base64.isEmpty()) {
            return;
        }
        try {
            if (applyViaPaperProperty(skull, base64)) {
                return;
            }
            String url = skinUrlFromBase64(base64);
            if (url == null || url.isEmpty()) {
                return;
            }
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "packetuxui");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create(url).toURL());
            profile.setTextures(textures);
            skull.setOwnerProfile(profile);
        } catch (Throwable ignored) {
        }
    }

    public static String extractBase64(SkullMeta skull) {
        if (skull == null) {
            return null;
        }
        try {
            String paper = extractViaPaperProperty(skull);
            if (paper != null && !paper.isEmpty()) {
                return paper;
            }
            PlayerProfile profile = skull.getOwnerProfile();
            if (profile == null) {
                return null;
            }
            PlayerTextures textures = profile.getTextures();
            if (textures == null || textures.getSkin() == null) {
                return null;
            }
            String url = textures.getSkin().toString();
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url.replace("\"", "") + "\"}}}";
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean applyViaPaperProperty(SkullMeta skull, String base64) {
        try {
            Class<?> propertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
            Object property = propertyClass
                    .getConstructor(String.class, String.class)
                    .newInstance("textures", base64);
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "packetuxui");
            // Paper PlayerProfile#setProperty
            profile.getClass().getMethod("setProperty", propertyClass).invoke(profile, property);
            skull.setOwnerProfile(profile);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String extractViaPaperProperty(SkullMeta skull) {
        try {
            PlayerProfile profile = skull.getOwnerProfile();
            if (profile == null) {
                return null;
            }
            Collection<?> properties = (Collection<?>) profile.getClass()
                    .getMethod("getProperties")
                    .invoke(profile);
            if (properties == null) {
                return null;
            }
            for (Object property : properties) {
                String name = String.valueOf(property.getClass().getMethod("getName").invoke(property));
                if (!"textures".equals(name)) {
                    continue;
                }
                return String.valueOf(property.getClass().getMethod("getValue").invoke(property));
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    static String skinUrlFromBase64(String base64) {
        try {
            String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            int urlKey = json.indexOf("\"url\"");
            if (urlKey < 0) {
                return null;
            }
            int colon = json.indexOf(':', urlKey);
            int firstQuote = json.indexOf('"', colon + 1);
            int secondQuote = json.indexOf('"', firstQuote + 1);
            if (firstQuote < 0 || secondQuote < 0) {
                return null;
            }
            return json.substring(firstQuote + 1, secondQuote);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
