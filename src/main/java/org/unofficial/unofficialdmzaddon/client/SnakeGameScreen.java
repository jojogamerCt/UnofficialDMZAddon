package org.unofficial.unofficialdmzaddon.client;

import com.dragonminez.client.gui.character.minigames.BaseMinigameScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.lwjgl.glfw.GLFW;
import org.unofficial.unofficialdmzaddon.UnofficialDMZConfig;

import java.util.ArrayDeque;
import java.util.Deque;

public final class SnakeGameScreen extends BaseMinigameScreen {
    private static final int COLUMNS = 22;
    private static final int ROWS = 16;

    private final Deque<Cell> snake = new ArrayDeque<>();
    private final RandomSource random = RandomSource.create();
    private Direction direction = Direction.RIGHT;
    private Direction queuedDirection = Direction.RIGHT;
    private Cell apple;
    private int moveTimer;
    private boolean directionQueued;

    public SnakeGameScreen() {
        super("snake", "gui.dragonminez.minigame.snake");
    }

    @Override
    protected void onStart() {
        snake.clear();
        int x = COLUMNS / 2;
        int y = ROWS / 2;
        snake.addLast(new Cell(x, y));
        snake.addLast(new Cell(x - 1, y));
        snake.addLast(new Cell(x - 2, y));
        direction = Direction.RIGHT;
        queuedDirection = direction;
        moveTimer = 0;
        directionQueued = false;
        placeApple();
    }

    @Override
    protected void tickGame() {
        if (++moveTimer < movementInterval()) return;
        moveTimer = 0;
        direction = queuedDirection;
        directionQueued = false;

        Cell head = snake.peekFirst();
        if (head == null) return;
        Cell next = new Cell(head.x + direction.dx, head.y + direction.dy);
        if (next.x < 0 || next.x >= COLUMNS || next.y < 0 || next.y >= ROWS) {
            playMiss();
            endGame();
            return;
        }

        boolean ateApple = next.equals(apple);
        if (!ateApple) snake.removeLast();
        if (snake.contains(next)) {
            playMiss();
            endGame();
            return;
        }

        snake.addFirst(next);
        if (ateApple) {
            playUi(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.35F, 0.5F);
            levelCleared();
            placeApple();
        }
    }

    private int movementInterval() {
        int base = UnofficialDMZConfig.SNAKE_BASE_MOVE_TICKS.get();
        int minimum = Math.min(base, UnofficialDMZConfig.SNAKE_MIN_MOVE_TICKS.get());
        int step = Math.max(1, UnofficialDMZConfig.SNAKE_APPLES_PER_SPEEDUP.get());
        return Math.max(minimum, base - levelsCleared / step);
    }

    private void placeApple() {
        if (snake.size() >= COLUMNS * ROWS) {
            endGame();
            return;
        }
        do {
            apple = new Cell(random.nextInt(COLUMNS), random.nextInt(ROWS));
        } while (snake.contains(apple));
    }

    @Override
    protected void renderGame(GuiGraphics graphics) {
        int cellSize = Math.max(7, Math.min((width - 80) / COLUMNS, (height - 100) / ROWS));
        int gridWidth = COLUMNS * cellSize;
        int gridHeight = ROWS * cellSize;
        int left = (width - gridWidth) / 2;
        int top = (height - gridHeight) / 2 + 12;

        graphics.fill(left - 4, top - 4, left + gridWidth + 4, top + gridHeight + 4, 0xFF061B0D);
        graphics.fill(left - 2, top - 2, left + gridWidth + 2, top + gridHeight + 2, 0xFF2C7A3F);
        graphics.fill(left, top, left + gridWidth, top + gridHeight, 0xFF07140B);
        for (int x = 1; x < COLUMNS; x++) graphics.fill(left + x * cellSize, top, left + x * cellSize + 1, top + gridHeight, 0x2219A74A);
        for (int y = 1; y < ROWS; y++) graphics.fill(left, top + y * cellSize, left + gridWidth, top + y * cellSize + 1, 0x2219A74A);

        if (apple != null) {
            fillCell(graphics, left, top, cellSize, apple, 0xFFDF3131, 2);
            int stemX = left + apple.x * cellSize + cellSize / 2;
            int stemY = top + apple.y * cellSize + 1;
            graphics.fill(stemX, stemY, stemX + 1, stemY + 3, 0xFF80D95B);
        }

        boolean head = true;
        for (Cell part : snake) {
            fillCell(graphics, left, top, cellSize, part, head ? 0xFFFFD84D : 0xFF42D96B, 1);
            head = false;
        }
    }

    private void fillCell(GuiGraphics graphics, int left, int top, int size, Cell cell, int color, int inset) {
        int x = left + cell.x * size + inset;
        int y = top + cell.y * size + inset;
        graphics.fill(x, y, left + (cell.x + 1) * size - inset, top + (cell.y + 1) * size - inset, color);
    }

    @Override
    protected boolean onKey(int keyCode) {
        Direction requested = switch (keyCode) {
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> Direction.UP;
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> Direction.DOWN;
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> Direction.LEFT;
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> Direction.RIGHT;
            default -> null;
        };
        if (requested == null) return false;
        if (!directionQueued && requested != direction.opposite()) {
            queuedDirection = requested;
            directionQueued = true;
        }
        return true;
    }

    private record Cell(int x, int y) {}

    private enum Direction {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

        private final int dx;
        private final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        private Direction opposite() {
            return switch (this) {
                case UP -> DOWN;
                case DOWN -> UP;
                case LEFT -> RIGHT;
                case RIGHT -> LEFT;
            };
        }
    }
}
