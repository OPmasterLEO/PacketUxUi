# PacketUxUi

Virtual packet menus for **Paper / Spigot / Folia** — Minecraft **1.8 → 26.2**.

Direct NMS. Netty clicks. READ_ONLY buttons or EDITABLE item moves. Folia-safe.

Distributed via [Reposilite](http://repo.mastersmp.net/).

---

## Install

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("http://repo.mastersmp.net/releases")
    }
}

dependencies {
    implementation("net.opmasterleo:packetuxui:0.12.2")
}
```

Shade into your plugin; if you `minimize()`, exclude `net.opmasterleo:packetuxui`.

---

## Use

```java
PacketUxUiAPI.init(this);      // onEnable
PacketUxUiAPI.terminate(this); // onDisable

PacketGuiManager gui = PacketMenus.gui();

// Read-only menu
gui.present(player, PacketMenus.build()
        .title("<gold>Shop")
        .rows(3)
        .readOnly()
        .action(11, item, p -> buy(p))
        .decorative(0, filler));

// Editable (move items in top slots)
gui.present(player, PacketMenus.build()
        .title("Deposit")
        .rows(3)
        .editable()
        .editableSlot(13, ItemStack.empty())
        .action(22, confirm, p -> save(p))
        .onClose((p, snap) -> reclaim(snap)));

// Same type+mode → differential update; size/type change → reopen
gui.present(player, sameTypeRefresh);
gui.reopen(player, smallerMenu);

// Close then SignGUI / chat / anything else
gui.closeThen(player, () -> signGui.open(player));
```


| API                                                          | Role                                              |
| ------------------------------------------------------------ | ------------------------------------------------- |
| `present`                                                    | Open, or dirty Set Slot if same type+mode         |
| `reopen`                                                     | Force close+open (54→27, etc.)                    |
| `close`                                                      | Empty cursor, close packet, unbind, clear session |
| `closeThen`                                                  | `close` + 1 tick, then runnable                   |
| `presentAsync`                                               | Build off-thread, then `present`                  |
| `patchSlots` / `patchSlotAtomic` / `refresh` / `updateTitle` | In-place mutators                                 |


Virtual window ids use vanilla `nextContainerCounter()` (**1–100**). Pipeline injects after Via/decoder. Modern 21.5+/26.x bind a real `ChestMenu` (9xN only) and own `stateId` via `incrementStateId`; top stacks are mirrored into the bound container.

### Protocol


| Rule            | Behavior                                                      |
| --------------- | ------------------------------------------------------------- |
| Window id       | Vanilla 1..100; tracked per player (`isOurs`)                 |
| stateId         | Bound menu `incrementStateId` (session mirrors last sent)     |
| READ_ONLY click | Netty: SetCursorItem(EMPTY). Main: one SetContent + handler   |
| EDITABLE click  | Simulate → mirror → one bumped SetSlot(s) + SetCursorItem     |
| Bind            | Generic 9xN chests only — no wrong-size bind for hopper/anvil |


Debug (server JVM, not plugin.yml):

```text
java -Dpacketuxui.debug=true -jar paper.jar
```

Or env `PACKETUXUI_DEBUG=true`, or at runtime `PacketUxUiAPI.getService().setDebugLogging(true)`.

On boot you should see `debug=true` in the PacketUxUi ready line, then `[PacketUxUi/debug]` on open/close/click.

---

## Library model

PacketUxUi **supports** pagination, live refresh, filters, custom layouts — it does **not** ship them as builtin widgets. You compose with primitives (same idea as PacketEvents listeners/wrappers).

| You want | Use |
|---|---|
| Open / update menu | `present` (diff) / `reopen` (force) |
| Live slot refresh | `patchSlots` / `patchSlotAtomic` / `refresh` / `MenuPackets.setSlot` |
| Pagination | Your page index + rebuild `MenuBuild` + `present`/`reopen`; nav buttons call your code |
| Global click filter | `PacketMenus.registerListener` → cancel `GuiClickEvent` |
| Bukkit-like click metadata | `action()` / `slotType()` / `currentItem()` / `cursor()` / `view()` |
| Drag | `onDrag(GuiDragEvent)` START/ADD/END |
| Layout math | `Slots.index` / `border` / `rectangle` |
| Raw packets | `MenuPackets` (stateId-aware) |

```java
void openPage(Player p, int page) {
    MenuBuild build = PacketMenus.build().title("Items").rows(6);
    for (int slot : Slots.rectangle(1, 1, 4, 7)) { /* page items */ }
    build.action(45, prev, x -> openPage(x, page - 1));
    build.action(53, next, x -> openPage(x, page + 1));
    PacketMenus.present(p, build.materialize()); // or reopen if size changed
}

PacketMenus.registerListener(new GuiListener() {
    @Override
    public void onClick(GuiClickEvent event) {
        if (event.action() == GuiClickAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true); // like cancelling InventoryClickEvent
        }
    }

    @Override
    public void onDrag(GuiDragEvent event) {
        if (event.phase() == GuiDragPhase.END) {
            // inspect event.slots()
        }
    }
});
```

---

## Modes


|                 | READ_ONLY         | EDITABLE                                           |
| --------------- | ----------------- | -------------------------------------------------- |
| Top             | Cancel + handlers | Place/take on EDITABLE slots; ACTION buttons       |
| Bottom          | Light settle      | Light settle; shift-from-bottom into EDITABLE tops |
| Cursor on close | Cleared           | Reclaimed                                          |


`.editable()` menus: `item(...)` defaults to movable. Use `action(...)` / `decorative(...)` when locked.

---



## Modules


| Module              | Role                                         |
| ------------------- | -------------------------------------------- |
| root fat jar        | `net.opmasterleo:packetuxui` — API + all NMS |
| `API`               | Public API                                   |
| `nms-api` / `nms:*` | Bridges + adapters                           |


JDK **21** for Gradle; JDK **25** auto for 26.x modules.

See [CHANGELOG.md](CHANGELOG.md) · [LICENSE.md](LICENSE.md).