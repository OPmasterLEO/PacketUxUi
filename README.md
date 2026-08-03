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

### Patch a single slot (Set Slot, not full Window Items)

```java
PacketMenus.patchSlot(player, 13, UxItem.builder("minecraft:diamond").build());
// unchanged items are skipped (equals check)
PacketMenus.refresh(player); // full Window Items only when you need a full rebuild
```

**Menu modes**
- `READ_ONLY` (default) — buttons only; clicks cancelled + dirty Set Slot; never mutates real inventory
- `EDITABLE` — packet-based top-slot movement; bottom strip mirrored read-only; cursor discarded on close unless you handle `onClose`
- `EDITABLE_PLAYER_INVENTORY` (deprecated) — injects bottom clicks into real inventory; prefer `EDITABLE` or Bukkit GUIs for ownership

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
| Click settle / patch 1–N slots | Set Slot (dirty only) |
| `refresh()` / EDITABLE bottom resync | Window Items |
| Close (API) | Close Window |

Never uses Bukkit `Player#updateInventory()` on the default click path.

## Window ids

Per-player virtual ids from a pool **100–126**. Released on close/quit. Sessions and caches are keyed by `UUID`, not `Player`.

## Folia / Paper

- Player-bound work (open, click, patch, pipeline inject) hops via `PlatformScheduler.runForPlayer` (entity scheduler on Folia, main thread on Spigot).
- Global repeating tasks use the global region scheduler when available.
- No PacketEvents dependency — direct NMS adapters for **1.8 → 26.2**.

## Design notes

- Prefer `READ_ONLY` for high-viewer leaderboards/stats
- `EDITABLE` for virtual rearrange UIs; commit via `onClose` yourself — library does not write real inventory
- Keep Bukkit EditableGui for sell/order ownership flows if you need true item transfer

## Requirements

- JDK **21** (build / modern runtimes); older Spigot servers still work via versioned NMS adapters
- Supported servers: **Minecraft 1.8 → 26.2** (Spigot / Paper / Folia where available)
  - **1.8–1.20.4** — relocated CraftBukkit/NMS buckets
  - **1.20.5–26.2** — Mojang-mapped Paper adapters (Paper/Folia recommended)
