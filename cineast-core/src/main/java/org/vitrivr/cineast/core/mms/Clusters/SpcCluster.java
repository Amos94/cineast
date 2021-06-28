package org.vitrivr.cineast.core.mms.Clusters;//package Clusters;
//
//import java.util.ArrayList;
//import java.util.List;
//import org.apache.commons.math3.linear.EigenDecomposition;
//import org.apache.commons.math3.linear.MatrixUtils;
//import org.apache.commons.math3.linear.RealMatrix;
//import org.apache.commons.math3.ml.clustering.CentroidCluster;
//import org.apache.commons.math3.ml.clustering.Clusterable;
//import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
//
//public class SpcCluster {
//    public static class Points implements Clusterable{
//        private String name;
//        private double[] points = new double[1];
//        public Points(String name, double point) {
//            this.points[0] = point;
//            this.name = name;
//        }
//        @Override
//        public double[] getPoint() {
//            return points;
//        }
//
//        public String toString() {
//            return String.valueOf(name + "\t" + String.format("%.3f", this.points[0]));
//        }
//    }
//    public static void main(String[] args){
//        RealMatrix adjMatrix = MatrixUtils.createRealMatrix(new double[][] {
//                {0,1,1,0,0,0,0},
//                {1,0,1,0,0,0,0},
//                {1,1,0,0,1,0,0},
//                {0,0,0,0,1,1,1},
//                {0,0,1,1,0,1,1},
//                {0,0,0,1,1,0,0},
//                {0,0,0,1,1,0,0}});
//        RealMatrix degreeMatrix = MatrixUtils.createRealMatrix(new double[][] {
//                {2,0,0,0,0,0,0},
//                {0,2,0,0,0,0,0},
//                {0,0,3,0,0,0,0},
//                {0,0,0,3,0,0,0},
//                {0,0,0,0,4,0,0},
//                {0,0,0,0,0,2,0},
//                {0,0,0,0,0,0,2}});
//        RealMatrix lapMatrix = degreeMatrix.subtract(adjMatrix);
//        EigenDecomposition eigen = new EigenDecomposition(lapMatrix);
//        // Get the eigen vector fo the 2nd least eigen value
//        double[] clusterTarget = eigen.getV().getColumn(eigen.getV().getColumnDimension()-2);
//        KMeansPlusPlusClusterer clusterer = new KMeansPlusPlusClusterer(5);
//        List<Points> input = new ArrayList();
//        char letter = 'A';
//
//        for (double val : clusterTarget) {
//            input.add(new Points(String.valueOf(letter), val));
//            letter++;
//        }
//
//        // K mean cluster the value inside eigen vector
//        List<CentroidCluster<Points>> result = clusterer.cluster(input);
//        for (int i=0; i < result.size(); i++) {
//            System.out.println("Clusters.Cluster " + i);
//            for (Points pts : result.get(i).getPoints())
//                System.out.println(pts);
//            System.out.println();
//        }
//    }
//}
