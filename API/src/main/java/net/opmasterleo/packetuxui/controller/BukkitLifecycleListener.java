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

import net.opmasterleo.packetuxui.network.PipelineManager;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.MenuService;

/**
 * Join/quit pipeline + Bukkit inventory safety net while a PacketUxUi session is open.
 * Packet path is authoritative; these cancels stop leaks if a click reaches CraftBukkit.
 */
public class BukkitLifecycleListener implements Listener {

    private final MenuService service;
    private final PipelineManager pipelineManager;
    private final PlatformScheduler scheduler;

    public BukkitLifecycleListener(
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
        scheduler.runForPlayer(player, new InjectPipelineTask(pipelineManager, player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        forceClose(event.getPlayer(), net.opmasterleo.packetuxui.event.GuiCloseReason.KICK);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        forceClose(event.getEntity(), net.opmasterleo.packetuxui.event.GuiCloseReason.DEATH);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        forceClose(event.getPlayer(), net.opmasterleo.packetuxui.event.GuiCloseReason.QUIT);
        pipelineManager.remove(event.getPlayer());
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

    private void forceClose(Player player, net.opmasterleo.packetuxui.event.GuiCloseReason reason) {
        try {
            service.onCloseMenu(player, reason);
        } catch (Throwable ignored) {
        }
    }

    private static final class InjectPipelineTask implements Runnable {
        private final PipelineManager pipelineManager;
        private final Player player;

        private InjectPipelineTask(PipelineManager pipelineManager, Player player) {
            this.pipelineManager = pipelineManager;
            this.player = player;
        }

        @Override
        public void run() {
            pipelineManager.inject(player);
        }
    }
}
