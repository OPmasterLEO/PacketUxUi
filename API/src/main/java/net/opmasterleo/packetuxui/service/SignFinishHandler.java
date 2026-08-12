package net.opmasterleo.packetuxui.service;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface SignFinishHandler {

    SignAction onFinish(Player player, SignResult result);
}
