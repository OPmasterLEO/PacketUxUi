package net.opmasterleo.packetuxui.service;

/**
 * Top-slot interaction policy.
 * <ul>
 *   <li>{@link #DECORATIVE} — display only</li>
 *   <li>{@link #ACTION} — click handlers (read-only menus)</li>
 *   <li>{@link #EDITABLE} — place and take (inv↔gui)</li>
 *   <li>{@link #EXTRACTABLE} — take only (gui→inv); cannot place from inv/cursor</li>
 * </ul>
 */
public enum SlotKind {
    DECORATIVE,
    ACTION,
    EDITABLE,
    EXTRACTABLE
}
