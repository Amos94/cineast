package org.vitrivr.cineast.core.mms.Tr;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.Core.*;
import static org.opencv.core.CvType.CV_8U;
import static org.opencv.imgcodecs.Imgcodecs.*;
import static org.opencv.imgproc.Imgproc.*;

/**
 * GrabCut algorithm was designed by Carsten Rother, Vladimir Kolmogorov & Andrew Blake from
 * Microsoft Research Cambridge, UK. in their paper, "GrabCut": interactive foreground extraction
 * using iterated graph cuts . An algorithm was needed for foreground extraction with minimal user
 * interaction, and the result was GrabCut.
 */
public class GC {
    public void init() {
        System.loadLibrary("opencv-452");
    }
    public GC(){}

    private int backGroundOriginX = 0;
    private int backGroundOriginY = 0;
    private int width = 0;
    private int height = 0;

    public int getBackGroundOriginX() {
        return backGroundOriginX;
    }
    public void setBackGroundOriginX(int backGroundOriginX) {
        this.backGroundOriginX = backGroundOriginX;
    }

    public int getBackGroundOriginY() {
        return backGroundOriginY;
    }
    public void setBackGroundOriginY(int backGroundOriginY) {
        this.backGroundOriginY = backGroundOriginY;
    }

    public int getWidth(){return width;}
    public void setWidth(int width) { this.width = width;}

    public int getHeight(){return height;}
    public void setHeight(int height) { this.height = height;}

    public Mat process(Mat image){
        Mat src = image;
        Mat mat = new Mat();
        Imgproc.cvtColor(src,mat, COLOR_BGRA2BGR);

        Point topRightPoint = new Point(getBackGroundOriginX(),
                getBackGroundOriginY());
        Point bottomLeftPoint = new Point(getBackGroundOriginX() + getWidth(),
                getBackGroundOriginY() + getHeight());
        Rect rect = new Rect(topRightPoint, bottomLeftPoint);

        Mat mask = new Mat();
        Mat finalMask = new Mat();
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();

        grabCut(mat, mask, rect, bgModel, fgModel, 1, GC_INIT_WITH_RECT);

        //SpecClustering sc = new SpecClustering(mask);
        //sc.MatTo2DArray();
        //sc.MatToMatrix();

        bgModel.release();
        fgModel.release();
        new Scalar(0,0,0);
        Mat fg_mask = mask.clone();
        Mat pfg_mask = mask.clone();
        Mat source = new Mat(1, 1, CV_8U, new Scalar(3.0));
        compare(mask, source, pfg_mask, CMP_EQ);
        source = new Mat(1, 1, CV_8U, new Scalar(1.0));
        compare(mask, source, fg_mask, CMP_EQ);
        bitwise_or(pfg_mask, fg_mask, finalMask);
        Mat fg_foreground = new Mat(mat.size(), mat.type(), new Scalar(0,0,0,0));
        Mat pfg_foreground = new Mat(mat.size(), mat.type(), new Scalar(0,0,0,0));
        mat.copyTo(fg_foreground, fg_mask);
        mat.copyTo(pfg_foreground, pfg_mask);

        bitwise_or(fg_foreground, pfg_foreground, mat);

        fg_mask.release();
        finalMask.release();
        pfg_foreground.release();
        fg_foreground.release();

        return mat;

    }

    public static void main(String args[]) throws Exception {
        System.loadLibrary("opencv_java452");
        final String sourceFile = "C:\\Dev\\fork\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\bear.jpg";
        final String targetFile = "C:\\Dev\\fork\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\bear-gc.jpg";
        final Mat source = imread(sourceFile, IMREAD_COLOR);

        GC grabCut2 = new GC();
        grabCut2.setBackGroundOriginX(330);
        grabCut2.setBackGroundOriginY(300);
        grabCut2.setWidth(150);
        grabCut2.setHeight(150);
        Mat result  = grabCut2.process(source);
        imwrite(targetFile, result);
    }
}
