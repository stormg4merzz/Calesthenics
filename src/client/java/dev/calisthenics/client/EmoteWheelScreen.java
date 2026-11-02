package dev.calisthenics.client;

import dev.calisthenics.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 3x3 emote picker. The 8 outer cells are emotes, the center cell shows the player's own head
 * (click it to stop the current emote). Number keys 1-8 pick an emote too. Press O again to close.
 */
public class EmoteWheelScreen extends Screen {

    private static final int CELL = 62;
    private static final int GAP = 4;

    // ARGB (since 1.21.6 the alpha byte matters, 0x00 would be invisible)
    private static final int COLOR_CELL = 0xB0101018;
    private static final int COLOR_CELL_HOVER = 0xD02A3A5A;
    private static final int COLOR_BORDER = 0xFF5A6A8A;
    private static final int COLOR_BORDER_HOVER = 0xFFFFFFFF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFFAAAAAA;

    private int gridX;
    private int gridY;

    public EmoteWheelScreen() {
        super(Component.translatable("screen.calisthenics.title"));
    }

    @Override
    protected void init() {
        int total = 3 * CELL + 2 * GAP;
        gridX = (width - total) / 2;
        gridY = (height - total) / 2;
    }

    private int cellX(int slot) {
        return gridX + (slot % 3) * (CELL + GAP);
    }

    private int cellY(int slot) {
        return gridY + (slot / 3) * (CELL + GAP);
    }

    /** Grid slot 0..8 (4 = center) -> emote, or null for the center. */
    private static Skill skillAt(int slot) {
        if (slot == 4) {
            return null;
        }
        int index = slot < 4 ? slot : slot - 1;
        return Skill.values()[index];
    }

    private boolean inside(int slot, double mx, double my) {
        int x = cellX(slot);
        int y = cellY(slot);
        return mx >= x && mx < x + CELL && my >= y && my < y + CELL;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        graphics.drawCenteredString(font, title, width / 2, gridY - 18, WHITE);

        for (int slot = 0; slot < 9; slot++) {
            int x = cellX(slot);
            int y = cellY(slot);
            boolean hovered = inside(slot, mouseX, mouseY);

            graphics.fill(x, y, x + CELL, y + CELL, hovered ? COLOR_CELL_HOVER : COLOR_CELL);
            graphics.renderOutline(x, y, CELL, CELL, hovered ? COLOR_BORDER_HOVER : COLOR_BORDER);

            Skill skill = skillAt(slot);
            if (skill == null) {
                drawHead(graphics, x, y);
            } else {
                drawEmoteLabel(graphics, skill, slot, x, y);
            }
        }

        graphics.drawCenteredString(font,
                Component.translatable("screen.calisthenics.hint").withStyle(ChatFormatting.GRAY),
                width / 2, gridY + 3 * CELL + 2 * GAP + 8, WHITE);
    }

    private void drawHead(GuiGraphics graphics, int x, int y) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        int size = CELL - 22;
        PlayerFaceRenderer.draw(graphics, player.getSkin(), x + (CELL - size) / 2, y + 6, size);
        graphics.drawCenteredString(font,
                Component.translatable("skill.calisthenics.stop").withStyle(ChatFormatting.GRAY),
                x + CELL / 2, y + CELL - 13, WHITE);
    }

    private void drawEmoteLabel(GuiGraphics graphics, Skill skill, int slot, int x, int y) {
        int number = (slot < 4 ? slot : slot - 1) + 1;
        graphics.drawString(font, Component.literal(String.valueOf(number)), x + 4, y + 4, GREY);

        List<FormattedCharSequence> lines = font.split(Component.translatable(skill.translationKey()), CELL - 8);
        int textHeight = lines.size() * 10;
        int ty = y + (CELL - textHeight) / 2;
        for (FormattedCharSequence line : lines) {
            graphics.drawCenteredString(font, line, x + CELL / 2, ty, WHITE);
            ty += 10;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int slot = 0; slot < 9; slot++) {
                if (inside(slot, event.x(), event.y())) {
                    Skill skill = skillAt(slot);
                    CalisthenicsClient.requestSkill(skill == null ? "" : skill.id);
                    onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Same key that opened the screen closes it again.
        if (CalisthenicsClient.menuKey.matches(event)) {
            onClose();
            return true;
        }
        // 1-8 pick an emote directly.
        int key = event.key();
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_8) {
            Skill skill = Skill.values()[key - GLFW.GLFW_KEY_1];
            CalisthenicsClient.requestSkill(skill.id);
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
