# Changelog

## 0.9

### Fixes
- Read-only clicks: netty-safe slot + cursor correction before scheduled handlers (Lunar optimistic pickup / ghost cursor).
- `stateId` uses `nextStateIdAbove(clientStateId)` on corrections and full resyncs (including debounce / invalid-slot paths).
- Always send `ClientboundSetCursorItemPacket(EMPTY)` after read-only reject/resync on 26.x.
- Virtual window clicks/closes never forwarded to vanilla (`fireChannelRead`).
- `closeWindowId` implemented on modern/mid classifiers so virtual closes are swallowed even if session already cleared.
- Pipeline prefers inject **after** Via/decoder; re-assert on menu open.
- Server-side inert `ChestMenu` bind/unbind on modern21_5 + modern26 for AC container validity (window ids 100–126).

### APIs
- `closeThen(Player, Runnable)` / `closeThen(Player, ticks, Runnable)` / `closeAsync(...)`
- `reopen(Player, Menu)` — force close+open for size/type changes
- `present` still differential when type+mode match
- `beginTransition` / `endTransition` unchanged; `closeThen` uses a transition token during settle
- Debug logging via `-Dpacketuxui.debug=true` or `PACKETUXUI_DEBUG=true`

### Publishing
- Artifact: `net.opmasterleo:packetuxui:0.9`
- Releases: `./gradlew publish` → `http://repo.mastersmp.net/releases`
- Snapshots: `./gradlew publishSnapshot` → `http://repo.mastersmp.net/snapshots`

### Core migration
```java
PacketMenus.gui().closeThen(player, () -> signGui.open(player));
PacketMenus.gui().reopen(player, menu54to27);
PacketMenus.gui().present(player, sameTypeRefresh); // differential OK
```
