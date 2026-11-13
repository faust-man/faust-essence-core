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
            check(image.getWidth() == 8 && image.getHeight() == 8, "filled sprite is exactly 8x8");
            check(empty.getWidth() == 8 && empty.getHeight() == 8, "empty sprite is exactly 8x8");
            int fullCount = 0, outlineCount = 0, leftCount = 0, rightCount = 0;
            for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
                int full = image.getRGB(x, y), outline = empty.getRGB(x, y);
                check(full == 0 || full == 0xFFFFFFFF, "binary white mask permits any tint");
                check(outline == 0 || outline == 0xFFFFFFFF, "binary outline mask");
                check(outline == 0 || full != 0, "outline stays inside filled silhouette");
                if (full != 0) { fullCount++; if (x < 4) leftCount++; else rightCount++; }
                if (outline != 0) outlineCount++;
            }
            check(fullCount > outlineCount && outlineCount > 0, "empty sprite has transparent interior");
            check(leftCount > 0 && rightCount > 0, "both half-units are visible");
            check(image.getRGB(0, 0) == 0, "transparent corners");
        }
        for (int state = 0; state < 3; state++) {
            int left = StaminaSprites.textureX(state, 0), right = StaminaSprites.textureX(state, 1);
            check(right == left + 4 && right + 4 == 8, "halves cover exactly one 8px sprite");
            check(left == 0, "all states start at the left edge of their own sprite");
            check(StaminaSprites.texture(2).equals(StaminaSprites.texture(0)), "fatigue and stamina use the same silhouette");
        }
        for(int filled=0;filled<=60;filled++) {
            int layer=com.faust.stamina.client.StaminaLayers.layer(filled);
            check(layer==Math.max(0,(filled-1)/20), "layer changes only above exact multiples of twenty");
            for(int slot=0;slot<20;slot++) {
                int state=com.faust.stamina.client.StaminaLayers.state(slot,60,55,filled);
                check((state==2)==(layer==2&&slot>=15), "fatigue only on the capacity layer currently shown");
            }
        }
        check(com.faust.stamina.client.StaminaLayers.state(5,30,25,20)!=2,"30/5 fatigue hidden at twenty");
        check(com.faust.stamina.client.StaminaLayers.state(5,30,25,21)==2,"30/5 fatigue shown at twenty-one");
        check(com.faust.stamina.client.StaminaLayers.state(0,30,25,21)==0,"second layer begins filled");
        for(int slot=5;slot<20;slot++)check(com.faust.stamina.client.StaminaLayers.state(slot,30,25,21)==2,"entire right tail is fatigue, including past partial capacity");
        check(com.faust.stamina.client.StaminaLayers.state(15,30,30,21)==0,"without fatigue lower layer remains visible");
        for(int layer=1;layer<5;layer++) {
            check(StaminaSprites.layerColor(layer,false,false)==StaminaSprites.STAMINA_COLOR,"spent upper layer reveals yellow base");
            check(StaminaSprites.layerColor(layer,true,false)==0xFFE9801C,"upper fill is orange");
        }
        int border=0,shine=0,body=0;
        for(int y=0;y<8;y++)for(int x=0;x<8;x++) {
            int pixel=StaminaSprites.pixel(x,y,0,StaminaSprites.STAMINA_COLOR);
            if(pixel==0xFF181818)border++;
            if(pixel==0xFFFFF3CF)shine++;
            if(pixel==StaminaSprites.STAMINA_COLOR)body++;
            int fatigue=StaminaSprites.pixel(x,y,2,StaminaSprites.STAMINA_COLOR);
            check(fatigue==0||fatigue==StaminaSprites.FATIGUE_COLOR,"fatigue has no colored outline or shine");
        }
        check(border>0&&shine>0&&body>0,"native sprite contains outline, shine and tinted body");
        System.out.println("Stamina lightning checks passed: " + checks);
    }
}
