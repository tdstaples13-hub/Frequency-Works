package dev.frequencyworks.client;

import dev.frequencyworks.menu.BufferMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Scrollable nine-column frequency storage above the usual player inventory.
 * Tier I–III have no scrolling; IV–V use a mouse wheel and draggable scrollbar.
 */
public final class BufferScreen extends AbstractContainerScreen<BufferMenu> {
    private Button sortButton;
    private boolean draggingScrollbar;
    // Use the exact vanilla 9-column chest panel, not a homemade inventory skin.
    private static final ResourceLocation CHEST_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int STORAGE_Y = 18;
    private static final int SCROLLBAR_X = 181;
    private static final int SCROLLBAR_WIDTH = 8;

    public BufferScreen(BufferMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        // Vanilla ChestMenu geometry: variable chest rows followed by normal
        // inventory and hotbar. Extra 18px at right holds sorting/scrolling only.
        imageWidth = 194;
        imageHeight = 114 + menu.visibleRows() * 18;
        inventoryLabelY = menu.visibleRows() * 18 + 20;
        titleLabelY = 6;
    }

    @Override protected void init() {
        super.init();
        sortButton = addRenderableWidget(Button.builder(Component.literal("S"), b -> choose(BufferMenu.SORT_BUTTON))
            .bounds(leftPos + 178, topPos + 5, 14, 16).build());
    }

    private void choose(int choice) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, choice);
    }

    private int barTop() { return topPos + STORAGE_Y; }
    private int barHeight() { return menu.visibleRows() * 18; }
    private int thumbHeight() {
        return Math.max(12, Math.round(barHeight() * (float)menu.viewSlots() / Math.max(menu.viewSlots(), menu.capacity())));
    }
    private int thumbTop() {
        int travel = barHeight() - thumbHeight();
        return barTop() + (menu.maxScrollRow() == 0 ? 0 : Math.round(travel * (float)menu.scrollRow() / menu.maxScrollRow()));
    }
    private boolean overScrollbar(double x, double y) {
        return menu.canScroll() && x >= leftPos + SCROLLBAR_X && x < leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH
            && y >= barTop() && y < barTop() + barHeight();
    }
    private void scrollToY(double mouseY) {
        int travel = barHeight() - thumbHeight();
        int target = travel == 0 ? 0 : Math.round((float)(mouseY - barTop() - thumbHeight() / 2.0) / travel * menu.maxScrollRow());
        target = Math.max(0, Math.min(menu.maxScrollRow(), target));
        if (target != menu.scrollRow()) choose(BufferMenu.SCROLL_TO_ROW_BASE + target);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (menu.canScroll() && scrollY != 0 && mouseX >= leftPos + 6 && mouseX < leftPos + imageWidth - 5
                && mouseY >= topPos + STORAGE_Y - 2 && mouseY < topPos + STORAGE_Y + barHeight() + 2) {
            int steps = Math.max(1, (int)Math.ceil(Math.abs(scrollY)));
            int next = Math.max(0, Math.min(menu.maxScrollRow(), menu.scrollRow() + (scrollY < 0 ? steps : -steps)));
            if (next != menu.scrollRow()) choose(BufferMenu.SCROLL_TO_ROW_BASE + next);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            scrollToY(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingScrollbar) {
            scrollToY(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        // Standard Minecraft generic chest texture; rows resize exactly like a
        // vanilla chest, with the unmodified player inventory panel beneath it.
        int topHeight = menu.visibleRows() * 18 + 17;
        graphics.blit(CHEST_TEXTURE, leftPos, topPos, 0, 0, 176, topHeight);
        graphics.blit(CHEST_TEXTURE, leftPos, topPos + topHeight, 0, 126, 176, 96);

        // Narrow controls strip *outside* Minecraft's normal inventory panel.
        int x = leftPos + 176;
        graphics.fill(x, topPos, x + 18, topPos + imageHeight, 0xFF8B8B8B);
        graphics.fill(x + 1, topPos + 1, x + 17, topPos + imageHeight - 1, 0xFFC6C6C6);
        if (menu.canScroll()) {
            int trackX = leftPos + SCROLLBAR_X;
            graphics.fill(trackX, barTop(), trackX + SCROLLBAR_WIDTH,
                barTop() + barHeight(), 0xFF555555);
            graphics.fill(trackX + 1, thumbTop(), trackX + SCROLLBAR_WIDTH - 1,
                thumbTop() + thumbHeight(), 0xFFE9E9E9);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Keep vanilla title and player-inventory labels at their normal depth.
        super.renderLabels(graphics, mouseX, mouseY);
    }

    /** Draw the *real* long item counts after Minecraft draws slot item icons.
     * Rendering these in renderLabels() puts the quantities behind the icons.
     * Tooltip rendering still runs afterwards, so hovered-item information wins.
     */
    private void renderStorageCounts(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 250.0F);
        try {
            for (int i = 0; i < menu.visibleInView(); i++) {
                long amount = menu.count(i);
                if (amount <= 0) continue;
                String txt = compact(amount);
                int x = leftPos + 8 + (i % 9) * 18;
                int y = topPos + STORAGE_Y + (i / 9) * 18;
                graphics.drawString(font, txt, x + 17 - font.width(txt), y + 9, 0xFFFFFF, true);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private static String compact(long amount) {
        if (amount < 1_000) return String.valueOf(amount);
        if (amount < 1_000_000) return amount / 1_000 + "K";
        if (amount < 1_000_000_000) return amount / 1_000_000 + "M";
        return amount / 1_000_000_000 + "B";
    }

    private String sortDescription() {
        return switch (menu.sortMode()) {
            case QUANTITY_ASC -> "Quantity: low to high";
            case QUANTITY_DESC -> "Quantity: high to low";
            case ALPHABETICAL -> "Alphabetical";
            case MOD_ALPHABETICAL -> "By mod";
        };
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics, mouseX, mouseY, partial);
        sortButton.setTooltip(Tooltip.create(Component.literal("Sorting: " + sortDescription() + " (click to cycle)")));
        super.render(graphics, mouseX, mouseY, partial);
        renderStorageCounts(graphics); // above item icons, below item tooltips
        renderTooltip(graphics, mouseX, mouseY);
    }
}
