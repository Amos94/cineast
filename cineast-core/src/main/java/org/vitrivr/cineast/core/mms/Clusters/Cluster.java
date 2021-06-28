package org.vitrivr.cineast.core.mms.Clusters;

public class Cluster {
    public int id;
    public double inv = 0;        // inv variable for optimization
    public double pixelCount;    // pixels in this cluster
    public double avg_red;     // average red value
    public double avg_green;    // average green value
    public double avg_blue;    // average blue value
    public double sum_red;     // sum red values
    public double sum_green;   // sum green values
    public double sum_blue;     // sum blue values
    public double sum_x;       // sum x
    public double sum_y;       // sum y
    public double avg_x;       // average x
    public double avg_y;       // average y

    public Cluster(int id, int in_red, int in_green,
                   int in_blue, int x, int y,
                   double S, double m) {
        // inverse for distance calculation
        this.inv = 1.0 / ((S / m) * (S / m));
        this.id = id;
        addPixel(x, y, in_red, in_green, in_blue);
        // calculate center with initial one pixel
        calculateCenter();
    }

    public void reset() {
        avg_red = 0;
        avg_green = 0;
        avg_blue = 0;
        sum_red = 0;
        sum_green = 0;
        sum_blue = 0;
        pixelCount = 0;
        avg_x = 0;
        avg_y = 0;
        sum_x = 0;
        sum_y = 0;
    }

    /*
     * Add pixel color values to sum of previously added
     * color values.
     */
    public void addPixel(int x, int y, int in_red,
                  int in_green, int in_blue) {
        sum_x+=x;
        sum_y+=y;
        sum_red  += in_red;
        sum_green+= in_green;
        sum_blue += in_blue;
        pixelCount++;
    }

    public void calculateCenter() {
        // Optimization: using "inverse"
        // to change divide to multiply
        double inv = 1/pixelCount;
        avg_red   = sum_red*inv;
        avg_green = sum_green*inv;
        avg_blue  = sum_blue*inv;
        avg_x = sum_x*inv;
        avg_y = sum_y*inv;
    }

    public double distance(int x, int y,
                    int red, int green, int blue,
                    double S, double m, int w, int h) {
        // power of color difference between
        // given pixel and cluster center
        double dx_color =  (avg_red-red)*(avg_red-red)
                + (avg_green-green)*(avg_green-green)
                + (avg_blue-blue)*(avg_blue-blue);
        // power of spatial difference between
        // given pixel and cluster center
        double dx_spatial = (avg_x-x)*(avg_x-x)+(avg_y-y)*(avg_y-y);
        // Calculate approximate distance D
        // double D = dx_color+dx_spatial*inv;
        // Calculate squares to get more accurate results
        double D = Math.sqrt(dx_color)+Math.sqrt(dx_spatial*inv);
        return D;
    }
}
