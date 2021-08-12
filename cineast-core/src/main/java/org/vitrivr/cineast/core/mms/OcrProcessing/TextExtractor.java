package org.vitrivr.cineast.core.mms.OcrProcessing;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.rectangle;

public class TextExtractor {
    public TextExtractor(){}

    public static void main(String args[]){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_java452");

        new TextExtractor().extractText();
    }

    public void extractText(){
        Mat Main = imread("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\flyer.png");
        Mat rgb = new Mat();

        Imgproc.pyrDown(Main, rgb);
        Mat small = new Mat();
        Imgproc.cvtColor(rgb, small, Imgproc.COLOR_RGB2GRAY);
        Mat grad = new Mat();
        Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3,3));

        Imgproc.morphologyEx(small, grad, Imgproc.MORPH_GRADIENT , morphKernel);
        Mat bw = new Mat();
        Imgproc.threshold(grad, bw, 0.0, 255.0, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        Mat connected = new Mat();
        morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 1));
        Imgproc.morphologyEx(bw, connected, Imgproc.MORPH_CLOSE  , morphKernel);


        Mat mask = Mat.zeros(bw.size(), CvType.CV_8UC1);
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(connected, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        for(int idx = 0; idx < contours.size(); idx++)
        {
            Rect rect = Imgproc.boundingRect(contours.get(idx));

            Mat maskROI = new Mat(mask, rect);
            maskROI.setTo(new Scalar(0, 0, 0));

            Imgproc.drawContours(mask, contours, idx, new Scalar(255, 255, 255), Core.FILLED);

            double r = (double) Core.countNonZero(maskROI)/(rect.width*rect.height);

            if (r > .45 && (rect.height > 8 && rect.width > 8))
                rectangle(rgb, rect.br() , new Point( rect.br().x-rect.width ,rect.br().y-rect.height),  new Scalar(0, 255, 0));


            String outputfile = "C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\np-out.png";
            imwrite(outputfile,rgb);
        }
    }
}
