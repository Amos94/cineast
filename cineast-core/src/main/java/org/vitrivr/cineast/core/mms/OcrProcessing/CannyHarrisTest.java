package org.vitrivr.cineast.core.mms.OcrProcessing;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.circle;

public class CannyHarrisTest 
{
    public CannyHarrisTest()
    {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_java452");
        
        Mat src, src_gray;
        int thresh = 80; //harris threshold
        int filter_thresh = 50; //binary thresh to get rid of smoothing
        
        src = imread("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\OCR\\m.png");
        src_gray = new Mat(src.size(), src.type());
        Imgproc.cvtColor(src, src_gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(src_gray, src_gray, filter_thresh, 255, Imgproc.THRESH_BINARY_INV);
        
        Mat dst, dst_norm, dst_norm_scaled;
        dst = Mat.zeros(src.size(), CvType.CV_32FC1);
        
        int blockSize = 2;
        int aperatureSize = 3;
        double k = 0.04;
        
        Mat canny = new Mat(src_gray.size(), src_gray.type());
        Imgproc.Canny(src_gray, canny, 150, 200);        imwrite("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\OCR\\canny1.png", canny);

        imwrite("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\OCR\\canny2.png", canny);
        
        Imgproc.cornerHarris(src_gray, dst, blockSize, aperatureSize, k);
        
        dst_norm = new Mat(src.size(), src.type());
        dst_norm_scaled = new Mat(src.size(), src.type());
        Core.normalize(dst, dst_norm, 0, 255, Core.NORM_MINMAX, CvType.CV_32FC1);
        Core.convertScaleAbs(dst_norm, dst_norm_scaled);
        
        int[][] locs = {{1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}, {0,-1}, {-1,1}};
        
        for(int j = 0; j < dst_norm.rows(); j++)
            for(int i = 0; i < dst_norm.cols(); i++)
                for(int pos = 0; pos < 8; pos++)
                    try
                    {
                        if(dst_norm.get(j, i)[0] 
                                - dst_norm.get(j + locs[pos][0], i + locs[pos][1])[0] > thresh)
                            circle(dst_norm_scaled, new Point(i, j), 2, new Scalar(0), 2, 8, 0);
                    } catch(Exception e) {}
        imwrite("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\OCR\\harris1.png", dst_norm_scaled);
        
    }

    public static void main(String[] args) 
    {
        new CannyHarrisTest();
    }
    
}
