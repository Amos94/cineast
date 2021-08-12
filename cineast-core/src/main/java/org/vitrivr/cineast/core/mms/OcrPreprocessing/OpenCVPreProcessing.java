package org.vitrivr.cineast.core.mms.OcrPreprocessing;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpenCVPreProcessing {

	private static final Logger LOGGER = Logger.getLogger(OpenCVPreProcessing.class.getName());
	/**
	 * Enable debug image output
	 */
	public static boolean DEBUG = Boolean.parseBoolean(System.getProperty("DEBUG", "false"));

	public static void main(String[] args) {

		// load the OpenCV native library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		LOGGER.setLevel(Level.SEVERE);
		OcrPreProcessing.LOGGER.setLevel(Level.SEVERE);

		String arg = "C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\example.png";
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
		if( blurSrc < 70 && modifiedSrc < 4) {
			LOGGER.log(Level.SEVERE, "\n BLUR - Source: varianceOfLaplacian: {1}, modifiedLaplacian: {2}, file: {0}", new Object[] {arg, blurSrc, modifiedSrc});
		}
//			LOGGER.log(Level.SEVERE, "\n BLUR - Source: varianceOfLaplacian: {1}, modifiedLaplacian: {2}, file: {0}", new Object[] {arg, blurSrc, modifiedSrc});

		if (!DEBUG) {
			Imgcodecs.imwrite(arg, ocr);
		}
		cropped.release();
		ocr.release();
		source.release();
	}

}
