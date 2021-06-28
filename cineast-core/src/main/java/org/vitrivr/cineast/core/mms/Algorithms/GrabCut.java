package org.vitrivr.cineast.core.mms.Algorithms;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class GrabCut{
    Scalar color;
    Point tl, br;
    Mat dst;

    private GrabCut(){}

    private void backgroundSubtracting(Mat img, Mat background) {
        color = new Scalar(255, 0, 0, 255);
        Mat firstMask = new Mat();
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();
        Mat mask;
        Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(3.0));
        dst = new Mat();
        Rect rect = new Rect(tl, br);

        Imgproc.grabCut(img, firstMask, rect, bgModel, fgModel, 1, 0 /* GC_INIT_WITH_RECT */);
        Core.compare(firstMask, source/* GC_PR_FGD */, firstMask, Core.CMP_EQ);

        Mat foreground = new Mat(img.size(), CvType.CV_8UC3, new Scalar(255,
                255, 255));
        img.copyTo(foreground, firstMask);

        Imgproc.rectangle(img, tl, br, color);

        Mat tmp = new Mat();
        Imgproc.resize(background, tmp, img.size());
        background = tmp;
        mask = new Mat(foreground.size(), CvType.CV_8UC1, new Scalar(255, 255, 255));

        Imgproc.cvtColor(foreground, mask, 6/* COLOR_BGR2GRAY */);
        Imgproc.threshold(mask, mask, 254, 255, 1 /* THRESH_BINARY_INV */);

        Mat vals = new Mat(1, 1, CvType.CV_8UC3, new Scalar(0.0));
        background.copyTo(dst);

        background.setTo(vals, mask);

        Core.add(background, foreground, dst, mask);

        firstMask.release();
        source.release();
        bgModel.release();
        fgModel.release();
        vals.release();
    }

}
