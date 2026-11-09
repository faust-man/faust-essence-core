package com.faust.core.progression;

import com.faust.stamina.client.StaminaSprites;
import javax.imageio.ImageIO;

public final class StaminaSpritesTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    public static void run() throws Exception {
        check(StaminaSprites.color(0, false) == 0xFFFFD000, "normal stamina is yellow");
        check(StaminaSprites.color(2, false) == 0xFF000000, "fatigue is black");
        check(StaminaSprites.color(2, true) == 0xFF000000, "exhaustion never recolors fatigue");
        check(StaminaSprites.color(0, true) == 0xFFFF4040, "exhausted fill stays red");
        check(StaminaSprites.color(1, true) == 0xFFFF4040, "exhausted empty outline stays red");
        check(StaminaSprites.color(1, false) != StaminaSprites.FATIGUE_COLOR, "spent differs from fatigue");
        for (int available = 0; available <= 25; available++) {
            for (int filled = 0; filled <= available; filled++) {
                for (int unit = 0; unit < 25; unit++) {
                    int expected = unit >= available ? 2 : unit < filled ? 0 : 1;
                    check(StaminaSprites.state(unit, available, filled) == expected,
                            "odd halves, empty stamina, fatigue boundary and additional rows");
                }
            }
        }
        try (var input = StaminaSpritesTest.class.getResourceAsStream("/assets/faustcore/" + StaminaSprites.texture(0)); var emptyInput = StaminaSpritesTest.class.getResourceAsStream("/assets/faustcore/" + StaminaSprites.texture(1))) {
            check(input != null && emptyInput != null, "both lightning sprites are packaged");
            var image = ImageIO.read(input);
            var empty = ImageIO.read(emptyInput);
            check(image.getWidth() == 16 && image.getHeight() == 16, "filled sprite is exactly 16x16");
            check(empty.getWidth() == 16 && empty.getHeight() == 16, "empty sprite is exactly 16x16");
            int fullCount = 0, outlineCount = 0, leftCount = 0, rightCount = 0;
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                int full = image.getRGB(x, y), outline = empty.getRGB(x, y);
                check(full == 0 || full == 0xFFFFFFFF, "binary white mask permits any tint");
                check(outline == 0 || outline == 0xFFFFFFFF, "binary outline mask");
                check(outline == 0 || full != 0, "outline stays inside filled silhouette");
                if (full != 0) { fullCount++; if (x < 8) leftCount++; else rightCount++; }
                if (outline != 0) outlineCount++;
            }
            check(fullCount > outlineCount && outlineCount > 0, "empty sprite has transparent interior");
            check(leftCount > 0 && rightCount > 0, "both half-units are visible");
            check(image.getRGB(0, 0) == 0, "transparent corners");
        }
        for (int state = 0; state < 3; state++) {
            int left = StaminaSprites.textureX(state, 0), right = StaminaSprites.textureX(state, 1);
            check(right == left + 8 && right + 8 == 16, "halves cover exactly one 16px sprite");
            check(left == 0, "all states start at the left edge of their own sprite");
            check(StaminaSprites.texture(2).equals(StaminaSprites.texture(0)), "fatigue and stamina use the same silhouette");
        }
        System.out.println("Stamina lightning checks passed: " + checks);
    }
}
