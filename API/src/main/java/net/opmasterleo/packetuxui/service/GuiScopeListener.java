package net.opmasterleo.packetuxui.service;

@FunctionalInterface
public interface GuiScopeListener {

    void onScopeChanged(org.bukkit.entity.Player player, boolean open, int topSlotCount);
}
