/**
 * PacketUxUi public API.
 *
 * <h2>Dependency (recommended)</h2>
 * <pre>{@code
 * implementation("net.opmasterleo:packetuxui:0.7")
 * // then shade into your plugin jar
 * }</pre>
 *
 * <h2>Init</h2>
 * <pre>{@code
 * PacketUxUiAPI.init(this); // onEnable
 * PacketUxUiAPI.terminate(this); // onDisable
 * }</pre>
 *
 * <h2>Open a read-only menu</h2>
 * <pre>{@code
 * PacketMenus.menu("<gold>Stats", InventoryType.GENERIC9X3)
 *     .button(13, b -> b.item(UxItem.builder("minecraft:emerald").name(Component.text("OK")).build())
 *                      .click(ctx -> ctx.player().sendMessage("clicked")))
 *     .open(player);
 * }</pre>
 */
package net.opmasterleo.packetuxui;
