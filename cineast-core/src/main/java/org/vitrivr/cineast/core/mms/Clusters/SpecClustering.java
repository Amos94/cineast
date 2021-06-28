package org.vitrivr.cineast.core.mms.Clusters;//package Clusters;
//
//import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
//import org.bytedeco.javacpp.indexer.IntRawIndexer;
//import org.bytedeco.javacpp.indexer.UByteRawIndexer;
//import smile.clustering.*;
//import org.bytedeco.javacpp.opencv_core.Mat;
//import smile.math.matrix.Matrix;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//
//import static org.opencv.core.CvType.CV_8UC;
//
//
//public class SpecClustering {
//
//    private static Mat superpixels;
//    public SpecClustering(Mat superpixels){
//        this.superpixels = superpixels;
//    }
//
//    public SpectralClustering Clustering(Matrix m){
//        var ret =  SpectralClustering.fit(m,30);
//
//        for(int i=0; i<ret.size.length; ++i){
//            System.out.print(ret.y + " ");
//        }
//
//        return ret;
//    }
//
//    public Matrix MatToMatrix(){
//        Matrix ret = new Matrix(superpixels.rows(), superpixels.cols());
//        var w = superpixels.createIndexer();
//        var sizes = w.sizes();
//
//        for(int i=0; i<superpixels.rows(); ++i){
//            for(int j=0; j<superpixels.cols(); ++j){
//                var x = w.getDouble(i,j);
//                ret.add(i,j, x);
//
//                System.out.print(ret.get(i,j) + " ");
//            }
//            System.out.println();
//        }
//        Clustering(ret);
//        return ret;
//    }
//
//    public double[][] MatTo2DArray() throws IOException {
//        FileWriter f = new FileWriter("C:\\Dev\\sppseg\\src\\main\\java\\labels.txt");
//        double[][]  ret = new double[superpixels.rows()][superpixels.cols()];
//        var w = superpixels.createIndexer();
//        var sizes = w.sizes();
//
//        for(int i=0; i<superpixels.rows(); ++i){
//            for(int j=0; j<superpixels.cols(); ++j){
//                var x = w.getDouble(i,j);
//                ret[i][j] = x;
//                if(i == superpixels.rows()-1)
//                    f.write( "" + (int)x);
//                else
//                    f.write( "" + (int)x + ",");
//            }
//            f.write("\n");
//        }
//
//        var x= SpectralClustering.fit(ret, 20, 10);
//
//        for(int i=0; i< x.y.length; ++i) {
//            System.out.print(x.y + " ");
//        }
//
//        return ret;
//    }
//
//}
