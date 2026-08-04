package net.opmasterleo.packetuxui.controller;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.opmasterleo.packetuxui.event.GuiCloseReason;
import net.opmasterleo.packetuxui.network.PipelineManager;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.MenuService;

public final class PaperLifecycleListener implements Listener {

    private final MenuService service;
    private final PipelineManager pipelineManager;
    private final PlatformScheduler scheduler;

    public PaperLifecycleListener(
            MenuService service,
            PipelineManager pipelineManager,
            PlatformScheduler scheduler
    ) {
        this.service = service;
        this.pipelineManager = pipelineManager;
        this.scheduler = scheduler;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (scheduler.isOwnedByCurrentRegion(player)) {
            pipelineManager.inject(player);
            return;
        }
        scheduler.runForPlayer(player, () -> pipelineManager.inject(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        forceClose(event.getPlayer(), GuiCloseReason.QUIT);
        pipelineManager.remove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        if (scheduler.isOwnedByCurrentRegion(player)) {
            forceClose(player, GuiCloseReason.KICK);
            return;
        }
        scheduler.runForPlayer(player, () -> forceClose(player, GuiCloseReason.KICK));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (scheduler.isOwnedByCurrentRegion(player)) {
            forceClose(player, GuiCloseReason.DEATH);
            return;
        }
        scheduler.runForPlayer(player, () -> forceClose(player, GuiCloseReason.DEATH));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (service.hasOpen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (service.hasOpen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void forceClose(Player player, GuiCloseReason reason) {
        try {
            service.onCloseMenu(player, reason);
        } catch (Throwable ignored) {
        }
    }
}
