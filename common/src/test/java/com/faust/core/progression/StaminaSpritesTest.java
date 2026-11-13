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
        check(StaminaSprites.color(2, false) == 0xFF37373B, "fatigue is dark grey");
        check(StaminaSprites.color(2, true) == 0xFF37373B, "exhaustion never recolors fatigue");
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
        String[] textures={"full","empty","fatigue"};
        for(int state=0;state<3;state++) {
            try(var input=StaminaSpritesTest.class.getResourceAsStream("/assets/faustcore/textures/gui/stamina_lightning_"+textures[state]+".png")) {
                check(input!=null,"approved sprite packaged");
                var expected=ImageIO.read(input);
                check(expected.getWidth()==9&&expected.getHeight()==9,"approved sprites are 9x9");
                var actual=new java.awt.image.BufferedImage(9,9,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                for(int y=0;y<9;y++)for(int x=0;x<9;x++)actual.setRGB(x,y,StaminaSprites.pixel(x,y,state,StaminaSprites.color(state,false)));
                var overlay=new java.awt.image.BufferedImage(9,9,java.awt.image.BufferedImage.TYPE_INT_ARGB);
                for(int y=0;y<9;y++)for(int x=0;x<9;x++)overlay.setRGB(x,y,StaminaSprites.overlay(x,y,state));
                var graphics=actual.createGraphics();graphics.drawImage(overlay,0,0,null);graphics.dispose();
                for(int y=0;y<9;y++)for(int x=0;x<9;x++)check(actual.getRGB(x,y)==expected.getRGB(x,y),"render matches approved preview including alpha overlays");
            }
        }
        check(StaminaSprites.textureX(0,1)==4,"split uses four left pixels and five right pixels");
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
        int border=0,shine=0,shadow=0,body=0;
        for(int y=0;y<9;y++)for(int x=0;x<9;x++) {
            int pixel=StaminaSprites.pixel(x,y,0,StaminaSprites.STAMINA_COLOR);
            if(pixel==0xFF211D29)border++;
            int overlay=StaminaSprites.overlay(x,y,0);
            if(overlay==0x60FFFFFF)shine++;
            if(overlay==0x50000000)shadow++;
            if(overlay!=0)check((overlay>>>24)>0&&(overlay>>>24)<255,"shine and shadow are translucent");
            check((overlay&0xFFFFFF)==0||(overlay&0xFFFFFF)==0xFFFFFF,"overlays are neutral without yellow tint");
            if(pixel==StaminaSprites.STAMINA_COLOR)body++;
            int fatigue=StaminaSprites.pixel(x,y,2,StaminaSprites.STAMINA_COLOR);
            check(fatigue==0||fatigue==StaminaSprites.FATIGUE_COLOR||fatigue==0xFF211D29,"fatigue uses grey body and dark outline");
        }
        check(border>0&&shine>0&&shadow>0&&body>0,"native sprite contains outline, shine and tinted body");
        System.out.println("Stamina lightning checks passed: " + checks);
    }
}
