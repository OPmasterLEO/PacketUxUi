# PacketUxUi

Modern packet API for Minecraft menus and other UX/UI components.

> Repository: https://github.com/OceJlot/PacketUxUi

## Overview

`PacketUxUi` is a Kotlin-based multi-module Gradle project focused on packet-driven UX/UI systems for Minecraft servers.

It currently contains:

- **API** — the reusable packet/UI API module
- **TestMenu** — a Paper test plugin that depends on and demonstrates the API module

## Modules

### `API`
Core module intended for integration into plugins/projects.

- Kotlin JVM (`2.0.20`)
- Java toolchain: **21**
- Publishes with `maven-publish`
- Uses Shadow plugin for packaging
- Key compile-time dependencies:
  - `paper-api`
  - `packetevents-spigot`

### `TestMenu`
A Paper plugin module used to run and test UX/UI behavior in-game.

- Kotlin JVM (`2.0.20`)
- Java toolchain: **21**
- Depends on `:API`
- Includes run task support via `xyz.jpenilla.run-paper`
- Uses Shadow JAR during build
- Additional dependencies include:
  - `kotlinx-coroutines-core`
  - `cloud-paper`
  - `packetevents-spigot`

## Tech Stack

- **Language:** Kotlin
- **Build System:** Gradle (Kotlin DSL)
- **Platform:** Paper / Spigot ecosystem
- **Minecraft Target (run config):** 1.21.1
- **Java Version:** 21

## Project Structure

```text
PacketUxUi/
├─ API/
│  ├─ build.gradle.kts
│  └─ src/
├─ TestMenu/
│  ├─ build.gradle.kts
│  └─ src/
├─ build.gradle.kts
└─ settings.gradle.kts
```

## Requirements

- **JDK 21**
- **Gradle** (or use the Gradle wrapper if added)
- Internet access for Maven dependencies

## Build

From the repository root:

```bash
gradle build
```

This builds both modules, including the shaded artifacts where configured.

## Run Test Server (TestMenu)

To run the Paper development server (from the repository root):

```bash
gradle :TestMenu:runServer
```

This uses the configured run-paper setup in `TestMenu`.

## Publishing (API)

The `API` module is configured with `maven-publish`.

To publish to your local Maven repository:

```bash
gradle :API:publishToMavenLocal
```

## Notes

- Root project name: `PacketUxUi`
- Included modules: `API`, `TestMenu`
- Group: `net.craftoriya`

## License

No license file is currently present in the repository.  
If you plan to distribute this project, consider adding a `LICENSE` file.
