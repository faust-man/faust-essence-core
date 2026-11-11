package com.faust.core.client;

import com.faust.core.progression.Balance;
import com.faust.core.runtime.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** All server balance sections, including nested actions and ability trees. */
public final class CoreConfigScreen extends Screen {
    private final Screen parent;
    private final java.util.List<String> visibleGroups;
    private final String namespace;
    private String section="progression";
    private Balance viewed;
    private MultiLineEditBox editor;
    private Button save;
    private int sectionOffset;
    private java.util.List<String> sectionNames;
    private Component message=Component.empty();
    private final Map<String,String> drafts=new HashMap<>();
    public CoreConfigScreen(Screen parent){this(parent,Balance.groups().stream().filter(g->!List.of("stamina","fatigue","armor","sweetFoods","client").contains(g)).toList());}
    public CoreConfigScreen(Screen parent,java.util.List<String> groups){this(parent,groups,"");}
    public CoreConfigScreen(Screen parent,java.util.List<String> groups,String namespace){super(Component.translatable("core.config.title"));this.parent=parent;this.namespace=namespace;this.visibleGroups=List.copyOf(groups);this.section=groups.getFirst();}
    private boolean connected(){return minecraft!=null&&minecraft.player!=null&&minecraft.getConnection()!=null;}
    @Override protected void init() {
        try{viewed=connected()&&CoreClient.balance!=null?CoreClient.balance:Balance.load(CoreClient.configDirectory);}
        catch(Exception e){viewed=new Balance();message=Component.translatable("core.config.invalid");}
        sectionNames=visibleGroups;int rows=Math.max(1,(height-90)/18);sectionOffset=Math.max(0,Math.min(sectionOffset,Math.max(0,sectionNames.size()-rows)));
        for(int i=sectionOffset;i<Math.min(sectionNames.size(),sectionOffset+rows);i++){
            String group=sectionNames.get(i);addRenderableWidget(Button.builder(Component.translatableWithFallback("core.config."+group,group),b->{drafts.put(section,editor.getValue());section=group;editor.setValue(drafts.getOrDefault(group,sectionText(group)));}).bounds(8,30+(i-sectionOffset)*18,100,17).build());
        }
        editor=addRenderableWidget(new MultiLineEditBox(font,114,30,width-124,Math.max(40,height-90),Component.empty(),Component.translatable("core.config.editor")));
        editor.setCharacterLimit(65536);editor.setValue(drafts.getOrDefault(section,sectionText(section)));
        save=addRenderableWidget(Button.builder(Component.translatable("core.config.save"),b->save()).bounds(114,height-50,100,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(width-90,height-50,80,20).build());
        if(connected()){save.active=false;CoreClient.sendConfig.accept(new ConfigEdit("",""));}
    }
    private String sectionText(String name){
        if(name.equals("abilities")&&!namespace.isEmpty())return Balance.JSON.toJson(viewed.abilities.stream().filter(a->a.id().startsWith(namespace+":")).toList());
        return viewed.group(name);
    }
    private void save() {
        String text=editor.getValue();
        if(section.equals("abilities")&&!namespace.isEmpty()){
            try{var edited=java.util.Arrays.asList(Balance.JSON.fromJson(text,Balance.Ability[].class));if(edited.stream().anyMatch(a->!a.id().startsWith(namespace+":")))throw new IllegalArgumentException();var merged=new ArrayList<>(viewed.abilities.stream().filter(a->!a.id().startsWith(namespace+":")).toList());merged.addAll(edited);text=Balance.JSON.toJson(merged);}
            catch(Exception e){message=Component.translatable("core.config.invalid");return;}
        }
        try {
            viewed.edited(section,text);
            if(connected()){save.active=false;message=Component.translatable("core.config.saving");CoreClient.sendConfig.accept(new ConfigEdit(section,text));}
            else {Balance.saveGroup(CoreClient.configDirectory,section,text);viewed=Balance.load(CoreClient.configDirectory);CoreRuntime.reload();drafts.put(section,editor.getValue());message=Component.translatable("core.config.saved");}
        }catch(Exception e){message=Component.translatable("core.config.invalid");}
    }
    public void result(ConfigResult result) {
        save.active=result.allowed();message=Component.translatable(result.message());
        if(result.success()&&CoreClient.balance!=null)viewed=CoreClient.balance;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        renderBackground(g,mx,my,delta);super.render(g,mx,my,delta);
        g.drawString(font,title,8,10,0xFFFFFF,false);
        g.drawString(font,Component.translatable("core.config.section",section),114,20,0xFFFFFF,false);
        g.drawWordWrap(font,message,114,height-25,width-124,0xE4CF98);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){if(mx<110&&vertical!=0){drafts.put(section,editor.getValue());sectionOffset+=vertical>0?-1:1;rebuildWidgets();return true;}return super.mouseScrolled(mx,my,horizontal,vertical);}
    @Override public void resize(net.minecraft.client.Minecraft mc,int w,int h){if(editor!=null)drafts.put(section,editor.getValue());super.resize(mc,w,h);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
