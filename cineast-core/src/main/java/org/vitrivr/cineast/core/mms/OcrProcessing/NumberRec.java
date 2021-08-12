package org.vitrivr.cineast.core.mms.OcrProcessing;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.opencv.imgcodecs.Imgcodecs.imencode;
import static org.opencv.imgcodecs.Imgcodecs.imread;

public class NumberRec implements KeyListener
{
    
    final int canny1 = 150;
    final int canny2 = 200;
    final int dsize = 24;
    final int iterations = 300;
        
    final int[][] locs = {{1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}, {0,-1}, {-1,1}};
    
    private boolean done = false;
    
    private int maxIterations = 10000;
    
    private int[] exts = {1, 0, 1, 0, 0, 0, 0, 0, 0, 0};
    private String[] extsStr = {".jpg", ".png"};

    public NumberRec()
    {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_java452");


        Hopfield hop = new Hopfield(dsize*dsize);
        for(int i = 0; i < 4; i++) //change to choose which nums to upload
            hop.addTrainingData(generate(imread("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\OCR\\num_training\\" + i + extsStr[exts[i]])));
        hop.train();
        
        float[] one = generate(imread("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\OCR\\num_training\\1.jpg")); //image to try to recall
        WebcamUM.imshow(WebcamUM.floatToMat(one, dsize), "Original");
        float[] ret = hop.recall(one, iterations);
        WebcamUM.imshow(WebcamUM.floatToMat(ret, dsize), "Recalled");
        
    }
    
    private float[] generate(Mat src)
    {        
        Mat pre = preprocess(src);
        Mat data_img = Mat.zeros(new Size(dsize, dsize), CvType.CV_8U);
        float[] data = new float[dsize*dsize];
        
        //create dsize*dsize version
        int w = pre.cols() / data_img.cols();
        int h = pre.rows() / data_img.rows();
        for(int row = 0; row < dsize; row++)
            for(int col = 0; col < dsize; col++)
            {
                double[] sumparts = Core.sumElems(pre.submat(new Rect(col*w, row*h, w, h))).val;
                double sum = sumparts[0] + sumparts[1] + sumparts[2];
                System.out.println(sum);
                if(sum > 0)
                {
                    data_img.put(row, col, new byte[] {(byte)127});
                    data[row*dsize + col] = 1f;
                }
                else
                    data[row*dsize + col] = -1f;
            }
        return data;
    }
    
    /** Apply canny */
    private Mat preprocess(Mat img)
    {
        Mat img_gray = new Mat(img.size(), img.type());
        Imgproc.cvtColor(img, img_gray, Imgproc.COLOR_BGR2GRAY);
        
        Mat canny = new Mat(img.size(), img.type());
        Imgproc.Canny(img_gray, canny, canny1, canny2);        

        return canny;
    }
    
    private boolean arraysEq(float[] a1, float[] a2)
    {
        if(a1.length != a2.length)
            return false;
        for(int i = 0; i < a1.length; i++)
            if(a1[i] != a2[i])
                return false;
            
        return true;
    }
    
    private void printArray(float[] a)
    {
        for(float f : a)
            System.out.print(f + ", ");
        System.out.println();
    }
    
    public Mat floatToMat(float[] f)
    {
        Mat mat = new Mat(new Size(dsize,dsize), CvType.CV_8U);
        for(int i = 0; i < f.length; i++)
            mat.put(i/dsize, i%dsize, new byte[] {f[i] == 1 ? (byte)127 : 0});
        return mat;
    }
    
    public static void imshow(JFrame frame, Mat img)
    {
        Mat resized = new Mat(img.size(), img.type());
        Imgproc.resize(img, resized, new Size(768,768), 0, 0, Imgproc.INTER_NEAREST);
        
        MatOfByte mob = new MatOfByte();
        imencode(".jpg", resized, mob);
        byte[] bytearray = mob.toArray();
        
        BufferedImage bimg;
        try
        {
            InputStream in = new ByteArrayInputStream(bytearray);
            bimg = ImageIO.read(in);
            try
            {
                frame.getContentPane().remove(0);
            } catch(ArrayIndexOutOfBoundsException e) {}
            frame.getContentPane().add(new JLabel(new ImageIcon(bimg)));
            frame.pack();
        }
        catch(Exception e) { System.err.println(e); }
    }
    
    public static void imshow(Mat img, String title)
    {
        JFrame otherframe = new JFrame(title);
        otherframe.setSize((int)img.size().width, (int)img.size().height);
        otherframe.setVisible(true);
        otherframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        imshow(otherframe, img);
    }
    
    public static void main(String[] args)
    {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_java452");

        new NumberRec();
    }

    @Override
    public void keyTyped(KeyEvent e) 
    {
        done = true;
    }

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
    
}
