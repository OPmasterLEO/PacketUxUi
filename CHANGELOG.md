# Changelog

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

