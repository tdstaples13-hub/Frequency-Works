package dev.frequencyworks.client;

import dev.frequencyworks.menu.PortMenu;
import dev.frequencyworks.network.ConfigNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** A single-screen Port editor: Powah-style six-side compass, two simultaneously
 * visible 3-row ghost-filter grids, and an ordinary Minecraft player inventory.
 */
public final class PortScreen extends AbstractContainerScreen<PortMenu> {
    private static final ResourceLocation CHEST_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int GRID_Y = 91;
    private static final int PLAYER_PANEL_Y = 174;
    // Direction.ordinal(): DOWN, UP, NORTH, SOUTH, WEST, EAST.
    // Three inert grey corners leave exactly six interactive direction buttons.
    private static final int[] FACE_LAYOUT = {-1, 1, -1, 4, 2, 5, -1, 3, 0};
    private static final String[] FACE_SYMBOLS = {"D", "U", "N", "S", "W", "E"};
    private static final String[] FACE_NAMES = {"Down", "Up", "North", "South", "West", "East"};
    private static final String[] MODES = {"Off", "Import", "Export", "Both"};
    private static final String[] MODE_WIRE = {"NONE", "INPUT", "OUTPUT", "BOTH"};
    private static final ChatFormatting[] MODE_COLORS = {
            ChatFormatting.GRAY, ChatFormatting.GREEN, ChatFormatting.GOLD, ChatFormatting.LIGHT_PURPLE
    };
    private String lastFaceChange = "";

    public PortScreen(PortMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = menu.panelWidth();
        imageHeight = 270;
        titleLabelY = 7;
        inventoryLabelY = PLAYER_PANEL_Y + 6;
    }

    public void refreshState() { rebuildWidgets(); }

    private void send(String action, String value, int slot) {
        PacketDistributor.sendToServer(new ConfigNetworking.Action(menu.portPos(), -1, action, value, slot));
    }

    private static String bankKey(boolean importing) { return importing ? "InputFilter" : "OutputFilter"; }
    private static String bankWire(boolean importing) { return importing ? "input" : "output"; }

    private int filterX(boolean importing) {
        int center = importing ? imageWidth / 4 : imageWidth * 3 / 4;
        return center - menu.columns() * 9;
    }

    private int faceGridX() { return imageWidth - 58; }

    @Override protected void init() {
        super.init();
        CompoundTag state = menu.state();
        if (!state.getBoolean("CanConfigure")) return;
        int[] currentModes = state.getIntArray("Faces");
        int x = leftPos + faceGridX();
        int y = topPos + 11;
        for (int cell = 0; cell < FACE_LAYOUT.length; cell++) {
            int direction = FACE_LAYOUT[cell];
            if (direction < 0) continue;
            int mode = direction < currentModes.length && currentModes[direction] >= 0 && currentModes[direction] < 4
                    ? currentModes[direction] : 0;
            int next = (mode + 1) % 4;
            Component caption = Component.literal(FACE_SYMBOLS[direction]).withStyle(MODE_COLORS[mode]);
            final int face = direction;
            final int nextMode = next;
            Button control = Button.builder(caption, b -> {
                lastFaceChange = FACE_NAMES[face] + ": " + MODES[nextMode];
                send("face", MODE_WIRE[nextMode], face);
            }).bounds(x + cell % 3 * 18, y + cell / 3 * 18, 17, 17).build();
            control.setTooltip(Tooltip.create(Component.literal(
                    FACE_NAMES[face] + ": " + MODES[mode] + " (click to cycle)")));
            addRenderableWidget(control);
        }
        Button transfer = Button.builder(Component.literal(state.getBoolean("Stack") ? "Full stack" : "One item"),
                b -> send("stack", Boolean.toString(!state.getBoolean("Stack")), 0))
                .bounds(leftPos + 8, topPos + 46, 103, 18).build();
        transfer.setTooltip(Tooltip.create(Component.literal("Transfer quantity mode")));
        addRenderableWidget(transfer);

        for (boolean importing : new boolean[]{true, false}) {
            final boolean bank = importing;
            CompoundTag rules = state.getCompound(bankKey(importing));
            boolean whitelist = !rules.contains("Whitelist") || rules.getBoolean("Whitelist");
            int center = importing ? imageWidth / 4 : imageWidth * 3 / 4;
            Button toggle = Button.builder(Component.literal(whitelist ? "Whitelist" : "Blacklist"), b ->
                    send("filter_mode", bankWire(bank) + ":" + (whitelist ? "black" : "white"), 0))
                    .bounds(leftPos + center - 43, topPos + 153, 86, 18).build();
            toggle.setTooltip(Tooltip.create(Component.literal(
                    (importing ? "Import" : "Export") + ": " + (whitelist ? "allow matching items" : "deny matching items"))));
            addRenderableWidget(toggle);
        }
    }

    /** No Minecraft world blur: the panel has its own readable opaque background. */
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x77000000);
    }

    @Override protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF747474);
        g.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2,
                topPos + PLAYER_PANEL_Y - 2, 0xFFC6C6C6);
        // The player's inventory is backed by genuine vanilla Slots, and drawn
        // using the exact vanilla chest texture segment for those slots.
        g.blit(CHEST_TEXTURE, leftPos + menu.inventoryLeft() - 7, topPos + PLAYER_PANEL_Y,
                0, 126, 176, 96);
        int baseX = leftPos + faceGridX();
        int baseY = topPos + 11;
        for (int cell = 0; cell < FACE_LAYOUT.length; cell++) {
            if (FACE_LAYOUT[cell] >= 0) continue;
            int x = baseX + (cell % 3) * 18;
            int y = baseY + (cell / 3) * 18;
            g.fill(x, y, x + 17, y + 17, 0xFF666666);
            g.fill(x + 2, y + 2, x + 15, y + 15, 0xFF898989);
        }
        for (boolean importing : new boolean[]{true, false}) {
            int base = filterX(importing);
            for (int slot = 0; slot < menu.filterCapacity(); slot++) {
                int x = leftPos + base + (slot % menu.columns()) * 18;
                int y = topPos + GRID_Y + (slot / menu.columns()) * 18;
                g.fill(x, y, x + 18, y + 18, 0xFF454545);
                g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
            }
        }
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, 0x383838, false);
        CompoundTag state = menu.state();
        if (state.getBoolean("Assigned")) {
            g.drawString(font, "Frequency " + state.getString("Frequency") +
                    (state.getBoolean("Private") ? " Private" : " Public"), 8, 24, 0x454545, false);
        }
        if (!lastFaceChange.isEmpty()) g.drawString(font, lastFaceChange, 8, 66, 0x453178, false);
        g.drawCenteredString(font, "IMPORT", imageWidth / 4, 78, 0x276B36);
        g.drawCenteredString(font, "EXPORT", imageWidth * 3 / 4, 78, 0x9B6318);
        g.drawString(font, playerInventoryTitle, menu.inventoryLeft(), inventoryLabelY, 0x404040, false);
        if (!state.getBoolean("CanConfigure"))
            g.drawString(font, "View only", 8, 51, 0xAA5228, false);
        String warning = state.getString("Warning");
        if (!warning.isEmpty()) g.drawString(font,
                warning.length() > 32 ? warning.substring(0, 29) + "..." : warning, 8, 66, 0xA13D2E, false);
    }

    private static String filterRule(CompoundTag bank, int slot) {
        ListTag rules = bank.getList("Rules", Tag.TAG_STRING);
        return slot < rules.size() ? rules.getString(slot) : "";
    }

    private void renderFilterIcons(GuiGraphics g, int mx, int my) {
        for (boolean importing : new boolean[]{true, false}) {
            CompoundTag bank = menu.state().getCompound(bankKey(importing));
            int base = filterX(importing);
            for (int slot = 0; slot < menu.filterCapacity(); slot++) {
                String rule = filterRule(bank, slot);
                int x = leftPos + base + (slot % menu.columns()) * 18;
                int y = topPos + GRID_Y + (slot / menu.columns()) * 18;
                if (rule.startsWith("#")) {
                    g.drawCenteredString(font, "#", x + 9, y + 5, 0x66E7EF);
                } else if (!rule.isEmpty()) {
                    ResourceLocation itemId = ResourceLocation.tryParse(rule);
                    if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId))
                        g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(itemId)), x + 1, y + 1);
                }
                if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
                    g.fill(x + 1, y + 1, x + 17, y + 17, 0x55FFFFFF);
                    Component tooltip = Component.literal(rule.isEmpty() ?
                            "Left-click: set held cursor item/tag; right-click: clear" :
                            rule + " (right-click to clear)");
                    g.renderTooltip(font, tooltip, mx, my);
                }
            }
        }
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0 || button == 1) && menu.state().getBoolean("CanConfigure")) {
            for (boolean importing : new boolean[]{true, false}) {
                int baseX = leftPos + filterX(importing);
                for (int slot = 0; slot < menu.filterCapacity(); slot++) {
                    int x = baseX + (slot % menu.columns()) * 18;
                    int y = topPos + GRID_Y + (slot / menu.columns()) * 18;
                    if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                        if (button == 1)
                            send("rule", bankWire(importing) + "|", slot);
                        else if (!menu.getCarried().isEmpty())
                            send("ghost", bankWire(importing), slot);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public void render(GuiGraphics graphics, int mx, int my, float partial) {
        renderBackground(graphics, mx, my, partial);
        super.render(graphics, mx, my, partial);
        renderFilterIcons(graphics, mx, my);
        renderTooltip(graphics, mx, my);
    }
}
