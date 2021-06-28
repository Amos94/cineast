package org.vitrivr.cineast.core.mms.Helper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Volume {
    private List<Voxel> volume = new ArrayList<Voxel>();
    public Volume(){}
    public Volume(Voxel v) {
        volume.add(v);
    }

    public List<Voxel> getVolume(){
        Comparator<Voxel> voxelComparator
                = Comparator.comparingDouble(Voxel::getT);

        volume.sort(voxelComparator);
        return volume;
    }

    public void addVoxel(Voxel v){
        volume.add(v);
    }

    public boolean removeVoxel(Voxel v){
        return volume.remove(v);
    }

    @Override
    public String toString(){
        StringBuilder ret = new StringBuilder();
        for(Voxel v : volume){
            ret.append(v.toString());
            ret.append("\n");
        }

        return ret.toString();
    }
}
