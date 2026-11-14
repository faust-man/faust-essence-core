package com.faust.core.progression;
/** Experience currently present on the bar; totalExperience can be stale after enchanting. */
public final class ExperienceMath {
 private ExperienceMath(){}
 public static int points(int level,float progress){long n=Math.max(0,Math.min(30000,level));long base=n<=16?n*n+6*n:n<=31?(5*n*n-81*n+720)/2:(9*n*n-325*n+4440)/2;long next=n>=30?112+(n-30)*9:n>=15?37+(n-15)*5:7+n*2;return (int)Math.min(Integer.MAX_VALUE,base+Math.round(Math.max(0,Math.min(1,progress))*next));}
 public static int stolen(int available,int intelligence,int recipientPoints){return (int)Math.min(Math.max(0L,(long)Integer.MAX_VALUE-recipientPoints),Math.max(0L,available)*Math.max(0,Math.min(100,intelligence))/100);}
}
