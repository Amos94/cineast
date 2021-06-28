package org.vitrivr.cineast.core.mms.Algorithms.Segmentation;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;
import org.opencv.ximgproc.SuperpixelLSC;
import org.opencv.ximgproc.SuperpixelSEEDS;
import org.opencv.ximgproc.SuperpixelSLIC;
import org.opencv.ximgproc.Ximgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.opencv.core.Core.bitwise_and;
import static org.opencv.core.Core.bitwise_not;
import static org.opencv.imgcodecs.Imgcodecs.*;
import static org.opencv.imgproc.Imgproc.COLOR_GRAY2BGR;
import static org.opencv.imgproc.Imgproc.cvtColor;


public class SeedsSuperpixel {
    public SeedsSuperpixel(){}

    public static void main(String args[]) throws IOException {
        System.loadLibrary("opencv_java452");
        //setUseOpenCL(true);
        final String sourceFile = "C:\\Dev\\mtproj\\src\\Data\\bear.jpg";
        final String targetFile = "C:\\Dev\\mtproj\\src\\Data\\testsegmentation-output-new.jpg";
        final Mat source = imread(sourceFile, IMREAD_COLOR);

        Mat result = new Mat();
        boolean runSeeds = false;
        boolean dolsc = false;


        if (runSeeds && !dolsc) {
            System.out.println("Creating SuperpixelSEEDS...");
            final SuperpixelSEEDS superpixelSEEDS = Ximgproc.createSuperpixelSEEDS(
                    source.cols(),
                    source.rows(),
                    source.channels(),
                    100,
                    4,
                    4,
                    5,
                    false);
            System.out.println("Iterating...");
            superpixelSEEDS.iterate(source, 4);
            System.out.println("Getting contour mask...");
            superpixelSEEDS.getLabelContourMask(result, true);
        } else if(!runSeeds && !dolsc) {
            Mat mask = new Mat();
            System.out.println("Creating createSuperpixelSLIC...");

            final SuperpixelSLIC superpixelSLIC = Ximgproc.createSuperpixelSLIC(
                    source, Ximgproc.SLIC, 10, 10.0f);
            System.out.println("Iterating...");
            superpixelSLIC.iterate(10);

            System.out.println("Number of superpixels generated: " + superpixelSLIC.getNumberOfSuperpixels());
            System.out.println("Getting contour mask...");
            superpixelSLIC.getLabelContourMask(mask, false);


            Mat mask_inv_slic = new Mat();

            bitwise_not(mask, mask_inv_slic);
            cvtColor(mask_inv_slic, mask_inv_slic, COLOR_GRAY2BGR, 3);
            bitwise_and(source, mask_inv_slic, result);

            Mat labels = new Mat();
            superpixelSLIC.getLabels(labels);
            //SpecClustering sc = new SpecClustering(mask);
            //sc.MatTo2DArray();
            //sc.MatToMatrix();
        }else if(dolsc && !runSeeds){
            System.out.println("Creating LSC...");
            final SuperpixelLSC lsc = Ximgproc.createSuperpixelLSC(
                    source, 50, 0.25f);
            System.out.println("Iterating...");
            lsc.iterate(10);

            lsc.getLabels(result);
            System.out.println("Number of superpixels generated: " + lsc.getNumberOfSuperpixels());
            System.out.println("Getting contour mask...");
            lsc.getLabelContourMask(result, true);
        }

        System.out.println("Writing...");
        imwrite(targetFile, result);
    }

    public static void omain(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        VideoCapture camera = new VideoCapture("C:\\Dev\\sppseg\\src\\main\\java\\bike.avi");

        if (!camera.isOpened())
            System.out.println("Error opening video stream or file");

        int codec = VideoWriter.fourcc('F', 'M', 'P', '4');
        VideoWriter videoWriter = new VideoWriter("test.mp4", codec,
            Videoio.CAP_PROP_FPS, new Size(320, 240));

        int i = 0;
        while (true) {
            final Mat frame = new Mat();

            if (camera.read(frame)) {
                Mat res = CVSLIC(frame);
                videoWriter.write(res);
                ++i;
            } else {
                camera.release();
                break;
            }
            System.out.println("segmenting frame: " + i);
        }
    }

    public static Mat CVSLIC(Mat source){
        Mat result = new Mat();
        Mat mask = new Mat();
        System.out.println("Creating createSuperpixelSLIC...");
        final SuperpixelSLIC superpixelSLIC = Ximgproc.createSuperpixelSLIC(
                source, Ximgproc.SLIC, 10, 10.0f);
        System.out.println("Iterating...");
        superpixelSLIC.iterate(10);

        System.out.println("Number of superpixels generated: " + superpixelSLIC.getNumberOfSuperpixels());
        System.out.println("Getting contour mask...");
        superpixelSLIC.getLabelContourMask(mask, true);

        Mat mask_inv_slic = new Mat();
        bitwise_not(mask, mask_inv_slic);
        cvtColor(mask_inv_slic, mask_inv_slic, COLOR_GRAY2BGR, 3);
        bitwise_and(source, mask_inv_slic, result);

        return result;
    }

    public static Mat CVSEEDS(Mat source){
        Mat result = new Mat();
        System.out.println("Creating SuperpixelSEEDS...");
        final SuperpixelSEEDS superpixelSEEDS = Ximgproc.createSuperpixelSEEDS(
                source.cols(),
                source.rows(),
                source.channels(),
                100,
                4,
                4,
                5,
                false);
        System.out.println("Iterating...");
        superpixelSEEDS.iterate(source, 4);
        System.out.println("Getting controur mask...");
        superpixelSEEDS.getLabelContourMask(result, true);

        return result;
    }

    public static BufferedImage Mat2BufferedImage(Mat m)
    {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (m.channels() > 1)
        {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels()*m.cols()*m.rows();
        byte[] b = new byte[bufferSize];
        //m.get(0, 0, b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }

    public static Mat BufferedImage2Mat(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", byteArrayOutputStream);
        byteArrayOutputStream.flush();
        return Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_UNCHANGED);
    }
}
