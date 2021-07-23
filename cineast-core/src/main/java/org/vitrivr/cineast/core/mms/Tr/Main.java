package org.vitrivr.cineast.core.mms.Tr;

import com.google.gson.JsonObject;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.vitrivr.cineast.core.mms.Algorithms.Polygons.Algos.Epsilon;
import org.vitrivr.cineast.core.mms.Algorithms.Polygons.Algos.models.Polygon;
import org.vitrivr.cineast.core.mms.Algorithms.Polygons.RamerDouglasPeucker;
import org.vitrivr.cineast.core.mms.Helper.ConvexHull;
import org.vitrivr.cineast.core.mms.Helper.Volume;
import org.vitrivr.cineast.core.mms.Helper.Voxel;
import org.vitrivr.cottontail.grpc.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.vitrivr.cineast.core.mms.Algorithms.Polygons.Algos.helpers.PolyBoolHelper.point;
import static org.vitrivr.cineast.core.mms.Tr.CONFIG.IS_DEVELOPMENT;

public class Main {
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		System.loadLibrary("opencv_java452");
		// System.loadLibrary("opencv_java2410");
	}
	//TODO serialize data in JSON

	/** Cottontail DB gRPC channel; adjust Cottontail DB host and port according to your needs. */
	private static final ManagedChannel CHANNEL  = ManagedChannelBuilder.forAddress("127.0.0.1", 1865).usePlaintext().build();

	/** Cottontail DB Stub for DDL operations (e.g. create a new Schema or Entity). */
	private static final DDLGrpc.DDLBlockingStub DDL_SERVICE = DDLGrpc.newBlockingStub(CHANNEL);

	/** Cottontail DB Stub for DML operations (i.e. inserting Data). */
	private static final DMLGrpc.DMLBlockingStub DML_SERVICE = DMLGrpc.newBlockingStub(CHANNEL);

	/** Cottontail DB Stub for DQL operations (i.e. issuing queries).*/
	private static final DQLGrpc.DQLBlockingStub DQL_SERVICE = DQLGrpc.newBlockingStub(CHANNEL);

	/** Cottontail DB Stub for Transaction management.*/
	private static final TXNGrpc.TXNBlockingStub TXN_SERVICE = TXNGrpc.newBlockingStub(CHANNEL);

	/** Name of the Cottontail DB Schema. */
	private static final String SCHEMA_NAME = "segmentation";

	static Mat imag = null;
	static Mat orgin = null;
	static Mat kalman = null;
	public static Tracker tracker;
	private static Vector<Float> rect_volume;
	private static Vector<Float> poly_volume;
	private static JsonObject volume_json;
	private static JsonObject polygons;
	private static JsonObject lastObject;
	private static boolean insertedFirstBB;
	private boolean insertedLastBB = false;
	private static DatabaseHelper dbHelper;

	public static void main(String[] args) throws InterruptedException, IOException {
		dbHelper = new DatabaseHelper();
		if (args.length>0){
			CONFIG.filename = args[0];
		}


		JLabel vidpanel = new JLabel();
		JLabel vidpanel2 = new JLabel();
		JLabel vidpanel3 = new JLabel();
		JLabel vidpanel4 = new JLabel();

		if(CONFIG.DISPLAY_FRAMES) {
			JFrame jFrame = new JFrame("MULTIPLE-TARGET TRACKING");
			jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			jFrame.setContentPane(vidpanel);
			jFrame.setSize(CONFIG.FRAME_WIDTH, CONFIG.FRAME_HEIGHT);
			jFrame.setLocation((3 / 4)
					* Toolkit.getDefaultToolkit().getScreenSize().width, (3 / 4)
					* Toolkit.getDefaultToolkit().getScreenSize().height);
			jFrame.setVisible(true);

			// ////////////////////////////////////////////////////////
			JFrame jFrame2 = new JFrame("BACKGROUND SUBSTRACTION");
			jFrame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			jFrame2.setContentPane(vidpanel2);
			jFrame2.setSize(CONFIG.FRAME_WIDTH, CONFIG.FRAME_HEIGHT);
			jFrame2.setLocation(
					Toolkit.getDefaultToolkit().getScreenSize().width / 2, (3 / 4)
							* Toolkit.getDefaultToolkit().getScreenSize().height);
			jFrame2.setVisible(true);
			// ////////////////////////////////////////////////////////

			// ////////////////////////////////////////////////////////
			JFrame jFrame3 = new JFrame("VIDEO INPUT");
			jFrame3.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			jFrame3.setContentPane(vidpanel3);
			jFrame3.setSize(CONFIG.FRAME_WIDTH, CONFIG.FRAME_HEIGHT);
			jFrame3.setLocation((3 / 4)
					* Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit
					.getDefaultToolkit().getScreenSize().height / 2);
			jFrame3.setVisible(false);
			// ////////////////////////////////////////////////////////

			// ////////////////////////////////////////////////////////
			JFrame jFrame4 = new JFrame("KALMAN FILTER");
			jFrame4.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			jFrame4.setContentPane(vidpanel4);
			jFrame4.setSize(CONFIG.FRAME_WIDTH, CONFIG.FRAME_HEIGHT);
			jFrame4.setLocation(
					Toolkit.getDefaultToolkit().getScreenSize().width / 2, Toolkit
							.getDefaultToolkit().getScreenSize().height / 2);
			jFrame4.setVisible(false);
			// ////////////////////////////////////////////////////////
		}

		List<Path> files = new ArrayList<Path>();
		Stream<Path> davis = Files.walk(Paths.get("C:/DEV/cineast/cineast-core/src/main/java/org/vitrivr/cineast/core/mms/Data/Davis/"));
		files.addAll(davis.filter(Files::isRegularFile).collect(Collectors.toList()));

		Stream<Path> yt = Files.walk(Paths.get("C:/DEV/cineast/cineast-core/src/main/java/org/vitrivr/cineast/core/mms/Data/YT-VOS/"));
		files.addAll(yt.filter(Files::isRegularFile).collect(Collectors.toList()));

		if(CONFIG.DB_INSERT) {
			List<String> processedFiles = new ArrayList<>();
			try (Stream<String> stream = Files.lines(Paths.get("C:/DEV/cineast/cineast-core/src/main/java/org/vitrivr/cineast/core/mms/Data/status.txt"))) {
				List<String> strs = stream.collect(Collectors.toList());
				for (int i = 0; i < strs.size(); ++i) {
					String fn = strs.get(i).split(" ")[1];
					processedFiles.add(fn);
				}
			}
			for (int fidx = 0; fidx < files.size(); ++fidx) {
				for (int pf = 0; pf < processedFiles.size(); ++pf)
					if (files.get(fidx).toString().contains(processedFiles.get(pf)))
						files.remove(fidx);
			}
		}

		if(CONFIG.PERFORM_EVALUATION) {
			List<String> processedFiles = new ArrayList<>();
			try (Stream<String> stream = Files.lines(Paths.get("C:/DEV/cineast/cineast-core/src/main/java/org/vitrivr/cineast/core/mms/Data/status.txt"))) {
				List<String> strs = stream.collect(Collectors.toList());
				for (int i = 0; i < strs.size(); ++i) {
					String fn = strs.get(i).split(" ")[1];
					processedFiles.add(fn);
				}
			}
			List<Path> temp = new ArrayList<>();
			for (int fidx = 0; fidx < files.size(); ++fidx) {
				for (int pf = 0; pf < processedFiles.size(); ++pf)
					if (files.get(fidx).toString().contains(processedFiles.get(pf)))
						temp.add(files.get(fidx));
			}
			files = temp;
		}

		for(int fidx=0; fidx<files.size(); ++fidx) {
			String[] fpath = files.get(fidx).toString().split("\\\\");
			Collections.reverse(Arrays.asList(fpath));
			String fileName = fpath[0];
			if(!fileName.contains(".avi"))
				continue;

			System.out.println(">>> PROCESSING: " + fileName);

			if(CONFIG.DB_INSERT)
				Files.write(Paths.get("C:/DEV/cineast/cineast-core/src/main/java/org/vitrivr/cineast/core/mms/Data/status.txt"), ("processing " + fileName + "\n").getBytes(), StandardOpenOption.APPEND);

			rect_volume = new Vector<Float>();
			poly_volume = new Vector<Float>();
			volume_json = new JsonObject();
			polygons = new JsonObject();
			insertedFirstBB = false;

			Mat frame = new Mat();
			Mat outbox = new Mat();
			Mat diffFrame = null;
			Vector<Rect> array = new Vector<Rect>();

			BackgroundSubtractorMOG2 mBGSub = Video
					.createBackgroundSubtractorMOG2();

			tracker = new Tracker((float) CONFIG._dt,
					(float) CONFIG._Accel_noise_mag, CONFIG._dist_thres,
					CONFIG._maximum_allowed_skipped_frames,
					CONFIG._max_trace_length);

			// Thread.sleep(1000);
			VideoCapture camera = new VideoCapture();
			//camera.open(CONFIG.filename);
			camera.open(files.get(fidx).toString());
			// VideoCapture camera = new VideoCapture(0);
			int i = 0;

			if (!camera.isOpened()) {
				System.out.print("Can not open Camera, try it later.");
				return;
			}
			int idx = 0;
			int index = 0;
			int frameNumber = 0;
			while (true) {
				if (!camera.read(frame))
					break;
				Imgproc.resize(frame, frame, new Size(CONFIG.FRAME_WIDTH, CONFIG.FRAME_HEIGHT),
						0., 0., Imgproc.INTER_LINEAR);
				imag = frame.clone();
				orgin = frame.clone();
				kalman = frame.clone();
				if (i == 0) {
					// jFrame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
					diffFrame = new Mat(outbox.size(), CvType.CV_8UC1);
					diffFrame = outbox.clone();
				}

				if (i == 1) {
					diffFrame = new Mat(frame.size(), CvType.CV_8UC1);
					processFrame(camera, frame, diffFrame, mBGSub);
					frame = diffFrame.clone();

					array = detectContours(diffFrame, index, frameNumber);
					++index;
					// ///////
					Vector<Point> detections = new Vector<>();
					// detections.clear();
					Iterator<Rect> it = array.iterator();
					while (it.hasNext()) {
						Rect obj = it.next();

						int ObjectCenterX = (int) ((obj.tl().x + obj.br().x) / 2);
						int ObjectCenterY = (int) ((obj.tl().y + obj.br().y) / 2);

						Point pt = new Point(ObjectCenterX, ObjectCenterY);
						detections.add(pt);
					}
					// ///////

					if (array.size() > 0) {
						tracker.update(array, detections, imag);
						Iterator<Rect> it3 = array.iterator();
						while (it3.hasNext()) {
							Rect obj = it3.next();

							if (IS_DEVELOPMENT) {
								int ObjectCenterX = (int) ((obj.tl().x + obj.br().x) / 2);
								int ObjectCenterY = (int) ((obj.tl().y + obj.br().y) / 2);

								Point pt = new Point(ObjectCenterX, ObjectCenterY);
								//THE GRABCUT GIVEN THE RECTANGLES
								GC gc = new GC();
								gc.setBackGroundOriginX((int) obj.tl().x);
								gc.setBackGroundOriginY((int) obj.tl().y);
								gc.setWidth((int) (obj.br().x + obj.tl().x));
								gc.setHeight((int) (obj.br().y + obj.tl().y));
								Mat result = gc.process(imag);
								final String targetFile = "C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\graphcutdata\\current-gc" + idx + ".jpg";
								if (!result.empty())
									imwrite(targetFile, result);
								else
									System.out.println("empty");

								Imgproc.rectangle(imag, obj.br(), obj.tl(), new Scalar(
										0, 255, 0), 2);
								Imgproc.circle(imag, pt, 1, new Scalar(0, 0, 255), 2);
							}
							++idx;
						}
					} else if (array.size() == 0) {
						tracker.updateKalman(imag, detections);
					}

					if (IS_DEVELOPMENT) {
						for (int k = 0; k < tracker.tracks.size(); k++) {
							int traceNum = tracker.tracks.get(k).trace.size();
							if (traceNum > 1) {
								for (int jt = 1; jt < tracker.tracks.get(k).trace
										.size(); jt++) {
									Imgproc.line(imag,
											tracker.tracks.get(k).trace.get(jt - 1),
											tracker.tracks.get(k).trace.get(jt),
											CONFIG.Colors[tracker.tracks.get(k).track_id % 9],
											2, 4, 0);
								}
							}
						}
					}
				}

				i = 1;

				if (CONFIG.DISPLAY_FRAMES) {
					ImageIcon image = new ImageIcon(Mat2bufferedImage(imag));
					vidpanel.setIcon(image);
					vidpanel.repaint();
					// temponFrame = outerBox.clone();

					ImageIcon image2 = new ImageIcon(Mat2bufferedImage(frame));
					vidpanel2.setIcon(image2);
					vidpanel2.repaint();

					ImageIcon image3 = new ImageIcon(Mat2bufferedImage(orgin));
					vidpanel3.setIcon(image3);
					vidpanel3.repaint();

					ImageIcon image4 = new ImageIcon(Mat2bufferedImage(kalman));
					vidpanel4.setIcon(image4);
					vidpanel4.repaint();
				}

				++frameNumber;
			}

			/**
			 * BB volume insert
			 */
			if(fidx == 0 && CONFIG.INITIALIZE_SCHEMA) {
				dbHelper.initializeBBSchema();
				dbHelper.initializeBBEntitites();
			}
			Vector<Float> rect_features = new Vector<>(Collections.<Float>nCopies(1000000, (float) -1));
			for (int ind = 0; ind < rect_volume.size(); ++ind)
				rect_features.set(ind, rect_volume.get(ind));
			String id = UUID.randomUUID().toString();

			if(CONFIG.DB_INSERT)
				dbHelper.insertBBToDb(id, fileName, rect_features);
			/**
			 * BB volume insert END
			 */


			/**
			 * Poly volume insert
			 */
			if(fidx == 0 && CONFIG.INITIALIZE_SCHEMA) {
				dbHelper.initializePolyVolumeSchema();
				dbHelper.initializePolyVolumeEntitites();
			}
			Vector<Float> polyvol_features = new Vector<>(Collections.<Float>nCopies(1000000, (float) -1));
			for (int ind = 0; ind < poly_volume.size(); ++ind)
				polyvol_features.set(ind, poly_volume.get(ind));

			if(CONFIG.DB_INSERT)
				dbHelper.insertPVToDb(id, fileName, polyvol_features);
			/**
			 * Poly volume insert END
			 */

			//perform KNN on vectors
			if(CONFIG.PERFORM_EVALUATION) {
				final CottontailGrpc.FloatVector.Builder bb_vol_vector = CottontailGrpc.FloatVector.newBuilder();
				bb_vol_vector.addAllVector(rect_features);
				final CottontailGrpc.FloatVector.Builder poly_vol_vector = CottontailGrpc.FloatVector.newBuilder();
				poly_vol_vector.addAllVector(polyvol_features);

				//BB
				File fbb = new File("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\evaluation\\" + fileName + "-BB.json");
				fbb.createNewFile();
				Files.write(fbb.toPath(), ("[\n").getBytes(), StandardOpenOption.APPEND);
				Iterator<CottontailGrpc.QueryResponseMessage> bbResults = dbHelper.executeNearestNeighborQuery(bb_vol_vector, "BB", "BB");
				bbResults.forEachRemaining(r -> r.getTuplesList().forEach(t -> {
					try {
						writeJSON(fileName, t.getData(1).getStringData(), "BB");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}));
				Files.write(fbb.toPath(), ("{ \"Query\": \"endQ\",\n" + "\"Result\": \"endR\"}\n]").getBytes(), StandardOpenOption.APPEND);
				//END BB

				File fpv = new File("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\evaluation\\" + fileName + "-PV.json");
				fpv.createNewFile();
				Files.write(fpv.toPath(), ("[\n").getBytes(), StandardOpenOption.APPEND);
				Iterator<CottontailGrpc.QueryResponseMessage> pvResults = dbHelper.executeNearestNeighborQuery(poly_vol_vector, "PV", "PV");
				pvResults.forEachRemaining(r -> r.getTuplesList().forEach(t -> {
					try {
						writeJSON(fileName, t.getData(1).getStringData(), "PV");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}));
				Files.write(fpv.toPath(), ("{ \"Query\": \"endQ\",\n" + "\"Result\": \"endR\"}\n]").getBytes(), StandardOpenOption.APPEND);
			}

			camera.release();
			volume_json.add("Polygons", polygons);
			volume_json.add("LastObject", lastObject);

			System.out.println(volume_json.toString());
		}
		System.out.println("============================ END ============================");
	}

	private static void writeJSON(String fileName, String data, String mode) throws IOException {
		if (mode == "BB") {
			Files.write(Paths.get("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\evaluation\\" + fileName + "-BB.json"), ("{ \"Query\": \"" + fileName + "\",\n" + "\"Result\": \"" + data + "\"},").getBytes(), StandardOpenOption.APPEND);
		} else {

			Files.write(Paths.get("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\evaluation\\" + fileName + "-PV.json"), ("{ \"Query\": \"" + fileName + "\",\n" + "\"Result\": \"" + data + "\"},").getBytes(), StandardOpenOption.APPEND);
		}
	}


	// background substractionMOG2
	protected static void processFrame(VideoCapture capture, Mat mRgba,
									   Mat mFGMask, BackgroundSubtractorMOG2 mBGSub) {
		// GREY_FRAME also works and exhibits better performance
		mBGSub.apply(mRgba, mFGMask, CONFIG.learningRate);
		Imgproc.cvtColor(mFGMask, mRgba, Imgproc.COLOR_GRAY2BGRA, 0);
		Mat erode = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(
				8, 8));
		Mat dilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
				new Size(8, 8));

		Mat openElem = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
				new Size(3, 3), new Point(1, 1));
		Mat closeElem = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
				new Size(7, 7), new Point(3, 3));

		Imgproc.threshold(mFGMask, mFGMask, 127, 255, Imgproc.THRESH_BINARY);
		Imgproc.morphologyEx(mFGMask, mFGMask, Imgproc.MORPH_OPEN, erode);
		Imgproc.morphologyEx(mFGMask, mFGMask, Imgproc.MORPH_OPEN, dilate);
		Imgproc.morphologyEx(mFGMask, mFGMask, Imgproc.MORPH_OPEN, openElem);
		Imgproc.morphologyEx(mFGMask, mFGMask, Imgproc.MORPH_CLOSE, closeElem);
	}

	private static BufferedImage Mat2bufferedImage(Mat image) {
		MatOfByte bytemat = new MatOfByte();
		Imgcodecs.imencode(".jpg", image, bytemat);
		byte[] bytes = bytemat.toArray();
		InputStream in = new ByteArrayInputStream(bytes);
		BufferedImage img = null;
		try {
			img = ImageIO.read(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return img;
	}

	public static double[][] Generate2DArray(Mat superpixels, int maxY, int maxX) {
		double[][] ret = new double[maxX][maxY];

		for (int i = 0; i < superpixels.rows(); ++i) {
			int x = (int) superpixels.get(i, 0)[0];
			int y = (int) superpixels.get(i, 0)[1];

			ret[x][y] = 255;
		}

		if(IS_DEVELOPMENT) {
			for (int i = 0; i < maxX; ++i)
				for (int j = 0; j < maxY; ++j)
					if (ret[i][j] != 0)
						System.out.println("i: " + i + " j: " + j + " val: " + ret[i][j]);
		}
		return ret;
	}

	public static Vector<Rect> detectContours(Mat outmat, int index, int frameNumber) {
		Mat v = new Mat();
		Mat vv = outmat.clone();
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(vv, contours, v, Imgproc.RETR_LIST,
				Imgproc.CHAIN_APPROX_SIMPLE);

		int maxAreaIdx = -1;
		Rect r = null;
		Vector<Rect> rect_array = new Vector<Rect>();
		//Vector<Float> rect_volume = new Vector<Float>();
		Volume volume = new Volume();
		for (int idx = 0; idx < contours.size(); idx++) {
			Mat contour = contours.get(idx);

			//CREATING POLYGONS
			double[][] _2dArray = Generate2DArray(contour, imag.rows(), imag.cols());
			ArrayList<org.vitrivr.cineast.core.mms.Helper.Point> points = new ArrayList<org.vitrivr.cineast.core.mms.Helper.Point>();
			for (int i = 0; i < imag.cols(); ++i) {
				for (int j = 0; j < imag.rows(); ++j) {
					if (_2dArray[i][j] != 0)
						points.add(new org.vitrivr.cineast.core.mms.Helper.Point(i, j));
				}
			}

			Mat mask = new Mat(imag.rows(), imag.cols(), CvType.CV_8UC1);

			List<org.vitrivr.cineast.core.mms.Helper.Point> convexHull = ConvexHull.makeHull(points);

			//var epsilon = 4;
			double epsilon = (points.size() / (3 * (points.size() / 4))) * 2;
			List<org.vitrivr.cineast.core.mms.Helper.Point> simplifiedPolygon = RamerDouglasPeucker.douglasPeucker(points, epsilon);
			//List<org.vitrivr.cineast.core.mms.Helper.Point> simplifiedPolygon = new ArrayList<org.vitrivr.cineast.core.mms.Helper.Point>();
			RamerDouglasPeucker.ramerDouglasPeucker(points, 1, simplifiedPolygon);
			//System.out.println("----------------Polygon with epsilon: " + epsilon +" ---------------------");
			for (org.vitrivr.cineast.core.mms.Helper.Point p : points) {
				Point3 p2f = new Point3();
				p2f.x = p.x;
				p2f.y = p.y;
				//System.out.println(p);
				mask.put((int) p.x, (int) p.y, 0);
			}
			//System.out.println("--------------------------------------------------------------------");

			//creating the voxel
			Voxel voxel = new Voxel(frameNumber, simplifiedPolygon);
			//add voxel to volume
			volume.addVoxel(voxel);

			//System.out.println(volume);

			if (IS_DEVELOPMENT) {
				boolean write = false;
				for (org.vitrivr.cineast.core.mms.Helper.Point p : points) {
					if (p.label == 0)
						write = true;
				}

				if (write) {
					System.out.println("Writing...");
					imwrite("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\graphcutdata\\contour" + index + ".jpg", mask);
				}
			}
			//END POLYGONS


			double contourarea = Imgproc.contourArea(contour);
			if (contourarea > CONFIG.MIN_BLOB_AREA && contourarea < CONFIG.MAX_BLOB_AREA) {
				// MIN_BLOB_AREA = contourarea;
				maxAreaIdx = idx;
				r = Imgproc.boundingRect(contours.get(maxAreaIdx));
				rect_array.add(r);

				/**
				 * Serialize JSON
				 * */
				if(!insertedFirstBB){
					JsonObject firstObject = new JsonObject();

					firstObject.addProperty("FrameNumber", frameNumber);
					firstObject.addProperty("Index", index);

					JsonObject firstBB = new JsonObject();
					//TODO: RECONCILE THE WAY POINTS ARE ADDED => TOO TIRED NOW
					JsonObject pointTopLeft= new JsonObject(); JsonObject pointTopRight= new JsonObject(); JsonObject pointBottomLeft= new JsonObject(); JsonObject pointBottomRight = new JsonObject();
					pointTopLeft.addProperty("X", r.tl().x); pointTopLeft.addProperty("Y",r.tl().y); firstBB.add("Point", pointTopLeft);
					pointTopRight.addProperty("X",r.tl().x + r.width); pointTopRight.addProperty("Y", r.tl().y); firstBB.add("Point", pointTopRight);
					pointBottomLeft.addProperty("X", r.tl().x); pointBottomLeft.addProperty("Y", r.tl().y + r.height); firstBB.add("Point", pointBottomLeft);
					pointBottomRight.addProperty("X",r.br().x); pointBottomRight.addProperty("Y", r.br().y); firstBB.add("Point", pointBottomRight);

					firstObject.add("Rect", firstBB);

					volume_json.add("FirstObject", firstObject);
					insertedFirstBB = true;
				}

				JsonObject volumeObject = new JsonObject();
				volumeObject.addProperty("FrameNumber", frameNumber);
				volumeObject.addProperty("Index", index);

				JsonObject polygonObject = new JsonObject();
				for(org.vitrivr.cineast.core.mms.Helper.Point p : simplifiedPolygon){
					JsonObject point = new JsonObject();
					point.addProperty("X", p.x);
					point.addProperty("Y", p.y);

					polygonObject.add("Point", point);
				}
				polygons.add("Polygon", polygons);


				lastObject = new JsonObject();
				lastObject.addProperty("FrameNumber", frameNumber);
				lastObject.addProperty("Index", index);

				JsonObject firstBB = new JsonObject();
				//TODO: RECONCILE THE WAY POINTS ARE ADDED => TOO TIRED NOW
				JsonObject pointTopLeft= new JsonObject(); JsonObject pointTopRight= new JsonObject(); JsonObject pointBottomLeft= new JsonObject(); JsonObject pointBottomRight = new JsonObject();
				pointTopLeft.addProperty("X", r.tl().x); pointTopLeft.addProperty("Y",r.tl().y); firstBB.add("Point", pointTopLeft);
				pointTopRight.addProperty("X",r.tl().x + r.width); pointTopRight.addProperty("Y", r.tl().y); firstBB.add("Point", pointTopRight);
				pointBottomLeft.addProperty("X", r.tl().x); pointBottomLeft.addProperty("Y", r.tl().y + r.height); firstBB.add("Point", pointBottomLeft);
				pointBottomRight.addProperty("X",r.br().x); pointBottomRight.addProperty("Y", r.br().y); firstBB.add("Point", pointBottomRight);

				lastObject.add("Rect", firstBB);
				/**
				 * END Serialize JSON
				 * */

				/**
				 * POLYGON VOLUME BELOW (DB)
				 * */
				if(CONFIG.DO_POLY_OPERATIONS)
					poly_volume.add((float)Integer.MIN_VALUE);

				poly_volume.add((float)frameNumber);
				poly_volume.add((float)index);
				for(org.vitrivr.cineast.core.mms.Helper.Point p : simplifiedPolygon){
					poly_volume.add((float)p.x);
					poly_volume.add((float)p.y);
				}

				/**
				 * POLYGON VOLUME END (DB)
				 * */


				/**
				 * BB VOLUME BELOW (DB)
				 * */
				//start new volume by adding min value
				if(CONFIG.DO_POLY_OPERATIONS)
					rect_volume.add((float)Integer.MIN_VALUE);

				rect_volume.add((float)frameNumber);
				rect_volume.add((float)index);

				//TODO: RECONCILE THE WAY POINTS ARE ADDED => TOO TIRED NOW
				rect_volume.add((float)r.tl().x); rect_volume.add((float)r.tl().y);
				rect_volume.add((float)r.tl().x + r.width); rect_volume.add((float)r.tl().y);
				rect_volume.add((float)r.tl().x); rect_volume.add((float)r.tl().y + r.height);
				rect_volume.add((float)r.br().x); rect_volume.add((float)r.br().y);
				//System.out.println(rect_volume);
				/**
				 * BB VOLUME END (DB)
				 * */
				Imgproc.drawContours(imag, contours, maxAreaIdx, new Scalar(255, 0, 255));

				//TODO: DO THE GRABCUT GIVEN THE RECTANGLES

				//TODO: DO THE SUPERPIXEL SEGMENTATION

				//TODO: POLY ALGOS

				//TODO: RETRUN POLYGONS

			}
		}

		if(!IS_DEVELOPMENT) {

			//id
			UUID id = UUID.randomUUID();

			//TODO: SAVE THE POLY TO DB, THEN RETRIEVE POLY AND DO THE BOOLEANS ON IT => LIKE SO:
			//using polybool
			Epsilon eps = new Epsilon();
			List<List<double[]>> regions = new ArrayList<List<double[]>>();

			List<double[]> region;
			for (int vi = 0; vi < volume.getVolume().size(); ++vi) {
				region = new ArrayList<double[]>();
				Voxel vox = volume.getVolume().get(vi);
				for (int pi = 0; pi < vox.getPolygon().size(); ++pi) {
					org.vitrivr.cineast.core.mms.Helper.Point p = vox.getPolygon().get(pi);
					region.add(point(p.x, p.y));
				}
				regions.add(region);
			}

			Polygon pol = new Polygon(regions);

			//System.out.println(pol);
			//Db.dropSchema();
			//initializeSchema();
			//insertPolyData(pol, frameNumber);
		}

		v.release();
		return rect_array;
	}
}

