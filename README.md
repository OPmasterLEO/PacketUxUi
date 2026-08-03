# PacketUxUi

[JitPack](https://jitpack.io/#OPmasterLEO/PacketUxUi)

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

One fat jar — API + all NMS adapters (1.8 → 26.2) shaded in. Do **not** depend on a separate API module.

#### Gradle

```gradle
dependencies {
    implementation 'com.github.OPmasterLEO.PacketUxUi:packetuxui:Tag'
}
```

#### Gradle Kotlin

```kotlin
dependencies {
    implementation("com.github.OPmasterLEO.PacketUxUi:packetuxui:Tag")
}
```

#### Maven

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
    //     exclude(dependency("com.github.OPmasterLEO.PacketUxUi:packetuxui:.*"))
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



### PacketGuiManager

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


|                                   | READ_ONLY                                     | EDITABLE                                               |
| --------------------------------- | --------------------------------------------- | ------------------------------------------------------ |
| Top clicks                        | Cancel + dirty Set Slot; run ACTION handlers  | DECORATIVE cancel; ACTION handler; EDITABLE place/take |
| Bottom clicks                     | Light settle (inv snapshot Set Slot / cursor) | Light settle; shift-click merges into EDITABLE tops    |
| Drag                              | Cancel                                        | Only if all touched top slots are EDITABLE             |
| Number / offhand / double-collect | Cancel                                        | Denied by default                                      |
| Cursor on close                   | Cleared                                       | Reclaimed into player inv (leftover dropped)           |
| Real inv writes                   | Never                                         | Only shift-from-bottom + close reclaim                 |
| Overlays                          | Reports `getTopSlotCount`                     | Same                                                   |


Slot kinds (`SlotKind`): `DECORATIVE`, `ACTION`, `EDITABLE`. Unspecified slots in `EDITABLE` menus default to editable.

Takeable predicates (`registerTakeablePredicate` / `registerTakeablePredicateBukkit`) allow special items to leave restricted top slots.



## Modules


| Module       | Artifact                     | Purpose                                              |
| ------------ | ---------------------------- | ---------------------------------------------------- |
| `packetuxui` | `net.opmasterleo:packetuxui` | **Only published artifact** — API + all NMS shaded |
| `API`        | — (internal)                 | Public API sources (bundled into fat jar)            |
| `nms-api`    | — (internal)                 | Bridges / `UxItem` / `AdapterLoader`                 |
| `nms:*`      | — (internal)                 | Per-version adapters (1.8–26.2)                      |
| `TestMenu`   | —                            | Demo Paper plugin                                    |




## Build / publish locally

Legacy / Spigot-mapped NMS jars resolve from [CodeMC NMS](https://docs.codemc.io/faq/using-nms-repository/) (`repo.codemc.org/repository/nms/`). Modern Paper adapters (`1.20.5+`) use paperweight and need network on first setup.

```bash
./gradlew.bat :packetuxui:publishToMavenLocal
./gradlew.bat :TestMenu:shadowJar
```



## Packet updates


| When                           | Packet                               |
| ------------------------------ | ------------------------------------ |
| Open menu                      | Open Window + Window Items           |
| Title-only / same-size present | Open Window (title) + dirty Set Slot |
| Click settle / patch 1–N slots | Set Slot (dirty only)                |
| `refresh()` / heavy settle     | Window Items                         |
| Close (API)                    | Close Window                         |


Never uses Bukkit `Player#updateInventory()` on the default click path.

## Window ids + overlays

Per-player virtual ids from a pool **100–126** (`BitSet.nextClearBit` reclaim). Exhaustion fails open gracefully (`setOpenFailedHandler`). Released on close / quit / kick / death. Sessions keyed by `UUID` only.

Overlays (e.g. Worth lore):

1. `setScopeListener` — **open before** content packets; **close after** session cleanup
2. Skip transforming outbound slots `0 .. getTopSlotCount()-1` for that player's `getWindowId()`
3. Works for both READ_ONLY and EDITABLE



## Folia / Paper

Schedulers and lifecycle listeners are **split at init** (no per-call Paper/Bukkit branching on hot paths):


| Backend                | When                 | Sync / entity                              | Async                  | Events                                              |
| ---------------------- | -------------------- | ------------------------------------------ | ---------------------- | --------------------------------------------------- |
| `SchedulerKind.BUKKIT` | Spigot / CraftBukkit | `BukkitScheduler` main thread              | async Bukkit tasks     | `BukkitLifecycleListener`                           |
| `SchedulerKind.PAPER`  | Paper / Folia        | entity + region + global-region schedulers | Paper `AsyncScheduler` | `PaperLifecycleListener` (inline when region-owned) |


- `PlatformScheduler` selects one backend once; `runForPlayer` / patches / clicks use that backend only.
- Spigot never loads Paper scheduler classes (separate packages: `scheduler.bukkit` / `scheduler.paper`).
- `presentAsync` / `updateAsync` build off-thread; apply on entity/main scheduler; stale generation is ignored.
- No PacketEvents — direct NMS **1.8 → 26.2**.



## Design notes

- Prefer `READ_ONLY` for high-viewer leaderboards/stats
- Prefer `EDITABLE` for rearrange / deposit UIs (server-owned top `ItemStack[]`, cursor reclaim on close)
- Anti-dupe: ignore clicks while `OPENING`/`CLOSING`; ~100ms click debounce (configurable)



## Requirements

- JDK **21** to run Gradle; JDK **25** toolchain is auto-provisioned (Foojay) for Minecraft 26.x paperweight modules
- Supported servers: **Minecraft 1.8 → 26.2** (Spigot / Paper / Folia where available)
  - **1.8–1.20.4** — relocated CraftBukkit/NMS buckets
  - **1.20.5–26.2** — Mojang-mapped Paper adapters (Paper/Folia recommended)

