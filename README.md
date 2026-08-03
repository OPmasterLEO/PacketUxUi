# PacketUxUi

[![JitPack](https://jitpack.io/v/OPmasterLEO/PacketUxUi.svg)](https://jitpack.io/#OPmasterLEO/PacketUxUi)

Virtual packet menus for Paper / Spigot / Folia (**Minecraft 1.8 → 26.2**).  
Direct NMS (no PacketEvents) — Netty click handling, dirty-slot Set Slot updates, Folia entity hops.

## How to

To get this project into your build:

### Step 1. Add the JitPack repository

#### Gradle (`settings.gradle`)

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

#### Gradle Kotlin (`settings.gradle.kts`)

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

#### Maven (`pom.xml`)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### Step 2. Add the dependency

Replace `Tag` with a release tag, commit hash, or `main-SNAPSHOT`.

Fat jar module (recommended if the aggregator POM is empty):

```kotlin
implementation("com.github.OPmasterLEO.PacketUxUi:packetuxui:Tag")
```

#### Gradle

```gradle
dependencies {
    implementation 'com.github.OPmasterLEO:PacketUxUi:Tag'
    // or: implementation 'com.github.OPmasterLEO.PacketUxUi:packetuxui:Tag'
}
```

#### Gradle Kotlin

```kotlin
dependencies {
    implementation("com.github.OPmasterLEO:PacketUxUi:Tag")
    // or: implementation("com.github.OPmasterLEO.PacketUxUi:packetuxui:Tag")
}
```

#### Maven

```xml
<dependency>
    <groupId>com.github.OPmasterLEO</groupId>
    <artifactId>PacketUxUi</artifactId>
    <version>Tag</version>
</dependency>
```

Or the fat module directly:

```xml
<dependency>
    <groupId>com.github.OPmasterLEO.PacketUxUi</groupId>
    <artifactId>packetuxui</artifactId>
    <version>Tag</version>
</dependency>
```

When shading into your plugin, relocate and keep the library tree if you use `minimize()`:

```kotlin
tasks.shadowJar {
    // relocate("net.opmasterleo.packetuxui", "your.plugin.lib.packetuxui")
    // minimize {
    //     exclude(dependency("com.github.OPmasterLEO:PacketUxUi:.*"))
    // }
}
```

### Local Maven (optional)

```bash
./gradlew :packetuxui:publishToMavenLocal
```

```kotlin
repositories { mavenLocal() }
dependencies { implementation("net.opmasterleo:packetuxui:1.0.0") }
```

## Use

### Init in your plugin

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

### PacketGuiManager (GuiManager replace)

**Full GuiManager replace requires `MenuMode.EDITABLE` + `PacketGuiManager` facade.**

```java
PacketGuiManager gui = PacketMenus.gui();

gui.setScopeListener((player, open, topSlots) -> {
    // Worth / lore overlays: open fires BEFORE content packets; close AFTER cleanup
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

gui.present(player, page);           // same size → differential Set Slot; else reopen
gui.presentAsync(player, () -> buildHeavyPage());
gui.updateTitle(player, Component.text("Shop (2/3)")); // in-place title (same window id)
gui.patchSlots(player, Map.of(13, stack));
gui.close(player);

int windowId = gui.getWindowId(player);     // 100–126 while open, else -1
int topCount = gui.getTopSlotCount(player); // overlay transformers skip plugin top
```

### Patch a single slot (Set Slot, not full Window Items)

```java
PacketMenus.patchSlot(player, 13, UxItem.builder("minecraft:diamond").build());
// unchanged items are skipped (equals check: material + amount + display/meta fingerprint)
PacketMenus.refresh(player); // full Window Items only when you need a full rebuild
```

### READ_ONLY vs EDITABLE

| | READ_ONLY | EDITABLE |
|--|-----------|----------|
| Top clicks | Cancel + dirty Set Slot; run ACTION handlers | DECORATIVE cancel; ACTION handler; EDITABLE place/take |
| Bottom clicks | Light settle (inv snapshot Set Slot / cursor) | Light settle; shift-click merges into EDITABLE tops |
| Drag | Cancel | Only if all touched top slots are EDITABLE |
| Number / offhand / double-collect | Cancel | Denied by default |
| Cursor on close | Cleared | Reclaimed into player inv (leftover dropped) |
| Real inv writes | Never | Only shift-from-bottom + close reclaim |
| Overlays | Reports `getTopSlotCount` | Same |

Slot kinds (`SlotKind`): `DECORATIVE`, `ACTION`, `EDITABLE`. Unspecified slots in `EDITABLE` menus default to editable.

Takeable predicates (`registerTakeablePredicate` / `registerTakeablePredicateBukkit`) allow special items to leave restricted top slots.

### Migration from Bukkit GuiManager / YamlGui

| DonutSMP / Bukkit | PacketUxUi |
|-------------------|------------|
| `GuiManager.open` | `PacketGuiManager.open` / `present` |
| `GuiManager.close` | `PacketGuiManager.close` |
| `YamlGui.present` / in-place refresh | `present` / `MenuBuild.applyTo` (differential) |
| `YamlGuiPayload` builder | `MenuBuild` (`title`, `rows`, `item`, `itemOwned`, `onClose`, `materialize`) |
| `EditableGui` / `FastEditableGui` | `MenuMode.EDITABLE` + `SlotKind` / `editableSlot` |
| `GuiSessionGuard` phases / debounce / gen | `SessionPhase` + debounce + `generation` (stale async no-op) |
| `PluginGuiScopeListener` | `GuiScopeListener` / `PacketGuiManager.setScopeListener` |
| Window id for overlays | `getWindowId` / `getTopSlotCount` |

## Modules

| Module | Artifact | Purpose |
|--------|----------|---------|
| `packetuxui` | `net.opmasterleo:packetuxui` | **Publish this** — API + nms-api + all adapters shaded |
| `API` | `net.opmasterleo:packetuxui-api` | Thin compile API (no adapters) |
| `nms-api` | — | Bridges / `UxItem` / `AdapterLoader` |
| `nms:*` | — | Per-version adapters (1.8–26.2) |
| `TestMenu` | — | Demo Paper plugin |

## Build / publish locally

```bash
./gradlew.bat :packetuxui:publishToMavenLocal
./gradlew.bat :TestMenu:shadowJar
```

## Packet updates

| When | Packet |
|------|--------|
| Open menu | Open Window + Window Items |
| Title-only / same-size present | Open Window (title) + dirty Set Slot |
| Click settle / patch 1–N slots | Set Slot (dirty only) |
| `refresh()` / heavy settle | Window Items |
| Close (API) | Close Window |

Never uses Bukkit `Player#updateInventory()` on the default click path.

## Window ids + overlays

Per-player virtual ids from a pool **100–126** (`BitSet.nextClearBit` reclaim). Exhaustion fails open gracefully (`setOpenFailedHandler`). Released on close / quit / kick / death. Sessions keyed by `UUID` only.

Overlays (e.g. Worth lore):

1. `setScopeListener` — **open before** content packets; **close after** session cleanup
2. Skip transforming outbound slots `0 .. getTopSlotCount()-1` for that player's `getWindowId()`
3. Works for both READ_ONLY and EDITABLE

## Folia / Paper

- Player-bound work (open, click, patch, shift-insert, pipeline inject) hops via `PlatformScheduler.runForPlayer` (entity scheduler on Folia).
- `presentAsync` / `updateAsync` build off-thread; apply on entity scheduler; stale generation is ignored.
- No global main-thread assumptions; no PacketEvents — direct NMS **1.8 → 26.2**.

## Design notes

- Prefer `READ_ONLY` for high-viewer leaderboards/stats
- Prefer `EDITABLE` for rearrange / deposit UIs (server-owned top `ItemStack[]`, cursor reclaim on close)
- Anti-dupe: ignore clicks while `OPENING`/`CLOSING`; ~100ms click debounce (configurable)

## Requirements

- JDK **21** (build / modern runtimes); older Spigot servers still work via versioned NMS adapters
- Supported servers: **Minecraft 1.8 → 26.2** (Spigot / Paper / Folia where available)
  - **1.8–1.20.4** — relocated CraftBukkit/NMS buckets
  - **1.20.5–26.2** — Mojang-mapped Paper adapters (Paper/Folia recommended)
