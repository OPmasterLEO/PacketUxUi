# Changelog

## 0.13.14

### Sign editor GUI

Open a vanilla sign editor for text input (no real sign placed):

```java
PacketMenus.sign()
    .line(0, "")
    .line(1, "<gray>^^^^^^^^")
    .line(2, "<gold>Type above")
    .color(DyeColor.YELLOW)
    .glow(false)
    .onFinish((player, result) -> {
        String name = result.plain(0);
        if (name.isEmpty()) {
            return SignAction.reopen("", "<gray>^^^^^^^^", "<gold>Type above");
        }
        player.sendMessage("Hello " + name);
        return SignAction.close();
    })
    .open(player);
```

- Fake sign 3 blocks behind the player (or `location(...)`); restored on close/quit
- Inbound `SignUpdate` is intercepted on the existing Netty pipeline and dropped (vanilla never edits a real block)
- `SignAction.close()` / `closeThen(Runnable)` / `reopen()` / `reopen(lines...)`
- MiniMessage, `§`, and `&` lines; dye color (1.15+) and glow (1.17+)
- Closes any open packet inventory / book first

### Artifact
`net.opmasterleo:packetuxui:0.13.14`

## 0.13.13

### Efficient InventoryClose analogue

- `PacketMenus.onInventoryClose(GuiCloseListener)` — direct close hook, no Bukkit bus
- Split from other hooks (`hasCloseListeners()`); includes `GuiCloseReason` + optional `CloseSnapshot`

### Artifact
`net.opmasterleo:packetuxui:0.13.13`

## 0.13.12

### Efficient InventoryClick / InventoryDrag analogues

- `PacketMenus.onInventoryClick(GuiClickListener)` / `onInventoryDrag(GuiDragListener)` — direct hooks, no Bukkit bus
- Open/click/drag interest is split: registering open-only no longer allocates click events (and vice versa)
- `GuiClickEvent.currentStack()` / `cursorStack()` (and drag `cursorStack()`) for Bukkit `ItemStack` when needed; prefer `UxItem` on hot paths
- No `EquipmentSlot` API — packet menus only expose container / player / hotbar / outside (`GuiSlotType`); armor equipment is outside this window model

### Artifact
`net.opmasterleo:packetuxui:0.13.12`

## 0.13.11

### Efficient InventoryOpen analogue

- `PacketMenus.onInventoryOpen(GuiOpenListener)` — direct open hook, no Bukkit `HandlerList` / plugin tree
- Zero alloc when nothing is registered (`hasOpenListeners()` gate)
- Click-only `GuiListener`s do not subscribe to opens
- Fires on fresh open and silent type-swap (`GuiOpenReason.OPEN` / `TYPE_SWAP`)
- Still available via `GuiListener.onOpen`

### Artifact
`net.opmasterleo:packetuxui:0.13.11`

## 0.13.10

### Fix: silent type swap (no close+open)

`present` with a different inventory type/size/mode while a menu is already open always uses OpenScreen-only swap (same window id). Mode mismatch no longer forces CloseWindow + reopen.

Also ignores the client's stale CloseWindow echo after a type change (grace while session stays OPEN), which previously tore the session down mid-swap.

### Artifact
`net.opmasterleo:packetuxui:0.13.10`

## 0.13.9

### Chore: single `nms-shared` variant tree

Deleted all `nms-shared-legacy*` / `mid*` / `modern*` folder copies. Shared sources live under one [`nms-shared/`](nms-shared/) tree keyed by content variant (`adapter/`, `item/`, `pipeline/`, `classifier/`, `menu/`, `limits/`). Each `nmsEra` picks the matching variant set in [`nms/shared-sources.gradle.kts`](nms/shared-sources.gradle.kts).

### Artifact
`net.opmasterleo:packetuxui:0.13.9`

## 0.13.8

### Chore: dedupe nms-shared sources

Identical shared files no longer live as 1:1 copies in every era tree. `prepareSharedSources` copies ordered base layers + era overlays (later wins). Bases: `nms-shared-legacy-base`, `nms-shared-mid-base`, `nms-shared-modern-common`, `nms-shared-modern21-base`, `nms-shared-modern-item`, `nms-shared-modern21_5-item`. Era dirs keep only divergent Menu/Classifier/Pipeline/Item files.

### Artifact
`net.opmasterleo:packetuxui:0.13.8`

## 0.13.7

### Full item NBT / components preserved

Items ingested from Bukkit (`fromBukkit` / menu fillers / player stacks) now keep an opaque Bukkit clone on `UxItem`. Packet conversion prefers that clone so enchantments, custom lore, potions, attributes, custom model data, skull textures, and other NBT/DataComponents survive and display correctly on the client.

Builder-created items still use the field DTO path (name, lore, enchants, CMD, head texture). `hideEnchantments` is read from `ItemFlag.HIDE_ENCHANTS` instead of always forced on.

### Artifact
`net.opmasterleo:packetuxui:0.13.7`

## 0.13.6

### Live NMS limits (no hardcoded vanilla copies)

Runtime values come from the loaded NMS jar via `ServerLimits` / `LiveLimits`:

- Book: `WritableBookContent.MAX_PAGES`, `PAGE_EDIT_LENGTH`
- Player inv: `Inventory.INVENTORY_SIZE`, `Inventory.getSelectionSize()`
- Hopper top: `HopperMenu.CONTAINER_SIZE`
- Window ids: counter min/max (mirrors `nextContainerCounter` cycle)
- Menu top sizes: resolved from live `MenuType` references where possible

`InventoryType`, `BookView`, `WindowIdPool`, `Slots`, and bottom snapshots all read through `LiveLimits` after `PacketUxUiAPI.init`.

### Artifact
`net.opmasterleo:packetuxui:0.13.6`

## 0.13.5

### Book page line builder

No more stuffing a whole page into one string with `\n`:

```java
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
```

Also: `page(p -> p.line(...).blank()...)`, `paragraph(...)`, `lines(...)`.

### Artifact
`net.opmasterleo:packetuxui:0.13.5`

## 0.13.4

### Written book GUI (text viewer)

Open a vanilla written-book screen for multi-page text (rules, lore, clickable links):

```java
PacketMenus.book()
    .title("<gold>Rules")
    .author("Server")
    .page("<white>Welcome!\n\nUse Adventure <aqua>ClickEvent</aqua>s for actions.")
    .page(Component.text("Page 2"))
    .open(player);
```

**Limits / notes**
- Max **100** pages; keep each page short (~1023 chars / ~14 lines)
- Page turns are client-side; no inventory slots or PacketUxUi buttons
- Use Adventure click events on page text (`run_command`, `change_page`, `open_url`, …)
- Closes any open packet inventory menu first; does not grant a real book item (Paper packet hand-swap)
- `onClose` is best-effort (displaced by another GUI / `close` / quit) — Esc alone may not fire it
- Prefer Paper/Folia (`Audience.openBook`); NMS hand-swap on 26.x; ItemStack `openBook` fallback when present

### Artifact
`net.opmasterleo:packetuxui:0.13.4`

## 0.13.3

### Session / GUI tracking harden (double-click stuck state)

Fast double-clicks that close a GUI could leave a stranded server session: client closed, close listener skipped, `hasOpen` stayed true → Bukkit clicks cancelled and new GUIs would not open until rejoin.

- `closeCurrent` always clears session/window/cursor maps in `finally` (packet send failures cannot strand CLOSING/OPEN)
- `hasOpen` is true only for `SessionPhase.OPEN` (not CLOSING leftovers)
- Close during transition no longer soft-ignored; pending `present` deferred until close finishes
- Stale click guards: window-id mismatch, closing flag, session identity after handlers
- Join resets tracking; quit/kick/death use full `onDisconnect` purge
- Public `PacketGuiManager.resetPlayer(Player)` for emergency clear

### Artifact
`net.opmasterleo:packetuxui:0.13.3`

## 0.13.2

### Silent type swap (no close/open feel)

`present` with same mode but different inventory type (hopper↔chest, 27↔54, …) now:

- Keeps the same window id and session (no teardown)
- Sends only `OpenScreen` + contents (protocol requires OpenScreen to change layout)
- Skips `CloseWindow`, `onClose`, GuiClose/Open churn, cursor wipe when staying EDITABLE
- Debug: `TYPE_SWAP` instead of CLOSE+OPEN

Hard close+open only on mode change or `reopen`.

### Artifact
`net.opmasterleo:packetuxui:0.13.2`

## 0.13.1

### Hopper / editable cursor place + close dup fix

- **Cursor place**: inv pickup is tracked in `bottomHeld`; placing into EDITABLE top now uses that cursor instead of restoring the item first (shift-click already worked)
- **Close dup**: flush empty top to the client before `CloseWindow` so vanilla cannot dump GUI items back into the inv alongside plugin `onClose` refunds
- **CloseSnapshot.cursor** includes `bottomHeld` (what the player actually sees on the cursor)
- Single cursor reclaim (no double reclaim of held + carried)
- In-place type replace (`present` same mode, different type) skips menu `onClose` — refresh-like, no false refunds

### Artifact
`net.opmasterleo:packetuxui:0.13.1`

## 0.13.0

### All vanilla inventory types on `MenuBuild`

- `PacketMenus.build().type(InventoryType)` / `.hopper()` / `.anvil()` / `.furnace()` / … for every Open Screen type
- Correct protocol sizes: hopper=5, brewing=5, lectern=1 (no player inv), crafter=9
- `InventoryType.bottomSlotCount()` / `totalProtocolSlots()` — MenuService no longer hardcodes `+36`
- `InventorySlots` named indices (`HOPPER_CENTER`, `ANVIL_RESULT`, …)
- Same modes on all types: readOnly / editable / extractable / action / decorative
- Chest bind still **generic 9×N only**; hopper/anvil/furnace/etc. are packet-only (correct Open Screen — do not fake hopper as `rows(1)`)

### Artifact
`net.opmasterleo:packetuxui:0.13.0`

## 0.12.10

### Full Folia/Paper scheduler surface + correct threads

- Entity: `run` / `runNextTick` (Folia `EntityScheduler.run`) / `runLater` / `runRepeating` + player helpers
- Region: location, chunk, and block — execute / next-tick / later / repeating
- Global + Async unchanged; `cancelAll` now also cancels `RegionScheduler` tasks
- Zero-delay schedules use next-tick / execute (no forced `max(1)` on delays)
- `TaskHandle.state()` maps Paper `ScheduledTask` execution state
- Pipeline inject/ensure/remove hop to the player entity thread when needed
- Init/shutdown use `runForEachOnlinePlayer` (Folia-safe)
- `runSync`/`runGlobal` documented as global-region only — menus stay on `runForPlayer`

### Artifact
`net.opmasterleo:packetuxui:0.12.10`

## 0.12.9

### Dedicated elastic menu worker pool (Paper / Folia / Spigot)

- `MenuWorkerPool`: CPU-derived sizing (core≈cpus/4, max≈75% of cores, hard cap 16); idle threads reclaim to **0** in 15s
- Scaling queue handoff so workers grow under load (not fill a fat queue first)
- Plugin ClassLoader only while a task runs; idle daemon threads use system CCL (reload-safe)
- Idempotent shutdown: interrupt, drain, purge — no retained runnables / player closures
- Saturate → reject (never `CallerRuns` on tick/entity threads)
- `presentAsync` / `presentAsyncFuture` use the pool + preload, then hop to entity/player scheduler
- Overrides: `-Dpacketuxui.menuWorkers.max|core|queue`
- Debug boot line logs `menuWorkers=core..max` and full diagnostics when debug is on

### Artifact
`net.opmasterleo:packetuxui:0.12.9`

## 0.12.8

### Silent size replace (27↔54 without flash)

`present` with same mode but different type/size now **reuses the window id** and skips
`CloseWindow` — only `OpenScreen` + contents. No inventory flash between 9x3 and 9x6.

- Same type+mode → differential SetSlots (unchanged)
- Same mode, different size → in-place replace (`REPLACE_IN_PLACE` debug)
- Mode change / cold open → full close+open
- `reopen` still forces hard close+open

### Artifact
`net.opmasterleo:packetuxui:0.12.8`

## 0.12.7

### Speed pass (hot paths, bounded caches)

- GuiEventManager: cached ordered listener array; skip click/drag/post alloc when no listeners
- Editable simulate: one predicate path (no double simulate)
- `writeBottom`: cache written list (no 36-slot Bukkit re-snapshot)
- VirtualClickSimulator / Menu.setItems: no defensive `List.copyOf` of full inventories
- ConversionCache: trim ~25% on overflow (no full wipe miss-storm)
- ItemBridge: `nmsPrototype` + Bukkit-then-NMS build; drop per-send HashMap
- Title/refresh: prefer `sendBoundAuthority` over Ux assemble
- Debug string concat gated on close

No unbounded caches / no PE reflection.

### Artifact
`net.opmasterleo:packetuxui:0.12.7`

## 0.12.6

### Instant refresh + extractable slots

- Prefer `present` / `patchSlots` / `refresh` for same type+mode (no close→open, cursor stays)
- `reopen` only when size/type/mode change (flicker is expected)
- `SlotKind.EXTRACTABLE` / `extractableSlot` — gui→inv only (spawner-style)
- `editableSlot` — inv↔gui both ways (sell-style)
- Shift top→player inv; place top-cursor into player inv slots

### Artifact
`net.opmasterleo:packetuxui:0.12.6`

## 0.12.5

### Own runtime mapping caches

PacketUxUi mapping tables (not third-party):

- `BukkitKeyMaps` — material/enchant keys resolved once (`ConcurrentHashMap`)
- `ConversionCache` — lock-free UxItem→stack prototypes (replaces synchronized LRU)
- `OrdinalMaps` — click-type enum → NMS ordinal tables built at class init
- `ItemBridge.preload` on open/present so converts hit cache before first send
- Default material warmup at API init

Hot path stays direct NMS `instanceof` + cached converts — no reflective packet unwrap.

### Artifact
`net.opmasterleo:packetuxui:0.12.5`

## 0.12.4

### Hot-path performance (server-authoritative, cheap)

Same free-move guarantee, far less work:

- `sendBoundAuthority`: SetContent from bound NMS slots (no UxItem↔NMS convert on click/resync)
- Skip duplicate main-thread `resyncFull` when Netty already corrected the same `stateId`
- Open: mirror top + bound authority (no Bukkit bottom snapshot when bound)
- Present differential: no bottom re-snapshot; sparse SetSlot; dense/title → bound authority
- Debug: volatile flag only (no `getBoolean`/`getenv` per call); string concat gated

### Artifact
`net.opmasterleo:packetuxui:0.12.4`

## 0.12.3

### Server-authoritative GUIs (free-move nuclear)

Virtual menus must be owned by the server — same failure mode as client-handled GUIs.

- Pipeline inject **before** `packet_handler` first (vanilla never sees session clicks)
- Direct NMS {@code instanceof} click/close classify (per-bucket adapters)
- Bound `ChestMenu` slots: `mayPickup`/`mayPlace`/`remove` locked; empty `clicked()`
- On every intercepted click: full `SetContent` from menu top + **cached** bottom (Netty-safe) + empty cursor
- Never leak container click/close to vanilla while a PacketUxUi session is open

### Artifact
`net.opmasterleo:packetuxui:0.12.3`

## 0.12.2

### Bukkit-parity packet events

Packet-native wrappers mirroring Bukkit inventory events (no Bukkit inventory mutation races):

| Bukkit | PacketUxUi |
|---|---|
| `InventoryOpenEvent` | `GuiOpenEvent` + `GuiView` |
| `InventoryCloseEvent` | `GuiCloseEvent` + `GuiCloseReason` + snapshot |
| `InventoryClickEvent` | `GuiClickEvent` (`action`, `slotType`, `currentItem`, `cursor`, `hotbarButton`) |
| `InventoryDragEvent` | `GuiDragEvent` (`START`/`ADD`/`END`) |
| `InventoryAction` | `GuiClickAction` |
| `SlotType` | `GuiSlotType` |

Cancel `GuiClickEvent` / `GuiDragEvent` instead of Bukkit click events for virtual menus.

### Artifact
`net.opmasterleo:packetuxui:0.12.2`

## 0.12.1

### Robust READ_ONLY / pipeline (free-move fix)

- Pipeline inject **never** `addFirst` (undecoded → leaks to vanilla → client free moves)
- Prefer **before** `packet_handler` (decoded NMS), else after a known decoder
- `ensureInjected` on every `present` / open
- While session open: **all** container clicks swallowed (mismatch → force resync)
- Bukkit `InventoryClickEvent` / `InventoryDragEvent` cancelled while PacketUxUi menu open
- RO netty: provisional SetSlot(s) + SetCursorItem so Lunar accepts before player-thread SetContent

### Artifact

`net.opmasterleo:packetuxui:0.12.1`

## 0.12

**Supports** pagination / refresh / filters via composable APIs — does **not** ship PaginatedMenu / ConfirmMenu products.

- **Event bus**: `GuiListener` + `GuiClickEvent` (cancelable) / `GuiClickPostEvent` / `GuiOpenEvent` / `GuiCloseEvent` via `PacketMenus.registerListener` / `PacketUxUiAPI.getEventManager()`
- `MenuPackets`: public facade for open / setContent / setSlot / setCursor / close / stateId / bind
- `Slots`: row/col, border, rectangle, top/bottom index helpers
- `ExecuteComponent`: `clickType()`, `carried()`, `isTop()` / `isBottom()`
- Existing: `present` / `reopen` / `patchSlots` / `refresh` / `closeThen` (plugins build pages on top)



### Artifact

`net.opmasterleo:packetuxui:0.12`

## 0.11



### Protocol rewrite (vanilla-aligned)

- Window ids from `ServerPlayer.nextContainerCounter()` (1..100); drop magic 100–126 pool
- Bound `ChestMenu` owns `stateId` via `incrementStateId`; session only mirrors last sent
- Top stacks mirrored into the bound `SimpleContainer` on every SetSlot/SetContent
- READ_ONLY: netty sends **SetCursorItem(EMPTY) only**; player thread does **one** SetContent
- EDITABLE: one stateId bump per apply; client-floor on all click settlers (including RO bottom)
- Right-click `ClickData` mapping fixed (post-click carried, same as left)
- READ_ONLY drag START/ADD now settles optimistic ghosts
- Chest bind only for generic 9xN (hopper/anvil stay packet-only)



### Artifact

`net.opmasterleo:packetuxui:0.11`

## 0.10



### Point restored

PacketUxUi is a **packet-menu library** again: open, click, move items, close, hand off to SignGUI.

### Editable fixed

- Debounce no longer rejects EDITABLE take→place
- Editable Set Slot / cursor use `nextStateIdAbove(clientStateId)`
- `.editable()` + `item(...)` defaults to `SlotKind.EDITABLE`
- Simulate updates matching `Button` items so rematerialize doesn’t resurrect stacks



### Removed (fighting the product)

- `MenuMode.EDITABLE_PLAYER_INVENTORY` + vanilla `injectClick` inventory path
- `PresentMode` / `PresentOptions` / `RefreshStrategy`
- `AsyncMenuResult` / `AsyncMenuStatus` / `presentOrUpdate` / `updateIfOpen` / `refreshPage`
- `ClickDecision` / `setStrictActionMode` / `setClickDecisionListener`
- Public `beginTransition` / `endTransition` / `TransitionToken` (internal to `closeThen` only)
- `update(...)` aliases (use `present`)



### Keep

`present` / `reopen` / `close` / `closeThen` / `presentAsync` / patch / refresh / `sealUnspecifiedTopSlots` / takeable predicates / NMS bind+pipeline

### Artifact

`net.opmasterleo:packetuxui:0.10`

## 0.9

- Netty-safe read-only cursor correction, `closeThen`, AC bind, pipeline after decoder

