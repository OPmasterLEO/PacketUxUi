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

/**
 * Fluent menu builder for every {@link InventoryType}.
 * <p>
 * Use {@link #rows(int)} for generic 9×N chests, or {@link #type(InventoryType)} /
 * {@link #hopper()} / {@link #anvil()} / … for correct protocol layouts (do not fake
 * hopper as {@code rows(1)}).
 * <p>
 * Modes ({@link #readOnly()}, {@link #editable()}, extractable/action/decorative slots)
 * work the same on chest and packet-only types. Only generic 9×N get NMS chest bind.
 */
public final class MenuBuild {

    private Component title = Component.empty();
    private InventoryType type = InventoryType.GENERIC9X3;
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

    /**
     * Generic chest rows 1–6 → {@link InventoryType#GENERIC9X1}…{@link InventoryType#GENERIC9X6}.
     * Prefer {@link #type(InventoryType)} / {@link #hopper()} for non-chest screens.
     */
    public MenuBuild rows(int rows) {
        this.type = InventoryType.genericRows(rows);
        return this;
    }

    /** Any vanilla Open Screen type (hopper, anvil, furnace, …). */
    public MenuBuild type(InventoryType type) {
        this.type = Objects.requireNonNull(type, "type");
        return this;
    }

    public MenuBuild hopper() {
        return type(InventoryType.HOPPER);
    }

    public MenuBuild anvil() {
        return type(InventoryType.ANVIL);
    }

    public MenuBuild furnace() {
        return type(InventoryType.FURNACE);
    }

    public MenuBuild smoker() {
        return type(InventoryType.SMOKER);
    }

    public MenuBuild blastFurnace() {
        return type(InventoryType.BLAST_FURNACE);
    }

    public MenuBuild brewingStand() {
        return type(InventoryType.BREWING_STAND);
    }

    public MenuBuild grindstone() {
        return type(InventoryType.GRINDSTONE);
    }

    public MenuBuild smithingTable() {
        return type(InventoryType.SMITHING_TABLE);
    }

    public MenuBuild loom() {
        return type(InventoryType.LOOM);
    }

    public MenuBuild cartographyTable() {
        return type(InventoryType.CARTOGRAPHY_TABLE);
    }

    public MenuBuild stonecutter() {
        return type(InventoryType.STONECUTTER);
    }

    public MenuBuild beacon() {
        return type(InventoryType.BEACON);
    }

    public MenuBuild enchantmentTable() {
        return type(InventoryType.ENCHANTMENT_TABLE);
    }

    public MenuBuild craftingTable() {
        return type(InventoryType.CRAFTING_TABLE);
    }

    /** Dispenser / dropper 3×3. */
    public MenuBuild dispenser() {
        return type(InventoryType.GENERIC3X3);
    }

    public MenuBuild generic3x3() {
        return type(InventoryType.GENERIC3X3);
    }

    public MenuBuild shulkerBox() {
        return type(InventoryType.SHULKER_BOX);
    }

    /** Merchant / villager trading screen. */
    public MenuBuild merchant() {
        return type(InventoryType.VILLAGER);
    }

    public MenuBuild villager() {
        return type(InventoryType.VILLAGER);
    }

    public MenuBuild lectern() {
        return type(InventoryType.LECTERN);
    }

    public MenuBuild crafter() {
        return type(InventoryType.CRAFTER3X3);
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
        UxItem ux = PacketUxUiAPI.getAdapter().items().fromBukkit(stack);
        return put(slot, ux, click, null, kind == null ? defaultItemKind() : kind);
    }

    public MenuBuild item(int slot, ItemStack stack, BiConsumer<Player, ClickType> click) {
        Objects.requireNonNull(stack, "stack");
        UxItem ux = PacketUxUiAPI.getAdapter().items().fromBukkit(stack);
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

    /** Take-only top slot (gui→inv); rejects place / shift-from-inv. */
    public MenuBuild extractableSlot(int slot, ItemStack stack) {
        return item(slot, stack == null ? new ItemStack(org.bukkit.Material.AIR) : stack, (Consumer<Player>) null, SlotKind.EXTRACTABLE);
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
                : PacketUxUiAPI.getAdapter().items().fromBukkit(filler);
        for (int slot = 0; slot < type.size(); slot++) {
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
        return type;
    }

    public Menu materialize() {
        BiConsumer<Player, CloseSnapshot> close = onClose;
        if (close == null && closePlayerOnly != null) {
            close = new PlayerOnlyClose(closePlayerOnly);
        }
        return new Menu(title, type, buttons, new net.opmasterleo.packetuxui.dto.CooldownComponent(), mode, close);
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

    /**
     * Chest rows for generic 9×N / shulker; {@code -1} for hopper, anvil, furnace, etc.
     */
    public int rows() {
        return type.chestRows();
    }

    public LayoutDiagnostics validateLayout() {
        ArrayList<LayoutIssue> issues = new ArrayList<>();
        int size = type.size();
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
        int size = type.size();
        if (slot < 0 || slot >= size) {
            throw new IllegalArgumentException(
                    "Slot out of range: " + slot + " for " + type + " (size " + size + ")"
            );
        }
        IButtonBuilder builder = new ButtonBuilder().item(ux).kind(kind);
        if (typedClick != null) {
            builder.click(new TypedClickAdapter(typedClick));
        } else if (click != null) {
            builder.click(new PlayerClickAdapter(click));
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

    private static final class PlayerOnlyClose implements BiConsumer<Player, CloseSnapshot> {
        private final Consumer<Player> playerOnly;

        private PlayerOnlyClose(Consumer<Player> playerOnly) {
            this.playerOnly = playerOnly;
        }

        @Override
        public void accept(Player player, CloseSnapshot snap) {
            playerOnly.accept(player);
        }
    }

    private static final class TypedClickAdapter implements Consumer<ExecuteComponent> {
        private final BiConsumer<Player, ClickType> typedClick;

        private TypedClickAdapter(BiConsumer<Player, ClickType> typedClick) {
            this.typedClick = typedClick;
        }

        @Override
        public void accept(ExecuteComponent ctx) {
            typedClick.accept(ctx.player(), toClickType(ctx.buttonType()));
        }
    }

    private static final class PlayerClickAdapter implements Consumer<ExecuteComponent> {
        private final Consumer<Player> click;

        private PlayerClickAdapter(Consumer<Player> click) {
            this.click = click;
        }

        @Override
        public void accept(ExecuteComponent ctx) {
            click.accept(ctx.player());
        }
    }
}
