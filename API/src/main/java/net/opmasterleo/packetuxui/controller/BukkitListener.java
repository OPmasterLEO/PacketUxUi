package net.opmasterleo.packetuxui.controller;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.opmasterleo.packetuxui.network.PipelineManager;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.MenuService;

public final class BukkitListener implements Listener {

    private final MenuService service;
    private final PipelineManager pipelineManager;
    private final PlatformScheduler scheduler;

    public BukkitListener(MenuService service, PipelineManager pipelineManager, PlatformScheduler scheduler) {
        this.service = service;
        this.pipelineManager = pipelineManager;
        this.scheduler = scheduler;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduler.runForPlayer(event.getPlayer(), () -> pipelineManager.inject(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.onCloseMenu(event.getPlayer());
        pipelineManager.remove(event.getPlayer());
    }
}
