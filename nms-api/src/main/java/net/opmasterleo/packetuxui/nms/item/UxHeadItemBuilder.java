package net.opmasterleo.packetuxui.nms.item;

public class UxHeadItemBuilder extends UxItemBuilder {

    public UxHeadItemBuilder headTextureBase64(String base64) {
        this.headTextureBase64 = base64;
        return this;
    }

    public UxHeadItemBuilder headTextureFromName(String name) {
        return this;
    }

    public UxHeadItemBuilder headTextureFromUrl(String url) {
        return this;
    }

    public UxHeadItemBuilder headTextureFromUuid(String uuid) {
        return this;
    }
}
