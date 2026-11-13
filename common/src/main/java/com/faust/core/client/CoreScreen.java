package com.faust.core.client;

import com.faust.core.progression.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;

/** Six item-icon skill trees clipped to the vanilla inventory panel. */
public final class CoreScreen extends Screen {
    private static final ResourceLocation PANEL=ResourceLocation.fromNamespaceAndPath("faustcore","textures/gui/container/skill_page_1.png");
    private static final int WIDTH=176,HEIGHT=166,VIEW_X=8,VIEW_Y=45;
    private String treeGroup="faustcore";
    private int skillPage;
    private List<String> groups(){return com.faust.core.progression.TreeGroups.groups(CoreClient.balance);}
    private List<Skill> skills(){return com.faust.core.progression.TreeGroups.skills(CoreClient.balance,treeGroup).stream().skip(skillPage*6L).limit(6).toList();}
    private Skill selected;
    private TreeView view;
    private Balance viewed;
    private boolean dragging;
    private boolean expanded;
    private int panelWidth(){return expanded?width:WIDTH;}
    private int panelHeight(){return expanded?height:HEIGHT;}
    private int viewWidth(){return panelWidth()-16;}
    private int viewHeight(){return panelHeight()-70;}
    public CoreScreen(){super(Component.translatable("core.panel"));}
    private int x(){return (width-panelWidth())/2;}
    private int y(){return (height-panelHeight())/2;}
    @Override protected void init(){view=null;dragging=false;}
    private static String number(double value){return java.math.BigDecimal.valueOf(value).setScale(4,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
    private static Component skillName(Skill skill){return Component.translatableWithFallback("core.skill."+skill,skill.name());}
    public static Component abilityName(Balance.Ability ability){return Component.translatableWithFallback("core.ability."+ability.id().split(":")[1],ability.id());}
    private static Balance.Ability ability(Balance b,String id){return b.abilities.stream().filter(a->a.id().equals(id)).findFirst().orElseThrow();}
    public static ItemStack icon(String id){var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));return new ItemStack(item==Items.AIR?Items.BARRIER:item);}
    private void command(String text){if(minecraft!=null&&minecraft.getConnection()!=null)minecraft.getConnection().sendCommand(text.replaceFirst("essence ","essence panel "));}
    private boolean insideView(double mx,double my){return PanelTabs.inside(mx,my,x()+VIEW_X,y()+VIEW_Y,viewWidth(),viewHeight());}
    private List<SkillTrees.Node> nodes(){var b=CoreClient.balance;var nodes=SkillTrees.nodes(b,selected).stream().filter(n->TreeGroups.contains(b,treeGroup,n.ability())).toList();if(view==null||viewed!=b){view=new TreeView(nodes,viewWidth(),viewHeight());viewed=b;}return nodes;}
    private SkillTrees.Node hovered(double mx,double my){if(!insideView(mx,my))return null;var nodes=nodes();for(int i=nodes.size()-1;i>=0;i--)if(view.hit(nodes.get(i),mx-x()-VIEW_X,my-y()-VIEW_Y))return nodes.get(i);return null;}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta){}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        if(CoreClient.balance!=null&&!groups().contains(treeGroup)){treeGroup="faustcore";skillPage=0;selected=null;view=null;}
        renderTransparentBackground(g);int x=x(),y=y();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();if(!expanded)g.blit(PANEL,x,y,0,0,WIDTH,HEIGHT,256,256);else g.fill(x,y,x+panelWidth(),y+panelHeight(),0xFF555555);g.fill(x+3,y+3,x+panelWidth()-3,y+panelHeight()-3,0xFFC6C6C6);
        List<Component> tooltip=null;var p=CoreClient.progress;var b=CoreClient.balance;
        if(p==null||b==null)center(g,Component.translatable("core.syncing"),x+panelWidth()/2,y+70,0x505050);
        else if(selected==null){
            center(g,minecraft.player==null?title:minecraft.player.getName(),x+panelWidth()/2,y+7,0x505050);
            center(g,Component.translatable("core.player_level",p.totalLevel()),x+panelWidth()/2,y+19,0x505050);
            int i=0;for(Skill skill:skills()){
                if(i>=6)break;int cx=x+8+(i%2)*82,cy=y+37+(i/2)*33;i++;
                g.fill(cx,cy,cx+78,cy+29,0xFF222222);g.fill(cx+1,cy+1,cx+77,cy+28,0xFF3C3C3C);
                String item=switch(skill.name()){case "strength"->"iron_sword";case "constitution"->"golden_apple";case "dexterity"->"bow";case "defense"->"shield";case "intelligence"->"enchanted_book";default->"iron_pickaxe";};
                g.renderItem(icon("minecraft:"+item),cx+6,cy+6);
                g.drawString(font,Component.translatable("core.short."+skill),cx+28,cy+5,0xFFFFFF,false);g.drawString(font,p.track(skill).level+" / "+(int)p.cap(skill,b),cx+28,cy+16,0xC6C6C6,false);
                if(PanelTabs.inside(mx,my,cx,cy,78,29))tooltip=List.of(skillName(skill));
            }
            center(g,Component.translatable("core.select_skill"),x+panelWidth()/2,y+148,0x505050);
        }else{
            center(g,skillName(selected),x+panelWidth()/2,y+7,0x505050);
            g.drawString(font,p.track(selected).level+" / "+(int)p.cap(selected,b),x+8,y+22,0x404040,false);
            var growth=Component.literal((int)Math.round(p.multiplier(selected,b)*100)+"%");g.drawString(font,growth,x+panelWidth()-8-font.width(growth),y+22,0x404040,false);
            double fraction=Math.max(0,Math.min(1,p.track(selected).xp/p.needed(selected,b)));g.fill(x+8,y+35,x+panelWidth()-8,y+38,0xFF303030);g.fill(x+8,y+35,x+8+(int)(viewWidth()*fraction),y+38,0xFF80B938);
            if(PanelTabs.inside(mx,my,x+4,y+4,panelWidth()-8,36)&&!PanelTabs.inside(mx,my,x+panelWidth()-19,y+5,14,14))tooltip=List.of(Component.translatable("core.description."+selected,number(b.skills.get("damageMinimumHP"))),Component.translatable("core.skill_progress",p.track(selected).level,number(100*fraction)),Component.translatable("core.tree.controls"));
            drawTree(g,p,b);
            var hovered=hovered(mx,my);if(hovered!=null){var a=ability(b,hovered.ability());boolean unlocked=p.unlocked.contains(a.id());var lines=new ArrayList<Component>();lines.add(abilityName(a));lines.addAll(abilityDescription(a,p,b));
                if(unlocked&&a.toggleable()){lines.add(Component.translatable(p.enabled(a.id())?"core.ability.enabled":"core.ability.disabled"));lines.add(Component.translatable(p.enabled(a.id())?"core.ability.click_disable":"core.ability.click_enable"));}
                else if(!unlocked){
                    if(a.skillRequirements().size()>1)lines.add(Component.translatable("core.tree.multiple"));
                    for(var r:a.skillRequirements())lines.add(Component.translatable("core.tree.level",skillName(r.skill()),r.level()));
                    if(a.actionConditions().size()>1)lines.add(Component.translatable(a.conditionMode().equals("all")?"core.tree.conditions_all":"core.tree.conditions_count",a.requiredConditions(),a.actionConditions().size()));
                    for(var c:a.actionConditions())if(c.counter().equals("skill_levels"))lines.add(Component.translatable("core.counter.skill_levels"));else lines.add(Component.translatable("core.tree.condition",number(p.conditionCount(a,c)),number(c.amount()),Component.translatable("core.counter."+c.counter())));
                    if(!a.all().isEmpty())lines.add(dependencyText(a.all().size()==1?"core.tree.one":"core.tree.all",a.all(),b));
                    if(!a.requiresAny().isEmpty())lines.add(Component.translatable("core.tree.parents_count",a.requiredParents(),dependencyText("core.tree.any",a.requiresAny(),b)));
                    var excluded=b.abilities.stream().filter(other->SkillTrees.exclusive(a,other)).map(Balance.Ability::id).toList();
                    if(!excluded.isEmpty())lines.add(dependencyText("core.tree.excludes",excluded,b));
                    if(p.excluded(a,b))lines.add(Component.translatable("core.tree.excluded"));
                }
                tooltip=lines;
            }
            box(g,x+8,y+panelHeight()-19,43,14,Component.translatable("core.back"),true);
            boolean full=p.cap(selected,b)>=b.progression.get("maximumCap");long cost=p.price(selected,b);
            box(g,x+55,y+panelHeight()-19,113,14,full?Component.translatable("core.cap.maximum"):Component.translatable("core.cap.price",cost),!full&&CoreClient.experience>=cost);
        }
        if(!expanded&&b!=null&&groups().size()>1){int index=0;for(String group:groups()){
            int bx=x+panelWidth()+2,by=y+index++*24;
            g.fill(bx,by,bx+24,by+23,group.equals(treeGroup)?0xFF83C24A:0xFF707070);
            g.renderItem(new ItemStack(Items.BOOK),bx+4,by+3);
            if(PanelTabs.inside(mx,my,bx,by,24,23))tooltip=List.of(Component.translatableWithFallback("core.tree_group."+group,group));
        }}
        if(selected!=null)box(g,x+panelWidth()-19,y+5,14,14,Component.literal(expanded?"-":"+"),true);
        if(!expanded)PanelTabs.draw(g,x,y,true,-1000,-1000);
        if(tooltip!=null)PanelTabs.tooltipWithin(g,tooltip,mx,my,x,y,panelWidth(),panelHeight());
    }
    private Component dependencyText(String key,List<String> ids,Balance b){var names=Component.empty();for(String id:ids){if(!names.getString().isEmpty())names.append(", ");names.append(abilityName(ability(b,id)));}return Component.translatable(key,names);}
    private void drawTree(GuiGraphics g,Progress p,Balance b){
        var nodes=nodes();int vx=x()+VIEW_X,vy=y()+VIEW_Y;
        g.fill(vx-1,vy-1,vx+viewWidth()+1,vy+viewHeight()+1,0xFF606060);g.fill(vx,vy,vx+viewWidth(),vy+viewHeight(),0xFF383B40);
        g.enableScissor(vx,vy,vx+viewWidth(),vy+viewHeight());
        double dotStep=2*SkillTrees.GRID*SkillTrees.PIXELS_PER_UNIT;
        int firstX=(int)Math.floor(-view.offsetX/(dotStep*view.zoom)),lastX=(int)Math.ceil((viewWidth()-view.offsetX)/(dotStep*view.zoom));
        int firstY=(int)Math.floor(-view.offsetY/(dotStep*view.zoom)),lastY=(int)Math.ceil((viewHeight()-view.offsetY)/(dotStep*view.zoom));
        for(int ix=firstX;ix<=lastX;ix++)for(int iy=firstY;iy<=lastY;iy++){
            int dx=vx+(int)Math.round(view.offsetX+ix*dotStep*view.zoom),dy=vy+(int)Math.round(view.offsetY+iy*dotStep*view.zoom);g.fill(dx,dy,dx+1,dy+1,0xFF45484D);
        }
        var byId=new HashMap<String,SkillTrees.Node>();for(var n:nodes)byId.put(n.ability(),n);
        for(var node:nodes)for(String parent:ability(b,node.ability()).dependencies()){
            var previous=byId.get(parent);if(previous==null)continue;
            int x1=vx+(int)Math.round(view.x(previous)+11*view.zoom),y1=vy+(int)Math.round(view.y(previous)+11*view.zoom),x2=vx+(int)Math.round(view.x(node)+11*view.zoom),y2=vy+(int)Math.round(view.y(node)+11*view.zoom);
            line(g,x1,y1,x2,y2,SkillTrees.lineColor(p,parent));
        }
        for(var node:nodes){var a=ability(b,node.ability());boolean learned=p.unlocked.contains(a.id()),excluded=p.excluded(a,b);int border=SkillTrees.nodeColor(p,a,b);
            g.pose().pushPose();g.pose().translate(vx+view.x(node),vy+view.y(node),0);g.pose().scale((float)view.zoom,(float)view.zoom,1);
            g.fill(0,0,22,22,0xFF161616);g.fill(1,1,21,21,border);g.fill(2,2,20,20,0xFF252A31);if(SkillTrees.validPosition(node,nodes))g.renderItem(icon(node.icon()),3,3);
            if(!learned){g.fill(3,20,19,21,0xFF171717);g.fill(3,20,3+(int)(16*p.requirementProgress(a)),21,0xFF83C24A);}
            if(excluded&&!learned){g.pose().pushPose();g.pose().translate(0,0,200);line(g,3,3,18,18,0xFFD75A5A);g.pose().popPose();}
            g.pose().popPose();
        }
        g.disableScissor();
    }
    private static void line(GuiGraphics g,int x1,int y1,int x2,int y2,int color){int steps=Math.max(Math.abs(x2-x1),Math.abs(y2-y1));if(steps==0){g.fill(x1,y1,x1+1,y1+1,color);return;}for(int i=0;i<=steps;i++){int x=(int)Math.round(x1+(double)(x2-x1)*i/steps),y=(int)Math.round(y1+(double)(y2-y1)*i/steps);g.fill(x,y,x+1,y+1,color);}}
    private static ArrayList<Component> abilityDescription(Balance.Ability a,Progress p,Balance b){
        var lines=new ArrayList<Component>();
        if(a.effect().equals("stat_unlock")){
            for(var rule:b.statBonuses)if(a.id().equals(rule.requiresAbility)){
                boolean percent=!rule.operation.equals("add")||rule.attribute.equals("faustcore:breaking");
                String name=rule.attribute.equals("faustcore:stamina")?"core.stat.stamina":rule.attribute.equals("faustcore:breaking")?"core.stat.breaking":"attribute.name."+rule.attribute.split(":")[1];
                lines.add(Component.translatable("core.stat.rate",Component.translatableWithFallback(name,rule.attribute),number(rule.perLevel*(percent?100:1)),percent?"%":"",rule.everyLevels));
                if(rule.maximum<1e9)lines.add(Component.translatable("core.stat.maximum",number(rule.maximum*(percent?100:1)),percent?"%":""));
                if(rule.sprintOnly)lines.add(Component.translatable("core.stat.sprint_only"));
            }
        }else {
            if(a.id().equals("faustcore:metabolism")){
                if(p.naturalRegeneration)lines.add(Component.translatable("core.effect.faustcore:metabolism"));
                lines.add(Component.translatable("core.metabolism.potions"));
            }
            else if(a.effect().equals("second_wind"))lines.add(Component.translatable("core.second_wind.rate",number(b.stamina.get("secondWindMultiplier")*100),number(b.stamina.get("runCost")*b.stamina.get("secondWindMultiplier"))));
            else {
                String key="core.effect."+(a.id().equals("faustcore:bow_efficiency")?"bow_cost_reduction_first":a.effect());
                if(net.minecraft.client.resources.language.I18n.exists(key)){
                    var description=Component.translatable(key,number(a.value()*(Set.of("damage","speed","breaking","shield_durability","shield_recovery","fall_damage_reduction","exhaustion_threshold_reduction").contains(a.effect())?100:1)));
                    if(!description.getString().isBlank())lines.add(description);
                }
            }
        }
        return lines;
    }

    private void center(GuiGraphics g,Component text,int x,int y,int color){g.drawString(font,text,x-font.width(text)/2,y,color,false);}
    private void box(GuiGraphics g,int x,int y,int w,int h,Component text,boolean active){g.fill(x,y,x+w,y+h,0xFF707070);g.fill(x+1,y+1,x+w-1,y+h-1,active?0xFF373737:0xFF555555);String label=text.getString();if(font.width(label)>w-4)label=font.plainSubstrByWidth(label,w-13)+"...";center(g,Component.literal(label),x+w/2,y+3,active?0xFFFFFF:0xAAAAAA);}
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(!expanded&&button==0&&CoreClient.balance!=null&&groups().size()>1){int index=0;for(String group:groups()){
            if(PanelTabs.inside(mx,my,x()+panelWidth()+2,y()+index++*24,24,23)){treeGroup=group;skillPage=0;selected=null;view=null;return true;}
        }}
        if(!expanded&&PanelTabs.click(mx,my,button,x(),y(),true))return true;
        if(button!=0||CoreClient.progress==null||CoreClient.balance==null)return super.mouseClicked(mx,my,button);
        if(selected==null){int i=0;for(Skill skill:skills()){if(i>=6)break;int cx=x()+8+(i%2)*82,cy=y()+37+(i/2)*33;i++;if(PanelTabs.inside(mx,my,cx,cy,78,29)){selected=skill;view=null;return true;}}}
        else{
            if(PanelTabs.inside(mx,my,x()+panelWidth()-19,y()+5,14,14)){expanded=!expanded;view=null;dragging=false;return true;}
            if(PanelTabs.inside(mx,my,x()+8,y()+panelHeight()-19,43,14)){selected=null;expanded=false;view=null;return true;}
            if(PanelTabs.inside(mx,my,x()+55,y()+panelHeight()-19,113,14)){command("essence cap "+selected);return true;}
            var node=hovered(mx,my);if(node!=null){var a=ability(CoreClient.balance,node.ability());if(CoreClient.progress.unlocked.contains(a.id())){if(a.toggleable())command("essence toggle "+a.id());}else if(CoreClient.progress.canUnlock(a,CoreClient.balance))command("essence unlock "+a.id());return true;}
            if(insideView(mx,my)){dragging=true;return true;}
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(dragging&&button==0&&selected!=null&&view!=null){view.pan(dx,dy);return true;}return super.mouseDragged(mx,my,button,dx,dy);}
    @Override public boolean mouseReleased(double mx,double my,int button){if(button==0)dragging=false;return super.mouseReleased(mx,my,button);}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){if(selected==null&&CoreClient.balance!=null){int pages=(com.faust.core.progression.TreeGroups.skills(CoreClient.balance,treeGroup).size()+5)/6;skillPage=Math.max(0,Math.min(pages-1,skillPage+(vertical<0?1:-1)));return true;}if(selected!=null&&CoreClient.balance!=null&&insideView(mx,my)){nodes();view.zoomAt(Math.pow(1.15,vertical),mx-x()-VIEW_X,my-y()-VIEW_Y);return true;}return super.mouseScrolled(mx,my,horizontal,vertical);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(CoreClient.OPEN.matches(key,scan)){onClose();return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public boolean isPauseScreen(){return false;}
}
