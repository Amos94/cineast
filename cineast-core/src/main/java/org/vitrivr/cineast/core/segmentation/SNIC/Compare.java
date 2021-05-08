package org.vitrivr.cineast.core.segmentation.SNIC;

import java.util.Comparator;

public class Compare implements Comparator<Node>{
    @Override
    public int compare(Node o1, Node o2) {
        if (o1.d < o2.d) {
            return -1;
        }
        if (o1.d > o2.d) {
            return 1;
        }
        return 0;
    }
}
