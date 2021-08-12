package org.vitrivr.cineast.core.mms.OcrProcessing;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.bytedeco.javacpp.opencv_videoio.CV_CAP_PROP_FRAME_HEIGHT;
import static org.bytedeco.javacpp.opencv_videoio.CV_CAP_PROP_FRAME_WIDTH;
import static org.opencv.imgcodecs.Imgcodecs.imencode;
import static org.opencv.imgcodecs.Imgcodecs.imread;

public class WebcamUM implements KeyListener
{
    
    final String fname1 = "C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\OCR\\u.png";
    final String fname2 = "C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\OCR\\m.png";
    final int canny1 = 150;
    final int canny2 = 200;
    final int dsize = 32;
    final int iterations = 300;
        
    final int[][] locs = {{1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}, {0,-1}, {-1,1}};
    
    private boolean done = false;
    
    private int maxIterations = 40000;

    public WebcamUM()
    {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_java452");


        Mat t1img = imread(fname1);
        Mat t2img = imread(fname2);
        
        imshow(t1img, "U Training Image");
        imshow(t2img, "M Training Image");
        
        float[] td1 = generate(t1img);
        float[] td2 = generate(t2img);
        
        Hopfield hop = new Hopfield(dsize*dsize);
        hop.addTrainingData(td1);
        hop.addTrainingData(td2);
        hop.train();
        
        VideoCapture cap = new VideoCapture(0);
        if(!cap.isOpened())
            System.err.println("Couldn't open video stream");
        
        imshow(floatToMat(td1, dsize), "U Training Data");
        imshow(floatToMat(td2, dsize), "M Training Data");
        
        Mat img = new Mat(new Size(cap.get(CV_CAP_PROP_FRAME_WIDTH), cap.get(CV_CAP_PROP_FRAME_HEIGHT)), CvType.CV_8UC3);
        JFrame vidFrame = new JFrame("Video");
        vidFrame.setSize(img.width(), img.height());
        vidFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        vidFrame.setVisible(true);
        vidFrame.addKeyListener(this);
        try {
            done = false;
            while(!done)
            {
                cap.read(img);
                imshow(vidFrame, img);
            }
            float[] in = generate(img);
            imshow(floatToMat(in, dsize), "Input");
            
            float[] sync, async;
            
            hop.loadInputs(in);
            JFrame syncFrame = new JFrame("Sync");
            syncFrame.setSize(512, 512);
            syncFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            syncFrame.setVisible(true);
            for(int it = 0; it < maxIterations; it++)
            {
                sync = hop.recallSync(HopfieldCompleteSync.randomInts(hop.numInputs, 10));
                imshow(syncFrame, floatToMat(sync, dsize));
                if(arraysEq(sync, td1))
                {
                    JOptionPane.showMessageDialog(null, "It's a U");
                    break;
                }
                else if(arraysEq(sync, td2))
                {
                    JOptionPane.showMessageDialog(null, "It's an M");
                    break;
                }
                Thread.sleep(5);
            }
            
            hop.loadInputs(in);
            JFrame asyncFrame = new JFrame("Async");
            asyncFrame.setSize(512, 512);
            asyncFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            asyncFrame.setVisible(true);
            for(int it = 0; it < maxIterations; it++)
            {
                async = hop.recallSingle(it % hop.numInputs);
                imshow(asyncFrame, floatToMat(async, dsize));
                if(arraysEq(async, td1))
                {
                    JOptionPane.showMessageDialog(null, "It's a U");
                    break;
                }
                else if(arraysEq(async, td2))
                {
                    JOptionPane.showMessageDialog(null, "It's an M");
                    break;
                }
            }
            
        } catch(Exception e) { System.err.println(e); }
        
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
    
    public static boolean arraysEq(float[] a1, float[] a2)
    {
        if(a1.length != a2.length)
            return false;
        for(int i = 0; i < a1.length; i++)
            if(a1[i] != a2[i])
                return false;
            
        return true;
    }
    
    public static void printArray(float[] a)
    {
        for(float f : a)
            System.out.print(f + ", ");
        System.out.println();
    }
    
    public static Mat floatToMat(float[] f, int dsize)
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
        imshow(otherframe, img);
    }
    
    public static void main(String[] args)
    {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_java452");

        new WebcamUM();
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
