package org.vitrivr.cineast.core.mms.Algorithms.Segmentation;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class Vid {

    public static BufferedImage Mat2BufferedImage(Mat m)
    {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (m.channels() > 1)
        {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels()*m.cols()*m.rows();
        byte[] b = new byte[bufferSize];
        m.get(0, 0, b); // get all the pixels
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

    public static void main(String args[]) throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        VideoCapture camera = new VideoCapture("C:\\Dev\\mtproj\\src\\Data\\atrium.avi");
        //VideoCapture camera = new VideoCapture("C:\\Dev\\sppseg\\src\\main\\java\\10.mp4");

        double S = 10; // distance between centers
        double m = 255; // parameter will determine the influence of the spatial distance between pixels when computing the final distance

        if(!camera.isOpened())
            System.out.println("Error opening video stream or file");

        VideoWriter videoWriter = new VideoWriter("C:\\Dev\\mtproj\\src\\Data\\test-segmented1111.avi", VideoWriter.fourcc('x', '2','6','4'),
                camera.get(Videoio.CAP_PROP_FPS), new Size(640,360), true);
        int i=0;
        while (true) {
            final Mat frame = new  Mat();

            if (camera.read(frame)) {
                BufferedImage image = null;
                image = Mat2BufferedImage(frame);

                //my superpixel algo
                Superpixel sp = new Superpixel();
                BufferedImage dstImage = sp.calculate(image,S,m);

                // save the resulting image
                //ImageIO.write(dstImage, "jpg", new File("C:\\Dev\\sppseg\\output\\image" + i +".jpg"));

                Mat res = BufferedImage2Mat(dstImage);
                videoWriter.write(res);
                ++i;
            }
            else {
                camera.release();
                break;
            }
            System.out.println("segmenting frame: " + i);
        }
    }
}
