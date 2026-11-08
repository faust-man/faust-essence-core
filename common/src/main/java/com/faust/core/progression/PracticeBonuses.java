package com.faust.core.progression;
public final class PracticeBonuses {
    private PracticeBonuses() {}
    public static int shelves(Progress p,Balance b){return p.effect("enchanting_power",b)>0?(int)Math.floor(Math.min(20,p.track(Skill.intelligence).level)/5)*15/8:0;}
    public static double duration(Progress p,Balance b){return 1+p.track(Skill.intelligence).level*p.effect("potion_duration",b);}
}
