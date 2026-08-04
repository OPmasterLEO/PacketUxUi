package net.opmasterleo.packetuxui.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.opmasterleo.packetuxui.PacketUxUiAPI;
import net.opmasterleo.packetuxui.common.StringUtils;
import net.opmasterleo.packetuxui.nms.item.UxItem;
import net.opmasterleo.packetuxui.service.Button;
import net.opmasterleo.packetuxui.service.ButtonBuilder;
import net.opmasterleo.packetuxui.service.CloseSnapshot;
import net.opmasterleo.packetuxui.service.IButtonBuilder;
import net.opmasterleo.packetuxui.service.Menu;
import net.opmasterleo.packetuxui.service.MenuMode;
import net.opmasterleo.packetuxui.service.SlotKind;
import net.opmasterleo.packetuxui.types.ButtonType;
import net.opmasterleo.packetuxui.types.ClickType;
import net.opmasterleo.packetuxui.types.ExecuteComponent;
import net.opmasterleo.packetuxui.types.InventoryType;

public final class MenuBuild {

    private Component title = Component.empty();
    private int rows = 3;
    private MenuMode mode = MenuMode.READ_ONLY;
    private final Map<Integer, Button> buttons = new LinkedHashMap<>();
    private final Map<Integer, UxItem> ownedItems = new LinkedHashMap<>();
    private BiConsumer<Player, CloseSnapshot> onClose;
    private Consumer<Player> closePlayerOnly;

    public static MenuBuild create() {
        return new MenuBuild();
    }

    public MenuBuild title(Component title) {
        this.title = title == null ? Component.empty() : title;
        return this;
    }

    public MenuBuild title(String miniMessageOrLegacy) {
        this.title = StringUtils.toComponent(miniMessageOrLegacy == null ? "" : miniMessageOrLegacy);
        return this;
    }

    public MenuBuild rows(int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("rows must be 1..6");
        }
        this.rows = rows;
        return this;
    }

    public MenuBuild mode(MenuMode mode) {
        this.mode = mode == null ? MenuMode.READ_ONLY : mode;
        return this;
    }

    public MenuBuild readOnly() {
        return mode(MenuMode.READ_ONLY);
    }

    public MenuBuild editable() {
        return mode(MenuMode.EDITABLE);
    }

    public MenuBuild item(int slot, ItemStack stack) {
        return item(slot, stack, (Consumer<Player>) null, defaultItemKind());
    }

    public MenuBuild item(int slot, ItemStack stack, Consumer<Player> click) {
        return item(slot, stack, click, click != null ? SlotKind.ACTION : defaultItemKind());
    }

    public MenuBuild item(int slot, ItemStack stack, Consumer<Player> click, SlotKind kind) {
        Objects.requireNonNull(stack, "stack");
        UxItem ux = PacketUxUiAPI.getAdapter().items().fromBukkit(stack.clone());
        return put(slot, ux, click, null, kind == null ? defaultItemKind() : kind);
    }

    public MenuBuild item(int slot, ItemStack stack, BiConsumer<Player, ClickType> click) {
        Objects.requireNonNull(stack, "stack");
        UxItem ux = PacketUxUiAPI.getAdapter().items().fromBukkit(stack.clone());
        return put(slot, ux, null, click, click != null ? SlotKind.ACTION : defaultItemKind());
    }

    public MenuBuild itemOwned(int slot, ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        UxItem ux = PacketUxUiAPI.getAdapter().items().fromBukkit(stack);
        ownedItems.put(slot, ux);
        return put(slot, ux, null, null, mode == MenuMode.EDITABLE ? SlotKind.EDITABLE : SlotKind.ACTION);
    }

    public MenuBuild itemOwned(int slot, ItemStack stack, Consumer<Player> click, SlotKind kind) {
        Objects.requireNonNull(stack, "stack");
        UxItem ux = PacketUxUiAPI.getAdapter().items().fromBukkit(stack);
        ownedItems.put(slot, ux);
        return put(slot, ux, click, null, kind == null ? SlotKind.ACTION : kind);
    }

    public MenuBuild editableSlot(int slot, ItemStack stack) {
        return item(slot, stack == null ? new ItemStack(org.bukkit.Material.AIR) : stack, (Consumer<Player>) null, SlotKind.EDITABLE);
    }

    public MenuBuild decorative(int slot, ItemStack stack) {
        return item(slot, stack, (Consumer<Player>) null, SlotKind.DECORATIVE);
    }

    public MenuBuild action(int slot, ItemStack stack, Consumer<Player> click) {
        return item(slot, stack, click, SlotKind.ACTION);
    }

    public MenuBuild sealUnspecifiedTopSlots(SlotKind kind, ItemStack filler) {
        SlotKind nextKind = kind == null ? SlotKind.DECORATIVE : kind;
        UxItem ux = filler == null
                ? UxItem.EMPTY
                : PacketUxUiAPI.getAdapter().items().fromBukkit(filler.clone());
        for (int slot = 0; slot < type().size(); slot++) {
            if (buttons.containsKey(slot)) {
                continue;
            }
            IButtonBuilder builder = new ButtonBuilder().item(ux).kind(nextKind);
            buttons.put(slot, builder.build());
        }
        return this;
    }

    public MenuBuild onClose(Consumer<Player> onClose) {
        this.closePlayerOnly = onClose;
        return this;
    }

    public MenuBuild onClose(BiConsumer<Player, CloseSnapshot> onClose) {
        this.onClose = onClose;
        return this;
    }

    public InventoryType type() {
        return switch (rows) {
            case 1 -> InventoryType.GENERIC9X1;
            case 2 -> InventoryType.GENERIC9X2;
            case 3 -> InventoryType.GENERIC9X3;
            case 4 -> InventoryType.GENERIC9X4;
            case 5 -> InventoryType.GENERIC9X5;
            default -> InventoryType.GENERIC9X6;
        };
    }

    public Menu materialize() {
        BiConsumer<Player, CloseSnapshot> close = onClose;
        if (close == null && closePlayerOnly != null) {
            Consumer<Player> playerOnly = closePlayerOnly;
            close = (player, snap) -> playerOnly.accept(player);
        }
        return new Menu(title, type(), buttons, new net.opmasterleo.packetuxui.dto.CooldownComponent(), mode, close);
    }

    public void applyTo(Player player) {
        PacketUxUiAPI.getService().present(player, materialize());
    }

    public Component title() {
        return title;
    }

    public MenuMode mode() {
        return mode;
    }

    public Map<Integer, Button> buttons() {
        return Map.copyOf(buttons);
    }

    public int rows() {
        return rows;
    }

    public LayoutDiagnostics validateLayout() {
        ArrayList<LayoutIssue> issues = new ArrayList<>();
        int size = type().size();
        for (Integer slot : buttons.keySet()) {
            if (slot == null || slot < 0 || slot >= size) {
                issues.add(new LayoutIssue(slot == null ? -1 : slot, "OUT_OF_BOUNDS", "Button slot out of bounds"));
            }
        }
        if (mode == MenuMode.EDITABLE) {
            for (int slot = 0; slot < size; slot++) {
                if (!buttons.containsKey(slot)) {
                    issues.add(new LayoutIssue(slot, "UNSEALED_EDITABLE_SLOT", "Editable menu has unspecified slot"));
                }
            }
        }
        return new LayoutDiagnostics(List.copyOf(issues));
    }

    public LayoutDiagnostics validateLayout(LayoutPlan plan) {
        if (plan == null) {
            return validateLayout();
        }
        ArrayList<LayoutIssue> issues = new ArrayList<>(validateLayout().issues());
        validateGroupOverlap(issues, "CONTENT_ACTION_COLLISION", plan.contentSlots(), plan.actionSlots());
        validateGroupOverlap(issues, "CONTENT_FOOTER_COLLISION", plan.contentSlots(), plan.footerSlots());
        validateGroupOverlap(issues, "CONTENT_NAV_COLLISION", plan.contentSlots(), plan.navigationSlots());
        validateGroupOverlap(issues, "ACTION_FOOTER_COLLISION", plan.actionSlots(), plan.footerSlots());
        validateGroupOverlap(issues, "ACTION_NAV_COLLISION", plan.actionSlots(), plan.navigationSlots());
        validateGroupOverlap(issues, "FOOTER_NAV_COLLISION", plan.footerSlots(), plan.navigationSlots());
        return new LayoutDiagnostics(List.copyOf(issues));
    }

    public void validateLayoutOrThrow() {
        LayoutDiagnostics diagnostics = validateLayout();
        if (!diagnostics.ok()) {
            throw new IllegalStateException("Invalid menu layout: " + diagnostics.issues());
        }
    }

    private static void validateGroupOverlap(
            ArrayList<LayoutIssue> issues,
            String code,
            Set<Integer> left,
            Set<Integer> right
    ) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return;
        }
        Set<Integer> overlap = new HashSet<>(left);
        overlap.retainAll(right);
        for (Integer slot : overlap) {
            issues.add(new LayoutIssue(slot == null ? -1 : slot, code, "Layout slot collision"));
        }
    }

    private SlotKind defaultItemKind() {
        return mode == MenuMode.EDITABLE ? SlotKind.EDITABLE : SlotKind.ACTION;
    }

    private MenuBuild put(
            int slot,
            UxItem ux,
            Consumer<Player> click,
            BiConsumer<Player, ClickType> typedClick,
            SlotKind kind
    ) {
        int size = type().size();
        if (slot < 0 || slot >= size) {
            throw new IllegalArgumentException("Slot out of range: " + slot);
        }
        IButtonBuilder builder = new ButtonBuilder().item(ux).kind(kind);
        if (typedClick != null) {
            builder.click((ExecuteComponent ctx) -> typedClick.accept(ctx.player(), toClickType(ctx.buttonType())));
        } else if (click != null) {
            builder.click((ExecuteComponent ctx) -> click.accept(ctx.player()));
        }
        buttons.put(slot, builder.build());
        return this;
    }

    private static ClickType toClickType(ButtonType buttonType) {
        if (buttonType == null) {
            return ClickType.PICKUP;
        }
        return switch (buttonType) {
            case SHIFT_LEFT, SHIFT_RIGHT -> ClickType.SHIFT_CLICK;
            case RIGHT -> ClickType.PLACE;
            default -> ClickType.PICKUP;
        };
    }
}
