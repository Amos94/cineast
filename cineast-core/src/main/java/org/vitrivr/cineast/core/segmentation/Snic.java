package org.vitrivr.cineast.core.segmentation;

//Implementation of SNIC (Simple Non-Iterative Clustering)
//Reference:  @inproceedings{snic_cvpr17, author = {Achanta, Radhakrishna and Susstrunk, Sabine}, title = {Superpixels and Polygons using Simple Non-Iterative Clustering}, booktitle = {IEEE Conference on Computer Vision and Pattern Recognition (CVPR)}, year = {2017} }

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;

public class Snic {

    public Snic(int numk, double compactness, String imgPath, int numelements) throws IOException {

        if(numk < 0)
            numk = 200;
        if(compactness < 0)
            compactness = 20;
        if(numelements < 0)
            numelements = 3;

        byte[] imgbytes = extractBytes(imgPath);
        Pair<Integer, Integer> size = getImageSize(imgPath);

        doSegmentation((int)size.first, (int)size.second, numk, compactness, imgbytes, numelements);
    }

    private Pair<Integer,Integer> getImageSize(String path) throws IOException {
        File imgPath = new File(path);
        BufferedImage bufferedImage = ImageIO.read(imgPath);

        Integer w = bufferedImage.getWidth();
        Integer h = bufferedImage.getHeight();

        return new Pair<Integer,Integer>(w,h);
    }

    public byte[] extractBytes (String ImageName) throws IOException {
        // open image
        File imgPath = new File(ImageName);
        BufferedImage bufferedImage = ImageIO.read(imgPath);

        // get DataBufferBytes from Raster
        WritableRaster raster = bufferedImage .getRaster();
        DataBufferByte data   = (DataBufferByte) raster.getDataBuffer();

        return ( data.getData() );
    }

    private static void rgbtolab(int[] rin, int[] gin, int[] bin, int sz, double[] lvec, double[] avec, double[] bvec)
    {
        int i, sR, sG, sB;
        double R, G, B, X, Y, Z;
        double r, g, b;
        final double epsilon = 0.008856; //actual CIE standard
        final double kappa = 903.3; //actual CIE standard

        final double Xr = 0.950456; //reference white
        final double Yr = 1.0; //reference white
        final double Zr = 1.088754; //reference white
        double xr, yr, zr, fx, fy, fz, lval, aval, bval;

        for (i = 0; i < sz; i++)
        {
            sR = rin[i];
            sG = gin[i];
            sB = bin[i];
            R = sR / 255.0;
            G = sG / 255.0;
            B = sB / 255.0;

            if (R <= 0.04045)
            {
                r = R / 12.92;
            }
            else
            {
                r = Math.pow((R + 0.055) / 1.055,2.4);
            }
            if (G <= 0.04045)
            {
                g = G / 12.92;
            }
            else
            {
                g = Math.pow((G + 0.055) / 1.055,2.4);
            }
            if (B <= 0.04045)
            {
                b = B / 12.92;
            }
            else
            {
                b = Math.pow((B + 0.055) / 1.055,2.4);
            }

            X = r * 0.4124564 + g * 0.3575761 + b * 0.1804375;
            Y = r * 0.2126729 + g * 0.7151522 + b * 0.0721750;
            Z = r * 0.0193339 + g * 0.1191920 + b * 0.9503041;

            //------------------------
            // XYZ to LAB conversion
            //------------------------
            xr = X / Xr;
            yr = Y / Yr;
            zr = Z / Zr;

            if (xr > epsilon)
            {
                fx = Math.pow(xr, 1.0 / 3.0);
            }
            else
            {
                fx = (kappa * xr + 16.0) / 116.0;
            }
            if (yr > epsilon)
            {
                fy = Math.pow(yr, 1.0 / 3.0);
            }
            else
            {
                fy = (kappa * yr + 16.0) / 116.0;
            }
            if (zr > epsilon)
            {
                fz = Math.pow(zr, 1.0 / 3.0);
            }
            else
            {
                fz = (kappa * zr + 16.0) / 116.0;
            }

            lval = 116.0 * fy - 16.0;
            aval = 500.0 * (fx - fy);
            bval = 200.0 * (fy - fz);

            lvec[i] = lval;
            avec[i] = aval;
            bvec[i] = bval;
        }
    }

    private static void FindSeeds(final int width, final int height, int numk, ArrayList<Integer> kx, ArrayList<Integer> ky)
    {
        final int sz = width * height;
        int gridstep = (int) (Math.sqrt((double)sz / (double)numk) + 0.5);
        int halfstep = gridstep / 2;
        double h = height;
        double w = width;

        int xsteps = width / gridstep;
        int ysteps = height / gridstep;
        int err1 = Math.abs(xsteps * ysteps - numk);
        int err2 = Math.abs((int)(width / (gridstep - 1)) * (int)(height / (gridstep - 1)) - numk);
        if (err2 < err1)
        {
            gridstep -= 1.0;
            xsteps = width / (gridstep);
            ysteps = height / (gridstep);
        }

        numk = (xsteps * ysteps);
        VectorHelper.resize(kx, numk);
        VectorHelper.resize(ky, numk);
        int n = 0;
        for (int y = halfstep, rowstep = 0; y < height && n < numk; y += gridstep, rowstep++)
        {
            for (int x = halfstep; x < width && n < numk; x += gridstep)
            {
                if (y <= h - halfstep && x <= w - halfstep)
                {
                    kx.set(n, x);
                    ky.set(n, y);
                    n++;
                }
            }
        }
    }

    public static void runSNIC(double[] lv, double[] av, double[] bv, final int width, final int height, int[] labels, int outnumk, final int innumk, final double compactness)
    {
        final int w = width;
        final int h = height;
        final int sz = w * h;
        final int[] dx8 = {-1, 0, 1, 0, -1, 1, 1, -1}; //for 4 or 8 connectivity
        final int[] dy8 = {0, -1, 0, 1, -1, -1, 1, 1}; //for 4 or 8 connectivity
        final int[] dn8 = {-1, -w, 1, w, -1 - w, 1 - w, 1 + w, -1 + w};
        //-------------
        // Find seeds
        //-------------
        ArrayList<Integer> cx = new ArrayList<Integer>(0);
        ArrayList<Integer> cy = new ArrayList<Integer>(0);
        int numk = innumk;
        FindSeeds(width,height,numk,cx,cy); //the function may modify numk from its initial value
        if(numk != cx.size())
            numk = cx.size();
        //-------------
        // Initialize
        //-------------
        Node tempnode = new Node();
        Compare c = new Compare();
        Queue<Node> pq = new PriorityQueue<Node>(numk, c);
        //C++ TO JAVA CONVERTER TODO TASK: The memory management function 'memset' has no equivalent in Java:
        int limit = sz * (Integer.SIZE / Byte.SIZE);
        Arrays.fill(labels,-1);
        for (int k = 0; k < numk; k++)
        {
            Node tn = new Node();
            tn.i = cx.get(k) << 16 | cy.get(k);
            tn.k = k;
            tn.d = 0;
            pq.add(tn);
        }
        ArrayList<Double> kl = new ArrayList<Double>(numk);
        kl = fillArray(kl, numk, 0);
        ArrayList<Double> ka = new ArrayList<Double>(numk);
        ka = fillArray(ka, numk, 0);
        ArrayList<Double> kb = new ArrayList<Double>(numk);
        kb = fillArray(kb, numk, 0);
        ArrayList<Double> kx = new ArrayList<Double>(numk);
        kx = fillArray(kx, numk, 0);
        ArrayList<Double> ky = new ArrayList<Double>(numk);
        ky = fillArray(ky, numk, 0);
        ArrayList<Double> ksize = new ArrayList<Double>(numk);
        ksize = fillArray(ksize, numk, 0);

        final int CONNECTIVITY = 4; //values can be 4 or 8
        final double M = compactness; //10.0;
        final double invwt = (M * M * numk) / (double)sz;

        int qlength = pq.size();
        int pixelcount = 0;
        int xx = 0;
        int yy = 0;
        int ii = 0;
        double ldiff = 0;
        double adiff = 0;
        double bdiff = 0;
        double xdiff = 0;
        double ydiff = 0;
        double colordist = 0;
        double xydist = 0;
        double slicdist = 0;
        //-------------
        // Run main loop
        //-------------
        while (qlength > 0) //while(nodevec.size() > 0)
        {
            Node node = pq.element();
            pq.remove();
            qlength--;
            final int k = node.k;
            final int x = node.i >>> 16 & 0xffff;
            final int y = node.i & 0xffff;
            final int i = y * width + x;

            if (labels[i] < 0)
            {
                labels[i] = k;
                pixelcount++;

                kl.set(k, kl.get(k) + lv[i]);
                ka.set(k, ka.get(k) + av[i]);
                kb.set(k, kb.get(k) + bv[i]);
                kx.set(k, kx.get(k) + x);
                ky.set(k, ky.get(k) + y);
                ksize.set(k, ksize.get(k) + 1.0);

                for (int p = 0; p < CONNECTIVITY; p++)
                {
                    xx = x + dx8[p];
                    yy = y + dy8[p];
                    if (!(xx < 0 || xx >= w || yy < 0 || yy >= h))
                    {
                        ii = i + dn8[p];
                        if (labels[ii] < 0) //create new nodes
                        {
                            ldiff = kl.get(k) - lv[ii] * ksize.get(k);
                            adiff = ka.get(k) - av[ii] * ksize.get(k);
                            bdiff = kb.get(k) - bv[ii] * ksize.get(k);
                            xdiff = kx.get(k) - xx * ksize.get(k);
                            ydiff = ky.get(k) - yy * ksize.get(k);

                            colordist = ldiff * ldiff + adiff * adiff + bdiff * bdiff;
                            xydist = xdiff * xdiff + ydiff * ydiff;
                            slicdist = (colordist + xydist * invwt) / (ksize.get(k) * ksize.get(k)); //late normalization by ksize[k], to have only one division operation

                            tempnode.i = xx << 16 | yy;
                            tempnode.k = k;
                            tempnode.d = slicdist;
                            pq.add(tempnode);
                            qlength++;

                        }
                    }
                }
            }
        }
        outnumk = numk;
        //---------------------------------------------
        // Label the rarely occuring unlabelled pixels
        //---------------------------------------------
        if (labels[0] < 0)
        {
            labels[0] = 0;
        }
        for (int y = 1; y < height; y++)
        {
            for (int x = 1; x < width; x++)
            {
                int i = y * width + x;
                if (labels[i] < 0) //find an adjacent label
                {
                    if (labels[i - 1] >= 0)
                    {
                        labels[i] = labels[i - 1];
                    }
                    else if (labels[i - width] >= 0)
                    {
                        labels[i] = labels[i - width];
                    }
                } //if labels[i] < 0 ends
            }
        }

    }

    private static ArrayList<Double> fillArray(ArrayList<Double> array, int capacity, double filler){
        for (int i=0; i<capacity; ++i)
            array.add(filler);

        return array;
    }

    private Pair<int[], Integer> doSegmentation(int width, int height, int numk, double compactness, byte[] imgbytes, int numelements)
    {
        int sz = width * height;

        int len = (Integer.SIZE / Byte.SIZE) * sz;
        int[] rin = new int[len];
        int[] gin = new int[len];
        int[] bin = new int[len];
        int[] klabels = new int[len]; //original k-means labels

        len = (Double.SIZE / Byte.SIZE) * sz;
        double[] lvec = new double[len];
        double[] avec = new double[len];
        double[] bvec = new double[len];
        //---------------------------
        // Perform color conversion
        //---------------------------
        //if(2 == numdims)
        if (numelements / sz == 1) //if it is a grayscale image, copy the values directly into the lab vectors
        {
            for (int x = 0, ii = 0; x < width; x++) //reading data from column-major MATLAB matrics to row-major C matrices (i.e perform transpose)
            {
                for (int y = 0; y < height; y++)
                {
                    int i = y * width + x;
                    lvec[i] = imgbytes[ii];
                    avec[i] = imgbytes[ii];
                    bvec[i] = imgbytes[ii];
                    ii++;
                }
            }
        }
        else //else covert from rgb to lab
        {
            if (true) //convert from rgb to cielab space
            {
                for (int x = 0, ii = 0; x < width; x++) //reading data from column-major MATLAB matrics to row-major C matrices (i.e perform transpose)
                {
                    for (int y = 0; y < height; y++)
                    {
                        int i = y * width + x;
                        rin[i] = imgbytes[ii];
                        gin[i] = imgbytes[ii + sz];
                        bin[i] = imgbytes[ii + sz + sz];
                        ii++;
                    }
                }
                rgbtolab(rin,gin,bin,sz,lvec,avec,bvec);
            }
            else //else use rgb values directly
            {
                for (int x = 0, ii = 0; x < width; x++) //reading data from column-major MATLAB matrics to row-major C matrices (i.e perform transpose)
                {
                    for (int y = 0; y < height; y++)
                    {
                        int i = y * width + x;
                        lvec[i] = imgbytes[ii];
                        avec[i] = imgbytes[ii + sz];
                        bvec[i] = imgbytes[ii + sz + sz];
                        ii++;
                    }
                }
            }
        }
        //---------------------------
        // Compute superpixels
        //---------------------------
        int numklabels = 0;
        runSNIC(lvec, avec, bvec, width, height, klabels, numklabels, numk, compactness);
        //---------------------------
        // Assign output labels
        //---------------------------
        int[][]plhs = new int[width][height];
        int[] outlabels = new int[width*height];
        for (int x = 0, ii = 0; x < width; x++) //copying data from row-major C matrix to column-major MATLAB matrix (i.e. perform transpose)
        {
            for (int y = 0; y < height; y++)
            {
                int i = y * width + x;
                outlabels[ii] = klabels[i];
                ii++;
            }
        }
        //---------------------------
        // Assign number of labels/seeds
        //---------------------------
        int outputNumSuperpixels = (int)plhs[1][1]; //gives a void*, cast it to int*
        outputNumSuperpixels = numk;

        System.out.println(outputNumSuperpixels);
        for(int i=0; i< outlabels.length; ++i)
            System.out.print(outlabels[i] + " ");

        return new Pair<int[], Integer>(outlabels, outputNumSuperpixels);
    }
}
