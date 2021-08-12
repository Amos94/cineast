package org.vitrivr.cineast.core.mms.OcrPreprocessing;

import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import java.util.Comparator;

/**
* 		  Compare two OpenCV contours based on their area. Larger contour areas are coming first.
 */
public class ContourComparator implements Comparator<MatOfPoint> {

	@Override
	public int compare(MatOfPoint lhs, MatOfPoint rhs) {
		return Double.valueOf(Imgproc.contourArea(rhs)).compareTo(Imgproc.contourArea(lhs));
	}
}
