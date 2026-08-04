# PacketUxUi

Virtual inventory GUIs over the Minecraft protocol — **Paper / Spigot / Folia**, **1.8 → 26.2**.

No Bukkit inventory views. Clicks arrive on Netty. Menus can be read-only or allow item moves. Folia-safe.

Artifact: [Reposilite](http://repo.mastersmp.net/) · `net.opmasterleo:packetuxui`

---

## Install

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("http://repo.mastersmp.net/releases") }
}

dependencies {
    implementation("net.opmasterleo:packetuxui:0.13.9")
}
```

Shade into your plugin. If you `minimize()`, exclude `net.opmasterleo:packetuxui`.

---

## Quick start

```java
PacketUxUiAPI.init(this);      // onEnable
PacketUxUiAPI.terminate(this); // onDisable

PacketGuiManager gui = PacketMenus.gui();

gui.present(player, PacketMenus.build()
        .title("<gold>Menu")
        .rows(3)
        .readOnly()
        .action(13, button, p -> { /* click */ })
        .decorative(0, filler));
```

---

## Menus

```java
// Read-only — clicks run handlers; top slots stay locked
gui.present(player, PacketMenus.build()
        .title("Example")
        .rows(3)
        .readOnly()
        .action(11, item, this::onClick)
        .decorative(0, filler));

// Editable — players can move items in marked slots
gui.present(player, PacketMenus.build()
        .title("Storage")
        .rows(3)
        .editable()
        .editableSlot(13, ItemStack.empty())
        .action(22, confirm, this::onConfirm)
        .onClose((p, snap) -> { /* reclaim snap if needed */ }));

// Take-only slots (GUI → player inventory)
gui.present(player, PacketMenus.build()
        .title("Rewards")
        .rows(3)
        .editable()
        .extractableSlot(13, reward));

// Hopper (5 slots — use .hopper(), not rows(1))
gui.present(player, PacketMenus.build()
        .title("Insert")
        .hopper()
        .editable()
        .editableSlot(2, ItemStack.empty())); // InventorySlots.HOPPER_CENTER
```

### Open / update / close

| Call | Behavior |
| --- | --- |
| `present` | Diff update if same type; silent type swap if same mode, different size; else open |
| `reopen` | Force close + open |
| `patchSlots` / `refresh` / `updateTitle` | In-place changes |
| `close` | Clear cursor, close packet, drop session |
| `closeThen` | `close`, then run something next tick (e.g. another GUI) |
| `presentAsync` | Build off-thread, then `present` on the entity scheduler |

```java
gui.present(player, refreshedSameType);          // differential
gui.patchSlots(player, Map.of(11, newItem));
gui.refresh(player);
gui.present(player, differentSizeSameMode);      // silent replace (no CloseWindow)
gui.reopen(player, menu);                        // hard reopen
gui.closeThen(player, () -> otherUi.open(player));
```

### Modes

| | `readOnly()` | `editable()` |
| --- | --- | --- |
| Top | Cancel + run handlers | Move items on editable slots; `action` stays locked |
| Bottom | Light settle | Shift into editable tops allowed |
| Cursor on close | Cleared | Reclaimed via `onClose` snapshot |

On `.editable()` menus, `item(...)` defaults to movable. Use `action(...)` / `decorative(...)` for locked slots. Prefer `editableSlot` / `extractableSlot` when you care about direction.

---

## Books

Written-book text viewer (no inventory slots). Page/length limits come from live NMS (`LiveLimits`).

```java
PacketMenus.book()
    .title("<gold>Rules")
    .author("Server")
    .newPage()
        .line("<gold>Welcome")
        .blank()
        .line("<white>Be nice")
    .done()
    .pageLines("<bold>Page 2", "", "<white>More text")
    .open(player);
```

Adventure click events work on page text. Turning pages is client-side.

---

## Inventory types

Every vanilla Open Screen type is available on `PacketMenus.build()`:

| Builder | Top |
| --- | --- |
| `.rows(1..6)` | 9–54 (generic chest; can bind real `ChestMenu` on modern) |
| `.hopper()` | 5 |
| `.anvil()` / `.furnace()` / `.brewingStand()` / … | per vanilla type |
| `.type(InventoryType.X)` | any |

Named slots: `InventorySlots.HOPPER_CENTER`, `ANVIL_RESULT`, `FURNACE_FUEL`, …

Only generic 9×N chests bind a real container on modern Paper. Hopper, anvil, furnace, and the rest stay packet-only.

---

## Events & composition

PacketUxUi gives primitives. Pagination, filters, and custom layouts are yours to build.

```java
void openPage(Player p, int page) {
    MenuBuild build = PacketMenus.build().title("Items").rows(6);
    for (int slot : Slots.rectangle(1, 1, 4, 7)) {
        // fill page
    }
    build.action(45, prev, x -> openPage(x, page - 1));
    build.action(53, next, x -> openPage(x, page + 1));
    PacketMenus.present(p, build.materialize());
}

PacketMenus.registerListener(new GuiListener() {
    @Override
    public void onClick(GuiClickEvent event) {
        if (event.action() == GuiClickAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onDrag(GuiDragEvent event) {
        if (event.phase() == GuiDragPhase.END) {
            // event.slots()
        }
    }
});
```

Useful pieces: `Slots.index` / `border` / `rectangle`, `MenuPackets`, `GuiClickEvent` (`action`, `slotType`, `currentItem`, `cursor`), `onDrag`.

Items from Bukkit (`fromBukkit` / fillers) keep full NBT (enchants, lore, potions, components). Builder items use name / lore / enchant / CMD / skull texture fields.

---

## Runtime notes

- Window ids follow vanilla `nextContainerCounter()` (bounds via `LiveLimits`)
- Pipeline injects before `packet_handler`
- Folia/Paper: use **entity** scheduler for player menus; `presentAsync` builds on menu workers then hops to the entity thread
- Overrides: `-Dpacketuxui.menuWorkers.max|core|queue`
- Debug: `-Dpacketuxui.debug=true`, env `PACKETUXUI_DEBUG=true`, or `PacketUxUiAPI.getService().setDebugLogging(true)`

---

## Modules

| | |
| --- | --- |
| `net.opmasterleo:packetuxui` | Fat jar — API + all NMS adapters |
| `API` / `nms-api` / `nms:*` | Sources if you build from this repo |

JDK **21** for Gradle; JDK **25** for 26.x modules.

[CHANGELOG](CHANGELOG.md) · [LICENSE](LICENSE.md)
