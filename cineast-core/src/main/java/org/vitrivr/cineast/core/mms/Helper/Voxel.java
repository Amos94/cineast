package org.vitrivr.cineast.core.mms.Helper;

import java.util.List;

public class Voxel {
    private int t;
    private List<Point> polygon;

    public Voxel(int t, List<Point> polygon){
        this.t = t;
        this.polygon = polygon;
    }

    public int getT(){
        return t;
    }

    public List<Point> getPolygon(){
        return polygon;
    }

    @Override
    public String toString(){
        StringBuilder ret = new StringBuilder();
        ret.append("T: "+t);
        ret.append("\n");
        for(Point p : polygon) {
            ret.append("x:" + p.x + " y:" + p.y);
            ret.append("\n");
        }
        return ret.toString();
    }
}
