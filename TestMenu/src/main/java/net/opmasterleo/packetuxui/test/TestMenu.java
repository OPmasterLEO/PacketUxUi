package net.opmasterleo.packetuxui.test;

import net.opmasterleo.packetuxui.PacketUxUiAPI;
import net.opmasterleo.packetuxui.nms.AdapterLoader;
import net.opmasterleo.packetuxui.test.commands.CommandListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class TestMenu extends JavaPlugin {

    @Override
    public void onEnable() {
        var adapter = AdapterLoader.load();
        getLogger().info("Loaded NMS adapter " + adapter.bucketId()
                + " (protocol " + adapter.minProtocol() + ".." + adapter.maxProtocol() + ")");
        PacketUxUiAPI.init(this, adapter);
        new CommandListener(this, PacketUxUiAPI.getService());
    }

    @Override
    public void onDisable() {
        PacketUxUiAPI.terminate(this);
    }
}
