# PacketUxUi

Virtual packet menus for Paper / Spigot / Folia (**Minecraft 1.8 → 26.x**).  
No Bukkit `Inventory` open — Netty click handling, dirty-slot updates, Folia entity hops.

**Coords:** `net.opmasterleo:packetuxui:1.0.0` (fat jar with all NMS adapters)

## Use as a dependency

### Gradle

```kotlin
repositories {
    mavenLocal() // after publishToMavenLocal
    // or your private Maven
}

dependencies {
    implementation("net.opmasterleo:packetuxui:1.0.0")
}

tasks.shadowJar {
    // Recommended when embedding into your plugin:
    // relocate("net.opmasterleo.packetuxui", "your.plugin.lib.packetuxui")
}
```

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
- `READ_ONLY` (default) — leaderboards / stats / worth browse; no real inventory mutation
- `EDITABLE_PLAYER_INVENTORY` — bottom bar injects into the player inventory (keep Bukkit GUIs for sell/order ownership)

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
