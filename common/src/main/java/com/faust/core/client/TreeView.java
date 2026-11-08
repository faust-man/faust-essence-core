package com.faust.core.client;

import com.faust.core.progression.SkillTrees;
import java.util.List;

/** Inventory-sized tree viewport, shared by rendering and hit testing. */
public final class TreeView {
    public double zoom,offsetX,offsetY;
    private final double width,height,minX,minY,maxX,maxY;
    public TreeView(List<SkillTrees.Node> nodes,double width,double height){
        this.width=width;this.height=height;
        minX=nodes.stream().mapToDouble(SkillTrees.Node::x).min().orElse(0)-6;
        minY=nodes.stream().mapToDouble(SkillTrees.Node::y).min().orElse(0)-6;
        maxX=nodes.stream().mapToDouble(SkillTrees.Node::x).max().orElse(0)+SkillTrees.NODE_SIZE+6;
        maxY=nodes.stream().mapToDouble(SkillTrees.Node::y).max().orElse(0)+SkillTrees.NODE_SIZE+6;
        zoom=Math.max(0.25,Math.min(1,Math.min(width/(maxX-minX),height/(maxY-minY))));clamp();
    }
    public void pan(double dx,double dy){offsetX+=dx;offsetY+=dy;clamp();}
    public void zoomAt(double factor,double x,double y){
        double wx=(x-offsetX)/zoom,wy=(y-offsetY)/zoom;zoom=Math.max(0.25,Math.min(3,zoom*factor));offsetX=x-wx*zoom;offsetY=y-wy*zoom;clamp();
    }
    private void clamp(){
        offsetX=(maxX-minX)*zoom<=width?(width-(minX+maxX)*zoom)/2:Math.max(width-maxX*zoom,Math.min(-minX*zoom,offsetX));
        offsetY=(maxY-minY)*zoom<=height?(height-(minY+maxY)*zoom)/2:Math.max(height-maxY*zoom,Math.min(-minY*zoom,offsetY));
    }
    public double x(SkillTrees.Node node){return offsetX+node.x()*zoom;}
    public double y(SkillTrees.Node node){return offsetY+node.y()*zoom;}
    public boolean hit(SkillTrees.Node node,double x,double y){return x>=x(node)&&y>=y(node)&&x<x(node)+SkillTrees.NODE_SIZE*zoom&&y<y(node)+SkillTrees.NODE_SIZE*zoom;}
}
