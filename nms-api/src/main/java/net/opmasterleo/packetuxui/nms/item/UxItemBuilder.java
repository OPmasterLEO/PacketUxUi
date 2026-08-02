package net.opmasterleo.packetuxui.nms.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

public class UxItemBuilder {

    protected String materialKey = "minecraft:air";
    protected Component name;
    protected final List<Component> lore = new ArrayList<>();
    protected int amount = 1;
    protected final Map<String, Integer> enchantments = new LinkedHashMap<>();
    protected boolean hideEnchantments = true;
    protected Integer customModelData;
    protected String headTextureBase64;

    @SuppressWarnings("unchecked")
    protected final <T extends UxItemBuilder> T self() {
        return (T) this;
    }

    public UxItemBuilder material(String materialKey) {
        this.materialKey = materialKey;
        return this;
    }

    public UxItemBuilder name(Component name) {
        this.name = name;
        return this;
    }

    public UxItemBuilder lore(Component line) {
        this.lore.add(line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        return this;
    }

    public UxItemBuilder lore(List<Component> lines) {
        for (Component line : lines) {
            lore(line);
        }
        return this;
    }

    public UxItemBuilder lore(Component... lines) {
        for (Component line : lines) {
            lore(line);
        }
        return this;
    }

    public UxItemBuilder amount(int amount) {
        this.amount = amount;
        return this;
    }

    public UxItemBuilder enchantment(String enchantKey, int level, boolean visible) {
        this.enchantments.put(enchantKey, level);
        this.hideEnchantments = !visible;
        return this;
    }

    public UxItemBuilder enchantments(Map<String, Integer> enchantments, boolean visible) {
        this.enchantments.putAll(enchantments);
        this.hideEnchantments = !visible;
        return this;
    }

    public UxItemBuilder customModelData(int cmd) {
        this.customModelData = cmd;
        return this;
    }

    public UxItem build() {
        return new UxItem(
                materialKey,
                amount,
                name,
                lore,
                enchantments,
                hideEnchantments,
                customModelData,
                headTextureBase64
        );
    }
}
