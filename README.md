# PacketUxUi

Virtual packet menus for **Paper / Spigot / Folia** — Minecraft **1.8 → 26.2**.

Direct NMS (no PacketEvents). Netty click handling, dirty-slot Set Slot updates, Folia-safe schedulers.

[JitPack](https://jitpack.io/#OPmasterLEO/PacketUxUi) · one fat jar (`packetuxui`) with API + all version adapters shaded in.

---

## Install

Add JitPack, then depend on the fat module. Replace `Tag` with a release tag, commit SHA, or `main-SNAPSHOT`.

**Gradle**

```gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.OPmasterLEO.PacketUxUi:packetuxui:Tag'
}
```

**Gradle Kotlin**

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.OPmasterLEO.PacketUxUi:packetuxui:Tag")
}
```

**Maven**

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.OPmasterLEO.PacketUxUi</groupId>
  <artifactId>packetuxui</artifactId>
  <version>Tag</version>
</dependency>
```

Shade into your plugin if you want a soft-depend-free jar. If you use `minimize()`, exclude this dependency so NMS wrappers are not stripped:

```kotlin
tasks.shadowJar {
    // relocate("net.opmasterleo.packetuxui", "your.plugin.lib.packetuxui")
    minimize {
        exclude(dependency("com.github.OPmasterLEO.PacketUxUi:packetuxui:.*"))
    }
}
```

**Local publish**

```bash
./gradlew :packetuxui:publishToMavenLocal
```

```kotlin
repositories { mavenLocal() }
dependencies { implementation("net.opmasterleo:packetuxui:1.0.0") }
```

---

## Quick start

```java
@Override
public void onEnable() {
    PacketUxUiAPI.init(this);
}

@Override
public void onDisable() {
    PacketUxUiAPI.terminate(this);
}
```

```java
PacketGuiManager gui = PacketMenus.gui();

gui.setScopeListener((player, open, topSlots) -> {
    // Worth / lore overlays: open BEFORE content packets; close AFTER cleanup
});
gui.setClickDebounceMillis(100);

MenuBuild page = PacketMenus.build()
        .title("<gold>Shop <gray>(1/3)")
        .rows(3)
        .editable()
        .decorative(0, filler)
        .action(4, confirmItem, p -> confirm(p))
        .editableSlot(13, ItemStack.empty())
        .onClose(p -> save(p));

gui.present(player, page);                          // same size → dirty Set Slot; else reopen
gui.presentAsync(player, () -> buildHeavyPage());
gui.updateTitle(player, Component.text("Shop (2/3)")); // same window id
gui.patchSlots(player, Map.of(13, stack));
gui.close(player);

int windowId = gui.getWindowId(player);     // 100–126 while open, else -1
int topCount = gui.getTopSlotCount(player); // overlays skip plugin top slots
```

Patch one slot without a full Window Items rebuild:

```java
PacketMenus.patchSlot(player, 13, UxItem.builder("minecraft:diamond").build());
PacketMenus.refresh(player); // full Window Items only when you need it
```

---

## READ_ONLY vs EDITABLE

| | READ_ONLY | EDITABLE |
|---|---|---|
| Top clicks | Cancel + dirty Set Slot; run ACTION handlers | DECORATIVE cancel; ACTION handler; EDITABLE place/take |
| Bottom clicks | Light settle | Light settle; shift-click merges into EDITABLE tops |
| Drag | Cancel | Only if every touched top slot is EDITABLE |
| Number / offhand / double-collect | Cancel | Denied by default |
| Cursor on close | Cleared | Reclaimed into player inv (leftover dropped) |
| Real inv writes | Never | Shift-from-bottom + close reclaim only |

Slot kinds: `DECORATIVE`, `ACTION`, `EDITABLE`. Unspecified slots in editable menus default to editable.

Use `registerTakeablePredicate` / `registerTakeablePredicateBukkit` when special items must leave restricted top slots.

**Prefer** `READ_ONLY` for leaderboards / stats. **Prefer** `EDITABLE` for rearrange / deposit UIs.

---

## Packets

| When | Packet |
|---|---|
| Open | Open Window + Window Items |
| Title-only / same-size present | Open Window (title) + dirty Set Slot |
| Click settle / patch | Set Slot (dirty only) |
| `refresh()` / heavy settle | Window Items |
| Close | Close Window |

No `Player#updateInventory()` on the default click path.

Virtual window ids are pooled **100–126** per player (`BitSet` reclaim). Exhaustion fails open via `setOpenFailedHandler`. Released on close / quit / kick / death. Sessions keyed by `UUID`.

**Overlays** (e.g. Worth lore):

1. `setScopeListener` — open before content; close after cleanup  
2. Skip outbound slots `0 .. getTopSlotCount()-1` for that player's `getWindowId()`  
3. Works for both menu modes  

Anti-dupe: clicks ignored while `OPENING` / `CLOSING`; ~100ms debounce (configurable).

---

## Folia / Paper / Spigot

Scheduler backend is chosen **once at init** — no Paper/Bukkit branching on hot paths.

| Backend | When | Sync / entity | Async | Events |
|---|---|---|---|---|
| `SchedulerKind.BUKKIT` | Spigot / CraftBukkit | main-thread Bukkit | Bukkit async | `BukkitLifecycleListener` |
| `SchedulerKind.PAPER` | Paper / Folia | entity + region + global | Paper `AsyncScheduler` | `PaperLifecycleListener` |

- Spigot never loads Paper scheduler classes (`scheduler.bukkit` / `scheduler.paper`)
- `presentAsync` / `updateAsync` build off-thread; apply on entity/main; stale generations ignored

---

## Project layout

| Module | Role |
|---|---|
| **`packetuxui`** | Only published artifact — fat jar |
| `API` | Public API (shaded into fat jar) |
| `nms-api` | Bridges / `UxItem` / `AdapterLoader` |
| `nms:*` | Per-version adapters (1.8–26.2) |
| `TestMenu` | Demo Paper plugin |

```bash
./gradlew :packetuxui:publishToMavenLocal
./gradlew :TestMenu:shadowJar
```

Legacy Spigot NMS jars come from [CodeMC](https://docs.codemc.io/faq/using-nms-repository/). Paper `1.20.5+` uses paperweight (network on first setup). JDK **21** runs Gradle; JDK **25** is auto-provisioned (Foojay) for Minecraft 26.x modules.

| Range | Mapping |
|---|---|
| 1.8 – 1.20.4 | Relocated CraftBukkit / Spigot NMS |
| 1.20.5 – 26.2 | Mojang-mapped Paper (Paper/Folia recommended) |

---

See [LICENSE.md](LICENSE.md).
