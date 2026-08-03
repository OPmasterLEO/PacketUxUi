# PacketUxUi

[![JitPack](https://jitpack.io/v/OPmasterLEO/PacketUxUi.svg)](https://jitpack.io/#OPmasterLEO/PacketUxUi)

Virtual packet menus for Paper / Spigot / Folia (**Minecraft 1.8 → 26.x**).  
No Bukkit `Inventory` open — Netty click handling, dirty-slot updates, Folia entity hops.

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

### Open a read-only menu

```java
PacketMenus.menu("<gold>Leaderboard", InventoryType.GENERIC9X6)
    .readOnly()
    .button(13, b -> b
        .item(UxItem.builder("minecraft:emerald")
            .name(Component.text("Refresh"))
            .build())
        .click(ctx -> ctx.player().sendMessage("clicked")))
    .open(player);
```

**Menu modes**
- `READ_ONLY` (default) — leaderboards / stats / worth browse; clicks reverted
- `EDITABLE` — packet-based top-slot movement; bottom strip mirrored read-only; cursor discarded on close unless you handle `onClose`
- `EDITABLE_PLAYER_INVENTORY` (deprecated) — injects bottom clicks into real inventory; prefer `EDITABLE`

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

## Design notes (DonutSMPCore-style)

- Never blanket `player.updateInventory()` on clicks — dirty `Set Slot` only
- Per-player virtual window id pool (`100–126`) + state ids
- Folia-safe: Netty → `PlatformScheduler.runForPlayer`
- Prefer PacketUxUi for **read-only** high-viewer menus; keep Bukkit EditableGui for item ownership

## Requirements

- JDK **21**
- Paper/Folia recommended for modern versions (Mojang-mapped adapters 1.20.5+)
