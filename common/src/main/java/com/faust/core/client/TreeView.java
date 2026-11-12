package com.faust.core.client;

import com.faust.core.progression.SkillTrees;
import java.util.List;

/** Inventory-sized tree viewport, shared by rendering and hit testing. */
public final class TreeView {
    public double zoom,offsetX,offsetY;
    private final double width,height,minX,minY,maxX,maxY;
    public TreeView(List<SkillTrees.Node> nodes,double width,double height){
        this.width=width;this.height=height;
        minX=nodes.stream().mapToDouble(n->n.x()*SkillTrees.PIXELS_PER_UNIT).min().orElse(0)-6;
        minY=nodes.stream().mapToDouble(n->n.y()*SkillTrees.PIXELS_PER_UNIT).min().orElse(0)-6;
        maxX=nodes.stream().mapToDouble(n->n.x()*SkillTrees.PIXELS_PER_UNIT).max().orElse(0)+SkillTrees.NODE_SIZE*SkillTrees.PIXELS_PER_UNIT+6;
        maxY=nodes.stream().mapToDouble(n->n.y()*SkillTrees.PIXELS_PER_UNIT).max().orElse(0)+SkillTrees.NODE_SIZE*SkillTrees.PIXELS_PER_UNIT+6;
        zoom=Math.max(0.25,Math.min(1,Math.min(width/(maxX-minX),height/(maxY-minY))));offsetX=(width-(minX+maxX)*zoom)/2;offsetY=(height-(minY+maxY)*zoom)/2;clamp();
    }
    public void pan(double dx,double dy){offsetX+=dx;offsetY+=dy;clamp();}
    public void zoomAt(double factor,double x,double y){
        double wx=(x-offsetX)/zoom,wy=(y-offsetY)/zoom;zoom=Math.max(0.25,Math.min(3,zoom*factor));offsetX=x-wx*zoom;offsetY=y-wy*zoom;clamp();
    }
    private void clamp(){
        // Keep at least one node-width of the tree reachable, even when it fits.
        offsetX=Math.max(16-maxX*zoom,Math.min(width-16-minX*zoom,offsetX));
        offsetY=Math.max(16-maxY*zoom,Math.min(height-16-minY*zoom,offsetY));
    }
    public double x(SkillTrees.Node node){return offsetX+node.x()*SkillTrees.PIXELS_PER_UNIT*zoom;}
    public double y(SkillTrees.Node node){return offsetY+node.y()*SkillTrees.PIXELS_PER_UNIT*zoom;}
    public boolean hit(SkillTrees.Node node,double x,double y){return x>=x(node)&&y>=y(node)&&x<x(node)+SkillTrees.NODE_SIZE*SkillTrees.PIXELS_PER_UNIT*zoom&&y<y(node)+SkillTrees.NODE_SIZE*SkillTrees.PIXELS_PER_UNIT*zoom;}
}
