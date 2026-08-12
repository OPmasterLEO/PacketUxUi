package net.opmasterleo.packetuxui.nms;

import org.bukkit.entity.Player;

public interface SignPacketBridge {

    SignPacketBridge UNSUPPORTED = new UnsupportedSigns();

    boolean open(Player player, SignOpenRequest request);

    boolean refresh(Player player, SignOpenRequest request);

    void close(Player player, int x, int y, int z);

    SignUpdate readUpdate(Object packet);

    final class UnsupportedSigns implements SignPacketBridge {
        @Override
        public boolean open(Player player, SignOpenRequest request) {
            return false;
        }

        @Override
        public boolean refresh(Player player, SignOpenRequest request) {
            return false;
        }

        @Override
        public void close(Player player, int x, int y, int z) {
        }

        @Override
        public SignUpdate readUpdate(Object packet) {
            return null;
        }
    }
}
