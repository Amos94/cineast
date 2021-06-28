package org.vitrivr.cineast.core.mms.Algorithms.Segmentation;

public class Merge {

    /**
     *
     * @param h: image height
     * @param w: image width
     * @param: constant - default in paper 100
     * @return spatial weight - SQRT(H*W)/M
     */
    public static double calculateSpatialWheight(int w, int h, int m){
        if(m<=0)
            m = 100;
        double spatialWeight = Math.sqrt(w*h)/m;

        return spatialWeight;
    }

    public static double calculate5DEuclideanDistance(int li, int lj, int ai, int aj, int bi, int bj, int xi, int xj, int yi, int yj, int sw){
        return Math.sqrt(Math.pow((li-lj), 2) + Math.pow((ai-aj), 2) + Math.pow((bi-bj), 2) + Math.pow(((xi-xj)/sw), 2)  + Math.pow(((yi-yj)/sw), 2));
    }

    public static double calculateLabEuclideanDistance(int li, int lj, int ai, int aj, int bi, int bj){
        return Math.sqrt(Math.pow((li-lj), 2) + Math.pow((ai-aj), 2) + Math.pow((bi-bj), 2));
    }

    public static double calculateSigma(double[] distances, int t){
        if(t<=0)
            t=5;

        double distancesSum = 0;
        for(double distance : distances)
            distancesSum += distance;

        return (1/t) * distancesSum;
    }

    public static double calculateSimilarityMatrixElement(double distanceij, double sigmai, double sigmaj){
        double d = -Math.pow(distanceij,2)/(2*sigmai*sigmaj);
        return Math.exp(d);
    }

    public static double calculateDifferenceBetweenTwoAdjacentClusters(double[] labEuclideanDistances, int superpixelPairsCount){
        double distancesSum = 0;
        for(double distance : labEuclideanDistances)
            distancesSum += distance;

        return (1/superpixelPairsCount) * distancesSum;
    }

    public static double calculateStandardDeviation(double numArray[])
    {
        double sum = 0, standardDeviation = 0;
        int length = numArray.length;

        for(double num : numArray) {
            sum += num;
        }

        double mean = sum/length;

        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation/length);
    }

    public static double calculateMean(double[] numArray){
        double sum = 0;
        for(double num:numArray){
            sum += num;
        }

        return sum/numArray.length;
    }

    public static double calculateThreshold(double[] maxEdges){
        double mean = calculateMean(maxEdges);
        double std = calculateStandardDeviation(maxEdges);
        double threshold = mean - std/2;

        if(threshold > 10)
            return threshold;

        return 10;
    }

    public static double calculateRefinedDifferenceBetweenTwoAdjacentClusters(double differenceBetweenTwoAdjacentClusters, int KW, int riSuperpixelsCount, int rjSuperPixelsCount){
        if(KW <= 0)
            KW = 60;
        return differenceBetweenTwoAdjacentClusters - (KW / Math.min(riSuperpixelsCount, rjSuperPixelsCount));
    }
}
