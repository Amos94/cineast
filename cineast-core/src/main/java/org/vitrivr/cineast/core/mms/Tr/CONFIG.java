package org.vitrivr.cineast.core.mms.Tr;

import org.opencv.core.Scalar;

import java.awt.*;

public class CONFIG {

	public static boolean IS_DEVELOPMENT = false;
	public static boolean PERFORM_EVALUATION = true;
	public static boolean DISPLAY_FRAMES = false;
	public static boolean INITIALIZE_SCHEMA = false;
	public static boolean DB_INSERT = false;
	public static boolean DO_POLY_OPERATIONS = false; //in order to know how to serialize the data

	public static String filename = "C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\atrium.avi";
	public static String davisFolder = "C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\Davis";
	public static int EVALUATION_MAXIMUM_STACK_FRAMES = 5;

	public static String ytVosFolder = "C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\YT-VOS";

	public static int FRAME_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width / 2;
	public static int FRAME_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height / 2;
	
	public static double MIN_BLOB_AREA = 250;
	public static double MAX_BLOB_AREA = 3000;
	
	public static Scalar Colors[] = { new Scalar(255, 0, 0), new Scalar(0, 255, 0),
		new Scalar(0, 0, 255), new Scalar(255, 255, 0),
		new Scalar(0, 255, 255), new Scalar(255, 0, 255),
		new Scalar(255, 127, 255), new Scalar(127, 0, 255),
		new Scalar(127, 0, 127) };
	
	public static double learningRate = 0.005;
	
	public static double _dt = 0.2;
	public static double _Accel_noise_mag = 0.9;
	public static double _dist_thres = 360;
	public static int _maximum_allowed_skipped_frames = 0;
	public static int _max_trace_length = 100;
}
