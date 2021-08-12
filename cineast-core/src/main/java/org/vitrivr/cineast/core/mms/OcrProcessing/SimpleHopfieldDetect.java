package org.vitrivr.cineast.core.mms.OcrProcessing;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgcodecs.Imgcodecs.imread;

public class SimpleHopfieldDetect
{
    
    final String fname1 = "C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\OCR\\u.png";
    final String fname2 = "C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\OCR\\m.png";
    final int canny1 = 150;
    final int canny2 = 200;
    final int dsize = 32;
    final int iterations = 300;
        
    final int[][] locs = {{1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}, {0,-1}, {-1,1}};

    public SimpleHopfieldDetect()
    {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_java452");


        float[] td1 = generate(imread(fname1));
        float[] td2 = generate(imread(fname2));

        Hopfield hop = new Hopfield(dsize*dsize);
        hop.addTrainingData(td1);
        hop.addTrainingData(td2);
        hop.train();
        
        float[] dist = generate(imread("m-dist.png"));
        float[] fixed = hop.recall(dist, iterations);
               
        printArray(td1);
        printArray(td2);
        printArray(fixed);

        if(arraysEq(fixed, td1))
            System.out.println("It's a U!!");
        else if(arraysEq(fixed, td2))
            System.out.println("It's an M!!");
        else
            System.out.println("No idea what that is");
        
        Mat fixedmat = new Mat(new Size(dsize,dsize), CvType.CV_8U);
        for(int i = 0; i < fixed.length; i++)
            fixedmat.put(i/dsize, i%dsize, new byte[] {fixed[i] == 1 ? (byte)127 : 0});
        
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
                double sum = Core.sumElems(pre.submat(new Rect(col*w, row*h, w, h))).val[0];
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
    
    public static void main(String[] args)
    {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_java452");

        new SimpleHopfieldDetect();
    }
}
