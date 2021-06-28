package org.vitrivr.cineast.core.mms.Algorithms.Segmentation;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.ximgproc.SuperpixelSLIC;
import org.opencv.ximgproc.Ximgproc;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.ximgproc.Ximgproc.createSuperpixelSLIC;

public class SLIC {

    public static void main(String[] args) throws IOException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        int min_element_size = 5;
        BufferedImage input = ImageIO.read(new File("C:\\Dev\\sppseg\\src\\main\\java\\Data\\rc.png"));
        Mat newMat = new Mat();
        newMat = Vid.BufferedImage2Mat(input);
        SuperpixelSLIC x = Ximgproc.createSuperpixelSLIC(newMat, Ximgproc.SLIC,newMat.rows()/9,(float)25);
        x.iterate(3);
        if (min_element_size>0)
            x.enforceLabelConnectivity(min_element_size);
        Mat mask=new Mat();
        x.getLabelContourMask(mask,true);
        newMat.setTo( new Scalar(0,0,255),mask);
        BufferedImage newimg = Vid.Mat2BufferedImage(newMat);
        Mat labels=new Mat();
        x.getLabels(labels);
    }
}

    //---------------------------------------------------------------------------

//    static void display(Mat image, String caption) {
//        // Create image window named "My Image".
//        final CanvasFrame canvas = new CanvasFrame(caption, 1.0);
//
//        // Request closing of the application when the image window is closed.
//        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//
//        // Convert from OpenCV Mat to Java Buffered image for display
//        final OpenCVFrameConverter<Mat> converter = new OpenCVFrameConverter.ToMat();
//        // Show image on window.
//        canvas.showImage(converter.convert(image));
//    }
