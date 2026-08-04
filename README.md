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
    implementation("net.opmasterleo:packetuxui:0.10")
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


Debug: `-Dpacketuxui.debug=true`

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