# Changelog

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
