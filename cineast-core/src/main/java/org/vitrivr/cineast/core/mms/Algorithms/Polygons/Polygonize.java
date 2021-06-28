package org.vitrivr.cineast.core.mms.Algorithms.Polygons;

import org.vitrivr.cineast.core.mms.Helper.ConvexHull;
import org.vitrivr.cineast.core.mms.Helper.Point;
import org.opencv.core.Point3;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.ximgproc.SuperpixelSLIC;

import java.util.*;

import static org.opencv.core.Core.*;
import static org.opencv.imgcodecs.Imgcodecs.*;
import static org.opencv.imgproc.Imgproc.COLOR_GRAY2BGR;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.ximgproc.Ximgproc.SLIC;
import static org.opencv.ximgproc.Ximgproc.createSuperpixelSLIC;


public class Polygonize {
    public Polygonize() {
    }

    public static void main(String args[]){
        System.loadLibrary(NATIVE_LIBRARY_NAME);
        //setUseOpenCL(true);
        final String sourceFile = "C:\\Dev\\sppseg\\src\\main\\java\\Data\\bear.jpg";
        final String targetFile = "C:\\Dev\\sppseg\\src\\main\\java\\Data\\testsegmentation-output111.jpg";
        final Mat source = imread(sourceFile, IMREAD_COLOR);

        Mat result = new Mat();

        System.out.println("Creating createSuperpixelSLIC...");

        final SuperpixelSLIC superpixelSLIC = createSuperpixelSLIC(
                source, SLIC, 10, 10.0f);
        System.out.println("Iterating...");
        superpixelSLIC.iterate(10);


        System.out.println("Number of superpixels generated: " + superpixelSLIC.getNumberOfSuperpixels());


        Mat labels = new Mat();
        superpixelSLIC.getLabels(labels);
        double[][] labels2D = Generate2DArray(labels);

        int rows = labels.rows();
        int cols = labels.cols();

        //convex hull
        Map<Double, ArrayList<Point>> points= new HashMap<Double, ArrayList<Point>>();
        for(int k = 0; k<superpixelSLIC.getNumberOfSuperpixels(); ++k) {
            points.put((double) k, new ArrayList<Point>());
        }

        double[][] mask2D = new double[rows][cols];
        for(int i = 0; i< rows; ++i){
            for(int j=0; j< cols; ++j){
                mask2D[i][j] = 0;
                //System.out.println("Indexing point for: " + labels2D[i][j]);

                ArrayList<Point> list = points.get((double)labels2D[i][j]);
                Point p = new Point(i,j);
                p.label = labels2D[i][j];
                list.add(p);
            }
        }
        Mat mask = new Mat (rows, cols, CvType.CV_8UC1);

        for (int i=0; i<superpixelSLIC.getNumberOfSuperpixels(); ++i){
            List<Point> polygon = ConvexHull.makeHull(points.get((double) i));

            int epsilon = 4; //var epsilon = (polygon.size() / (3 * (polygon.size()/4))) * 2;
            //var simplifiedPolygon = RamerDouglasPeucker.douglasPeucker(polygon, epsilon);
            List<Point> simplifiedPolygon = new ArrayList<>();
            RamerDouglasPeucker.ramerDouglasPeucker(polygon, 1, simplifiedPolygon);
            System.out.println("----------------Polygon for label: " + i + " with epsilon: " + epsilon +" ---------------------");
            for(Point p : polygon) {
                Point3 p2f = new Point3();
                p2f.x = p.x;
                p2f.y = p.y;
                System.out.println(p);
                mask2D[(int)p.x][(int)p.y] = 255.0;
                mask.put((int) p.x, (int)p.y, 255.0);
            }
            System.out.println("--------------------------------------------------------------------");
        }

        System.out.println("Getting contour mask...");
        //superpixelSLIC.getLabelContourMask(mask, false);


        Mat mask_inv_slic = new Mat();

        bitwise_not(mask, mask_inv_slic);
        cvtColor(mask_inv_slic, mask_inv_slic, COLOR_GRAY2BGR, 3);
        bitwise_and(source, mask_inv_slic, result);

        System.out.println("Writing...");
        imwrite(targetFile, mask);
    }

    public static double[][] Generate2DArray(Mat superpixels) {
        double[][] ret = new double[superpixels.rows()][superpixels.cols()];

        for (int i = 0; i < superpixels.rows(); ++i) {
            for (int j = 0; j < superpixels.cols(); ++j) {
                double x = superpixels.get(i, j)[0];
                ret[i][j] = x;
            }
        }

        return ret;
    }
}
