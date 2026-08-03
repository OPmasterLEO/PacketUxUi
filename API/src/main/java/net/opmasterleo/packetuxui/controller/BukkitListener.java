package net.opmasterleo.packetuxui.controller;

import net.opmasterleo.packetuxui.network.PipelineManager;
import net.opmasterleo.packetuxui.scheduler.PlatformScheduler;
import net.opmasterleo.packetuxui.service.MenuService;

/**
 * @deprecated use {@link BukkitLifecycleListener} or {@link LifecycleListeners}
 */
@Deprecated
public final class BukkitListener extends BukkitLifecycleListener {

    public BukkitListener(MenuService service, PipelineManager pipelineManager, PlatformScheduler scheduler) {
        super(service, pipelineManager, scheduler);
    }
}
