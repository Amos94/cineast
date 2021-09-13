package org.vitrivr.cineast.core.mms.OcrPreprocessing;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;

public class OpenCVPreProcessing {

	/**
	 * Enable debug image output
	 */
	public static boolean DEBUG = Boolean.parseBoolean(System.getProperty("DEBUG", "false"));

	public static void main(String[] args) {

		// load the OpenCV native library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		String arg = "C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\flyer.png";
		File f = new File(arg);
		if (!f.exists() || !f.isFile()) {
			return;
		}

		Mat source = Imgcodecs.imread(arg, Imgproc.COLOR_BGR2RGB);
		ArrayList<Point> corners = OcrPreProcessing.detectPage(source, OcrPreProcessing.MIN_PAGE_FRACTION);
		if (DEBUG) {
			corners = OcrPreProcessing.detectPageWithMinRect(source, OcrPreProcessing.MIN_PAGE_FRACTION);
			corners = OcrPreProcessing.detectPageConvexHull(source, OcrPreProcessing.MIN_PAGE_FRACTION);
		}
		Mat cropped = OcrPreProcessing.transform(source, corners);
		Mat ocr = OcrPreProcessing.prepare(cropped, false, true);
		double blurSrc = OcrPreProcessing.varianceOfLaplacian(source);
		double modifiedSrc = OcrPreProcessing.modifiedLaplacian(source);
		//if( blurSrc < 70 && modifiedSrc < 4) {
		//}

		if (!DEBUG) {
			Imgcodecs.imwrite(arg, ocr);
		}
		cropped.release();
		ocr.release();
		source.release();
	}

	/**
	 * @param source - MAT: COLOR_BGR2RGB
	 * */
	public static Mat doTextExtractionPreProcessing(Mat source){
		ArrayList<Point> corners = OcrPreProcessing.detectPage(source, OcrPreProcessing.MIN_PAGE_FRACTION);
		if (DEBUG) {
			corners = OcrPreProcessing.detectPageWithMinRect(source, OcrPreProcessing.MIN_PAGE_FRACTION);
			corners = OcrPreProcessing.detectPageConvexHull(source, OcrPreProcessing.MIN_PAGE_FRACTION);
		}
		Mat cropped = OcrPreProcessing.transform(source, corners);
		Mat ocr = OcrPreProcessing.prepare(cropped, false, true);
		double blurSrc = OcrPreProcessing.varianceOfLaplacian(source);
		double modifiedSrc = OcrPreProcessing.modifiedLaplacian(source);

		//if( blurSrc < 70 && modifiedSrc < 4) {
		//	System.out.println("\n BLUR - Source: varianceOfLaplacian: "+ source + " , modifiedLaplacian: "+blurSrc+", file: " + modifiedSrc);
		//}
		cropped.release();
		source.release();

		return ocr;
	}

}
