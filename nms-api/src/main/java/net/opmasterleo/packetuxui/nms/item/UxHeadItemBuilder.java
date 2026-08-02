package net.opmasterleo.packetuxui.nms.item;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class UxHeadItemBuilder extends UxItemBuilder {

    public UxHeadItemBuilder() {
        this.materialKey = "minecraft:player_head";
    }

    public UxHeadItemBuilder headTextureBase64(String base64) {
        this.headTextureBase64 = base64;
        return this;
    }

    public UxHeadItemBuilder headTextureFromName(String name) {
        // Offline / legacy texture lookup is server-specific; encode a name profile payload.
        if (name != null && !name.isBlank()) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"https://minotar.net/skin/" + name + "\"}}}";
            this.headTextureBase64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }
        return this;
    }

    public UxHeadItemBuilder headTextureFromUrl(String url) {
        if (url != null && !url.isBlank()) {
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url.replace("\"", "") + "\"}}}";
            this.headTextureBase64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }
        return this;
    }

    public UxHeadItemBuilder headTextureFromUuid(String uuid) {
        if (uuid != null && !uuid.isBlank()) {
            String clean = uuid.replace("-", "");
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"https://crafatar.com/skins/" + clean + "\"}}}";
            this.headTextureBase64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }
        return this;
    }

    public UxHeadItemBuilder headTextureFromUuid(UUID uuid) {
        return uuid == null ? this : headTextureFromUuid(uuid.toString());
    }
}
