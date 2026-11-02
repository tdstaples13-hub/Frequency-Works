package dev.frequencyworks.client;

import dev.frequencyworks.menu.BufferMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Scrollable nine-column frequency storage above the usual player inventory.
 * Tier I–III have no scrolling; IV–V use a mouse wheel and draggable scrollbar.
 */
public final class BufferScreen extends AbstractContainerScreen<BufferMenu> {
    private Button sortButton;
    private boolean draggingScrollbar;
    private static final int STORAGE_Y = 48;
    private static final int SCROLLBAR_X = 177;
    private static final int SCROLLBAR_WIDTH = 8;

    public BufferScreen(BufferMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        imageWidth = menu.supportsScrolling() ? 194 : 176;
        imageHeight = menu.inventoryY() + 84;
        inventoryLabelY = menu.inventoryY() - 11;
        titleLabelY = 8;
    }

    @Override protected void init() {
        super.init();
        sortButton = addRenderableWidget(Button.builder(Component.literal("Sort"), b -> choose(BufferMenu.SORT_BUTTON))
            .bounds(leftPos + imageWidth - 54, topPos + 23, 45, 18).build());
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
        graphics.fill(0, 0, width, height, 0x8A000000);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFCBC6BA);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, 0xFFE4DED0);
        graphics.fill(leftPos + 6, topPos + STORAGE_Y - 3,
            leftPos + 170, topPos + STORAGE_Y + menu.visibleRows() * 18 + 3, 0xFFB3AC9D);
        for (int i = 0; i < menu.visibleInView(); i++) {
            int x = leftPos + 8 + (i % 9) * 18;
            int y = topPos + STORAGE_Y + (i / 9) * 18;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF665E54);
            graphics.fill(x, y, x + 16, y + 16, 0xFF8A8379);
        }
        for (int i = menu.viewSlots(); i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            int x = leftPos + slot.x, y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF6B645B);
            graphics.fill(x, y, x + 16, y + 16, 0xFF918B80);
        }
        if (menu.canScroll()) {
            int x = leftPos + SCROLLBAR_X;
            graphics.fill(x, barTop(), x + SCROLLBAR_WIDTH, barTop() + barHeight(), 0xFF746F64);
            graphics.fill(x + 1, thumbTop(), x + SCROLLBAR_WIDTH - 1, thumbTop() + thumbHeight(), 0xFFDDD7C9);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 8, 0x3F3F3F, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
        String sortText = switch (menu.sortMode()) {
            case QUANTITY_ASC -> "Quantity: low to high";
            case QUANTITY_DESC -> "Quantity: high to low";
            case ALPHABETICAL -> "Alphabetical";
            case MOD_ALPHABETICAL -> "By mod";
        };
        graphics.drawString(font, sortText, 8, 28, 0x404040, false);
        for (int i = 0; i < menu.visibleInView(); i++) {
            long amount = menu.count(i);
            if (amount <= 0) continue;
            String txt = compact(amount);
            int x = 8 + (i % 9) * 18;
            int y = STORAGE_Y + (i / 9) * 18;
            graphics.drawString(font, txt, x + 17 - font.width(txt), y + 9, 0xFFFFFF, true);
        }
    }

    private static String compact(long amount) {
        if (amount < 1_000) return String.valueOf(amount);
        if (amount < 1_000_000) return amount / 1_000 + "K";
        if (amount < 1_000_000_000) return amount / 1_000_000 + "M";
        return amount / 1_000_000_000 + "B";
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
