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
}
