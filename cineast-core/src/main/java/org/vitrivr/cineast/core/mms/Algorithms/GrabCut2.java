package org.vitrivr.cineast.core.mms.Algorithms;//package Algorithms;


import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import static org.opencv.core.Core.*;
import static org.opencv.core.CvType.CV_8U;
import static org.opencv.imgcodecs.Imgcodecs.*;
import static org.opencv.imgproc.Imgproc.GC_INIT_WITH_RECT;
import static org.opencv.imgproc.Imgproc.grabCut;

/**
 * GrabCut algorithm was designed by Carsten Rother, Vladimir Kolmogorov & Andrew Blake from
 * Microsoft Research Cambridge, UK. in their paper, "GrabCut": interactive foreground extraction
 * using iterated graph cuts . An algorithm was needed for foreground extraction with minimal user
 * interaction, and the result was GrabCut.
 */
public class GrabCut2 {

    public GrabCut2(){}

    private int sideSquare = 50;
    private int backGroundOriginX = 50;
    private int backGroundOriginY = 50;

    public int getSideSquare() {
        return sideSquare;
    }

    public void setSideSquare(int sideSquare) {
        this.sideSquare = sideSquare;
    }

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

    public Mat process(Mat image) throws Exception {
        Mat mat = image;
        Point p1 = new Point(getBackGroundOriginX() - getSideSquare(),
                getBackGroundOriginY() - getSideSquare());
        Point p2 = new Point(getBackGroundOriginX() + getSideSquare(),
                getBackGroundOriginY() + getSideSquare());
        Rect rect = new Rect(p1, p2);

        Mat mask = new Mat();
        Mat finalMask = new Mat();
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();

        grabCut(mat, mask, rect, bgModel, fgModel, 1, GC_INIT_WITH_RECT);

        bgModel.release();
        fgModel.release();
        new Scalar(0,0,0,0);
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
        final String sourceFile = "C:\\Dev\\sppseg\\src\\main\\java\\Data\\bear.jpg";
        final String targetFile = "C:\\Dev\\sppseg\\src\\main\\java\\Data\\bear-gc.jpg";
        final Mat source = imread(sourceFile, IMREAD_COLOR);

        GrabCut2 grabCut2 = new GrabCut2();
        grabCut2.setBackGroundOriginX(330);
        grabCut2.setBackGroundOriginY(300);
        grabCut2.setSideSquare(150);
        Mat result  = grabCut2.process(source);
        imwrite(targetFile, result);

    }
}
