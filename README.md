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
    implementation("net.opmasterleo:packetuxui:0.13.6")
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

// Hopper insert (correct 5-slot layout — do not fake with rows(1))
gui.present(player, PacketMenus.build()
        .title("Insert item")
        .hopper()
        .editable()
        .editableSlot(2, ItemStack.empty())); // InventorySlots.HOPPER_CENTER

// Same type+mode → differential update
gui.present(player, sameTypeRefresh);
gui.patchSlots(player, Map.of(11, newItem)); // single/multi slot in place
gui.refresh(player); // full SetContent, same window

// Different size, same mode (27↔54) → silent replace (no CloseWindow)
gui.present(player, smallerOrLargerMenu);

// Force hard close+open only when needed
gui.reopen(player, menu);

// Written book text GUI — stack lines, no \n needed
PacketMenus.book()
    .title("<gold>Rules")
    .author("Server")
    .newPage()
        .line("<gold>Welcome!")
        .blank()
        .line("<white>Be nice")
        .line("<gray>No cheating")
    .done()
    .pageLines("<bold>Page 2", "", "<white>More text")
    .open(player);

// Close then SignGUI / chat / anything else
gui.closeThen(player, () -> signGui.open(player));
```


| API                                                          | Role                                                                         |
| ------------------------------------------------------------ | ---------------------------------------------------------------------------- |
| `present`                                                    | Diff if same type; **silent type swap** (OpenScreen only) if same mode different type; else open |
| `book` / `openBook`                                          | Written-book text viewer (NMS page/length caps; no slots; Adventure click events) |
| `reopen`                                                     | Force close+open                                                             |
| `close`                                                      | Empty cursor, close packet, unbind, clear session                            |
| `closeThen`                                                  | `close` + 1 tick, then runnable                                              |
| `presentAsync`                                               | Build on dedicated menu pool + preload, then `present` on entity thread      |
| `patchSlots` / `patchSlotAtomic` / `refresh` / `updateTitle` | In-place mutators                                                            |


### Editable policies


| Goal                               | Build                                           |
| ---------------------------------- | ----------------------------------------------- |
| Sell / deposit both ways (inv↔gui) | `.editable()` + `.editableSlot(slot, stack)`    |
| Spawner loot take-only (gui→inv)   | `.editable()` + `.extractableSlot(slot, stack)` |
| Buttons + fillers                  | `.action` / `.decorative` as usual              |


```java
// Place into GUI and take out
gui.present(p, PacketMenus.build().title("Sell").rows(6).editable()
        .editableSlot(10, ItemStack.empty())
        .editableSlot(11, ItemStack.empty())
        .action(49, confirm, this::sell));

// Only take from GUI into inv (shift or pickup→click inv)
gui.present(p, PacketMenus.build().title("Spawner").rows(3).editable()
        .extractableSlot(13, drop)
        .action(22, closeBtn, Player::closeInventory));
```

Virtual window ids use vanilla `nextContainerCounter()` (live min/max via `LiveLimits`). Pipeline injects before `packet_handler`. Modern 21.5+/26.x bind a real `ChestMenu` (**generic 9×N only**) and own `stateId` via `incrementStateId`; top stacks are mirrored into the bound container.

### Inventory types

`PacketMenus.build()` supports every vanilla Open Screen type:


| Builder                                                                                     | Type           | Top slots         | Bind                                |
| ------------------------------------------------------------------------------------------- | -------------- | ----------------- | ----------------------------------- |
| `.rows(1..6)`                                                                               | GENERIC9xN     | 9–54              | ChestMenu                           |
| `.hopper()`                                                                                 | HOPPER         | 5                 | packet-only                         |
| `.anvil()`                                                                                  | ANVIL          | 3                 | packet-only                         |
| `.furnace()` / `.smoker()` / `.blastFurnace()`                                              | furnace family | 3                 | packet-only                         |
| `.brewingStand()`                                                                           | BREWING_STAND  | 5                 | packet-only                         |
| `.grindstone()` / `.smithingTable()` / `.loom()` / `.cartographyTable()` / `.stonecutter()` | workstations   | 2–4               | packet-only                         |
| `.beacon()` / `.enchantmentTable()` / `.craftingTable()`                                    | specialty      | 1–10              | packet-only                         |
| `.dispenser()` / `.generic3x3()`                                                            | GENERIC3X3     | 9                 | packet-only                         |
| `.shulkerBox()`                                                                             | SHULKER_BOX    | 27                | packet-only                         |
| `.merchant()` / `.villager()`                                                               | VILLAGER       | 3                 | packet-only                         |
| `.lectern()`                                                                                | LECTERN        | 1 (no player inv) | packet-only                         |
| `.crafter()`                                                                                | CRAFTER3X3     | 9                 | packet-only                         |
| `.type(InventoryType.X)`                                                                    | any            | per type          | chest only if `supportsChestBind()` |


Named indices: `InventorySlots.HOPPER_CENTER`, `ANVIL_RESULT`, `FURNACE_FUEL`, … Same modes on all types: `readOnly` / `editable` / `extractableSlot` / `action` / `decorative`.

Threading: full Folia/Paper scheduler coverage — **entity** (player menus), **region** (location/chunk/block), **global**, **async**, plus elastic **menu workers**. `presentAsync` builds on the menu pool then hops to the entity thread. Init/shutdown/pipeline inject hop per-player. Overrides: `-Dpacketuxui.menuWorkers.max|core|queue`. Never use global/async for player inventory on Folia.

### Protocol


| Rule            | Behavior                                                        |
| --------------- | --------------------------------------------------------------- |
| Window id       | Vanilla 1..100; tracked per player (`isOurs`)                   |
| stateId         | Bound menu `incrementStateId` (session mirrors last sent)       |
| READ_ONLY click | Netty: SetCursorItem(EMPTY). Main: one SetContent + handler     |
| EDITABLE click  | Simulate → mirror → one bumped SetSlot(s) + SetCursorItem       |
| Bind            | Generic 9xN chests only — hopper/anvil/furnace/etc. packet-only |


Debug (server JVM, not plugin.yml):

```text
java -Dpacketuxui.debug=true -jar paper.jar
```

Or env `PACKETUXUI_DEBUG=true`, or at runtime `PacketUxUiAPI.getService().setDebugLogging(true)`.

On boot you should see `debug=true` in the PacketUxUi ready line, then `[PacketUxUi/debug]` on open/close/click.

---

## Library model

PacketUxUi **supports** pagination, live refresh, filters, custom layouts — it does **not** ship them as builtin widgets. You compose with primitives ({@code present}, {@code reopen}, {@code patchSlots}, listeners).


| You want                   | Use                                                                                    |
| -------------------------- | -------------------------------------------------------------------------------------- |
| Open / update menu         | `present` (diff) / `reopen` (force)                                                    |
| Live slot refresh          | `patchSlots` / `patchSlotAtomic` / `refresh` / `MenuPackets.setSlot`                   |
| Pagination                 | Your page index + rebuild `MenuBuild` + `present`/`reopen`; nav buttons call your code |
| Global click filter        | `PacketMenus.registerListener` → cancel `GuiClickEvent`                                |
| Bukkit-like click metadata | `action()` / `slotType()` / `currentItem()` / `cursor()` / `view()`                    |
| Drag                       | `onDrag(GuiDragEvent)` START/ADD/END                                                   |
| Layout math                | `Slots.index` / `border` / `rectangle`                                                 |
| Raw packets                | `MenuPackets` (stateId-aware)                                                          |


```java
void openPage(Player p, int page) {
    MenuBuild build = PacketMenus.build().title("Items").rows(6);
    for (int slot : Slots.rectangle(1, 1, 4, 7)) { /* page items */ }
    build.action(45, prev, x -> openPage(x, page - 1));
    build.action(53, next, x -> openPage(x, page + 1));
    PacketMenus.present(p, build.materialize()); // size change is fine — silent replace
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