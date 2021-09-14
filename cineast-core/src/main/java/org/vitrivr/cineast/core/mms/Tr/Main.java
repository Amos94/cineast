package org.vitrivr.cineast.core.mms.Tr;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.vitrivr.cineast.core.mms.Algorithms.Polygons.Algos.Epsilon;
import org.vitrivr.cineast.core.mms.Algorithms.Polygons.Algos.PolyBool;
import org.vitrivr.cineast.core.mms.Algorithms.Polygons.Algos.models.Polygon;
import org.vitrivr.cineast.core.mms.Algorithms.Polygons.RamerDouglasPeucker;
import org.vitrivr.cineast.core.mms.Helper.ConvexHull;
import org.vitrivr.cineast.core.mms.Helper.Volume;
import org.vitrivr.cineast.core.mms.Helper.Voxel;
import org.vitrivr.cineast.core.mms.OcrPreprocessing.OpenCVPreProcessing;
import org.vitrivr.cineast.core.mms.OcrProcessing.TextExtractor;
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
import static org.vitrivr.cineast.core.mms.Algorithms.Polygons.Algos.helpers.PolyBoolHelper.epsilon;
import static org.vitrivr.cineast.core.mms.Algorithms.Polygons.Algos.helpers.PolyBoolHelper.point;
import static org.vitrivr.cineast.core.mms.Tr.CONFIG.*;

public class Main {
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		System.loadLibrary("opencv_java452");
		// System.loadLibrary("opencv_java2410");
	}
	//TODO serialize data in JSON

	/** Cottontail DB gRPC channel; adjust Cottontail DB host and port according to your needs. */
	private static final ManagedChannel CHANNEL  = ManagedChannelBuilder.forAddress("127.0.0.1", 1865).usePlaintext().maxInboundMessageSize(9999999).build();

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
	private static JsonObject volume_json;
	private static JsonArray volumeElements;
	private static DatabaseHelper dbHelper;

	private static HashMap<Triple<List<Triple<Polygon, Polygon, Double>>, String, String>, Double> evalMap;
	private static HashMap<String, String> resultsJson;


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
		Stream<Path> benchmarkVids = Files.walk(Paths.get("cineast-core/src/main/java/org/vitrivr/cineast/core/mms/Data/benchmark"));
		files.addAll(benchmarkVids.filter(Files::isRegularFile).collect(Collectors.toList()));

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


			if(CONFIG.JSON && CONFIG.PERFORM_EVALUATION){
				resultsJson = new HashMap<>();
				if(CONFIG.PERFORM_EVALUATION_WITH_DB)
					fetchSelectData();
				else
					fetchDiskData();
			}
		}

		for(int fidx=0; fidx<files.size(); ++fidx) {
			String[] fpath = files.get(fidx).toString().split("\\\\");
			Collections.reverse(Arrays.asList(fpath));
			String fileName = fpath[0];
			boolean skip = !(fileName.contains(".mp4") ||filename.contains(".avi"));
			if(skip) //&& !filename.contains(".mp4"))
				continue;

			System.out.println(">>> PROCESSING: " + fileName);

			if(CONFIG.DB_INSERT)
				Files.write(Paths.get("C:/DEV/cineast/cineast-core/src/main/java/org/vitrivr/cineast/core/mms/Data/status.txt"), ("processing " + fileName + "\n").getBytes(), StandardOpenOption.APPEND);


			volume_json = new JsonObject();
			volumeElements = new JsonArray();
			evalMap = new HashMap<>();

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
			int maxF = CONFIG.EVALUATION_MAXIMUM_STACK_FRAMES;

			if (!camera.isOpened()) {
				System.out.print("Can not open Camera, try it later.");
				return;
			}
			int idx = 0;
			int index = 0;
			int frameNumber = 0;
			while (true) {

				if(PERFORM_EVALUATION_WITH_DB) {
					if (maxF <= 0) {
						System.out.println("> "+CONFIG.EVALUATION_MAXIMUM_STACK_FRAMES+" frames");
						break;
					}
					--maxF;
				}

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

					array = detectContours(diffFrame, index, frameNumber, frame);
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



			camera.release();
			volume_json.add("Elements", volumeElements);

			if(CONFIG.JSON && CONFIG.DB_INSERT){
				/**
				 * JSON volume insert
				 */
				if (fidx == 0 && CONFIG.INITIALIZE_SCHEMA) {
					dbHelper.initializeJSONSchema();
					dbHelper.initializeJSONEntitites();
				}

				String id = UUID.randomUUID().toString();
				if(false) {
					boolean isSuccess = dbHelper.insertJSONToDb(id, fileName, volume_json.toString());
				}


				if(false){//!isSuccess) {
					List<String> ff = Files.readAllLines(Paths.get("C:/DEV/cineast/cineast-core/src/main/java/org/vitrivr/cineast/core/mms/Data/status.txt"));
					if(ff.get(ff.size()-1) == "")
						ff.remove(ff.size()-1);
					ff.remove(ff.size()-1);
					Files.delete(Paths.get("C:/DEV/cineast/cineast-core/src/main/java/org/vitrivr/cineast/core/mms/Data/status.txt"));
					Files.createFile(Paths.get("C:/DEV/cineast/cineast-core/src/main/java/org/vitrivr/cineast/core/mms/Data/status.txt"));

					for(int fi = 0; fi<ff.size(); ++fi)
						Files.write(Paths.get("C:/DEV/cineast/cineast-core/src/main/java/org/vitrivr/cineast/core/mms/Data/status.txt"), ("processing " + ff.get(fi) + "\n").getBytes(), StandardOpenOption.APPEND);
				}

				Files.write(Paths.get("C:/DEV/cineast/cineast-core/src/main/java/org/vitrivr/cineast/core/mms/Data/evaluation/"+fileName+".json"), (volume_json.toString()).getBytes(), StandardOpenOption.CREATE_NEW);

			}

			if(PERFORM_EVALUATION_WITH_DB && CONFIG.JSON) {
				//JSON VOL
				File fbb = new File("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\evaluation\\" + fileName + "-PVJ.json");
				fbb.createNewFile();
				Files.write(fbb.toPath(), ("[\n").getBytes(), StandardOpenOption.APPEND);

				for (Map.Entry<String, String> entry : resultsJson.entrySet()) {
					String key = entry.getKey();
					String value = entry.getValue();
					performEvaluationWithJSON(volume_json.toString(), value, fileName, key);
				}
				ArrayList<String> top10 = fetchTopXScores(evalMap, 10);

				for(String entry : top10) {
					Files.write(fbb.toPath(), ("{ \"Query\":" + fileName + ",\n" + "\"Result\": " + entry + "}\n]").getBytes(), StandardOpenOption.APPEND);
				}

				Files.write(fbb.toPath(), ("{ \"Query\": \"endQ\",\n" + "\"Result\": \"endR\"}\n]").getBytes(), StandardOpenOption.APPEND);
				if(top10.size() == 0)
					Files.delete(Paths.get("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\evaluation\\" + fileName + "-PVJ.json"));
				//END JSON VOL
			}

			if(CONFIG.PERFORM_EVALUATION && CONFIG.JSON){
				//JSON VOL
				File fbb = new File("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\evaluation\\" + fileName + "-PVJ.json");
				fbb.createNewFile();
				Files.write(fbb.toPath(), ("[\n").getBytes(), StandardOpenOption.APPEND);

				for (Map.Entry<String, String> entry : resultsJson.entrySet()) {
					String key = entry.getKey();
					String value = entry.getValue();
					performEvaluationWithJSON(volume_json.toString(), value, fileName, key);
				}
				ArrayList<String> top10 = fetchTopXScores(evalMap, 10);

				for(String entry : top10) {
					Files.write(fbb.toPath(), ("{ \"Query\":" + fileName + ",\n" + "\"Result\": " + entry + "}\n]").getBytes(), StandardOpenOption.APPEND);
				}

				Files.write(fbb.toPath(), ("{ \"Query\": \"endQ\",\n" + "\"Result\": \"endR\"}\n]").getBytes(), StandardOpenOption.APPEND);
				if(top10.size() == 0)
					Files.delete(Paths.get("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\evaluation\\" + fileName + "-PVJ.json"));
				//END JSON VOL
			}

			//System.out.println(volume_json.toString());
		}
		System.out.println("============================ END ============================");
	}

	private static void fetchSelectData() {
		Iterator<CottontailGrpc.QueryResponseMessage> results = dbHelper.executeSimpleSelect("PVJ", "PVJ");
		results.forEachRemaining(r -> r.getTuplesList().forEach(t -> {
			try {
				resultsJson.put(t.getData(1).getStringData(), t.getData(2).getStringData());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));
	}

	private static void fetchDiskData() {
		Iterator it = FileUtils.iterateFiles(new File("cineast-core/src/main/java/org/vitrivr/cineast/core/mms/Data/results"), new String[]{"json"}, false);
		while(it.hasNext()){
			String fname = ((File) it.next()).getName().replace(".json", "");

			try {
				File f = new File(((File) it.next()).getPath());
				Scanner reader = new Scanner(f);
				StringBuffer data = new StringBuffer();
				while (reader.hasNextLine()) {
					data.append(reader.nextLine());
				}
				resultsJson.put(fname, data.toString());
				reader.close();
			} catch (Exception e) {
			}

		}
	}

	private static void writeJSON(String fileName, String data, String mode) throws IOException {
		if (mode == "BB") {
			Files.write(Paths.get("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\evaluation\\" + fileName + "-BB.json"), ("{ \"Query\": \"" + fileName + "\",\n" + "\"Result\": \"" + data + "\"},").getBytes(), StandardOpenOption.APPEND);
		} else if(mode == "PV"){

			Files.write(Paths.get("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\evaluation\\" + fileName + "-PV.json"), ("{ \"Query\": \"" + fileName + "\",\n" + "\"Result\": \"" + data + "\"},").getBytes(), StandardOpenOption.APPEND);
		}
		else{
			Files.write(Paths.get("C:\\DEV\\cineast\\cineast-core\\src\\main\\java\\org\\vitrivr\\cineast\\core\\mms\\Data\\evaluation\\" + fileName + "-PVJ.json"), ("{ \"Query\": \"" + fileName + "\",\n" + "\"Result\": \"" + data + "\"},").getBytes(), StandardOpenOption.APPEND);

		}
	}

	private static void performEvaluationWithJSON(String queryJson, String resultJson, String queryFileName, String resultFileName){
		List<Triple<Polygon, Polygon, Double>> queryElements = new ArrayList<Triple<Polygon, Polygon, Double>>();
		List<Triple<Polygon, Polygon, Double>> resultElements = new ArrayList<Triple<Polygon, Polygon, Double>>();

		JsonObject queryObjJson = new JsonParser().parse(queryJson).getAsJsonObject();
		JsonObject resultObjJson = new JsonParser().parse(resultJson).getAsJsonObject();

		JsonArray queryElementsJson = queryObjJson.getAsJsonArray("Elements");
		JsonArray resultElementsJson = resultObjJson.getAsJsonArray("Elements");

		for(int i = 0; i<queryElementsJson.size(); ++i)
		{
			JsonObject qo = queryElementsJson.get(i).getAsJsonObject();
			JsonArray qpoly = qo.getAsJsonArray("Polygon");
			JsonArray qRect = qo.getAsJsonArray("Rect");
			JsonElement qAreaJson = qo.get("Area");
			double qArea = (double) qAreaJson.getAsFloat();
			Polygon polygon =  transformJsonToPolygon(qpoly);
			Polygon rect =  transformJsonToPolygon(qRect);
			queryElements.add(new Triple<Polygon, Polygon, Double>() {
				@Override
				public Polygon getLeft() {
					return polygon;
				}

				@Override
				public Polygon getMiddle() {
					return rect;
				}

				@Override
				public Double getRight() {
					return qArea;
				}
			});
		}

		for(int i = 0; i<resultElementsJson.size(); ++i)
		{
			JsonObject ro = resultElementsJson.get(i).getAsJsonObject();
			JsonArray rpoly = ro.getAsJsonArray("Polygon");
			JsonArray rRect = ro.getAsJsonArray("Rect");
			JsonElement rAreaJson = ro.get("Area");
			double rArea = (double) rAreaJson.getAsFloat();
			Polygon polygon =  transformJsonToPolygon(rpoly);
			Polygon rect =  transformJsonToPolygon(rRect);
			resultElements.add(new Triple<Polygon, Polygon, Double>() {
				@Override
				public Polygon getLeft() {
					return polygon;
				}

				@Override
				public Polygon getMiddle() {
					return rect;
				}

				@Override
				public Double getRight() {
					return rArea;
				}
			});
		}

		double similarity = calculateJaccardIndex(queryElements, resultElements); //TODO: add also the filename

		evalMap.put(new Triple<List<Triple<Polygon, Polygon, Double>>, String, String>() {
			@Override
			public List<Triple<Polygon, Polygon, Double>> getLeft() {
				return resultElements;
			}

			@Override
			public String getMiddle() {
				return queryFileName;
			}

			@Override
			public String getRight() {
				return resultFileName;
			}
		}, similarity);
	}

	private static ArrayList<String> fetchTopXScores(HashMap<Triple<List<Triple<Polygon, Polygon, Double>>, String, String>, Double> evalMap, int x) {
		ArrayList<String> results = new ArrayList<>();

		HashMap<Triple<List<Triple<Polygon, Polygon, Double>>, String, String>, Double> resultMap = new HashMap<>();
		evalMap.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.forEachOrdered(v -> resultMap.put(v.getKey(), v.getValue()));

		ArrayList<Double> topXScores = new ArrayList<Double>();

		int curidx = 0;
		for (Map.Entry<Triple<List<Triple<Polygon, Polygon, Double>>, String, String>, Double> entry : resultMap.entrySet()) {
			if(entry.getValue() < CONFIG.JACCARDACCEPTANCESCORE)
				continue;
			if(curidx == x-1)
				break;
			if(!topXScores.contains(entry.getValue()) && entry.getValue() != 0){
				topXScores.add(entry.getValue());
				++curidx;
			}
		}

		for(Map.Entry<Triple<List<Triple<Polygon, Polygon, Double>>, String, String>, Double> entry : resultMap.entrySet()){
			if(entry.getValue() < CONFIG.JACCARDACCEPTANCESCORE)
				continue;
			if(topXScores.contains(entry.getValue())) {
				results.add(entry.getKey().getRight());
			}
		}

		return results;
	}

	private static Polygon transformJsonToPolygon(JsonArray polyJson){
		Polygon polygon = new Polygon();
		List<List<double[]>> regions = new ArrayList<>();
		List<double[]> points = new ArrayList<>();
		for(int i= 0; i<polyJson.size(); ++i){
			JsonObject point = polyJson.get(i).getAsJsonObject();
			points.add(new double[]{(double) point.get("X").getAsFloat(), (double) point.get("Y").getAsFloat()});
			regions.add(points);
		}
		polygon.setRegions(regions);
		return polygon;
	}

	private static double similarity(List<Polygon> queryPolygons, List<Polygon> resultPolygons){

		double count = 0; int maxSize = -1;
		for(int i=0; i<queryPolygons.size(); ++i){
			for(int j=0; j<resultPolygons.size(); ++j){
				if(PolyEqual(queryPolygons.get(i),resultPolygons.get(j)))
					++count;
			}
		}

		if(queryPolygons.size() >= resultPolygons.size())
			maxSize = queryPolygons.size();
		else
			maxSize = resultPolygons.size();

		return count/maxSize;
	}

	private static double calculateJaccardIndex(List<Triple<Polygon, Polygon, Double>> queryPolygons, List<Triple<Polygon, Polygon, Double>> resultPolygons){
		double jaccardIndex = 0.0;

		HashSet<Triple<Polygon, Polygon, Double>> union = new HashSet<>();
		union.addAll(queryPolygons);
		union.addAll(resultPolygons);

		HashSet<Polygon> intersection = new HashSet<>();
		for(int i=0; i<queryPolygons.size(); ++i){
			for(int j=0; j<resultPolygons.size(); ++j){
				//boolean areasNotEqual = queryPolygons.get(i).getRight().equals(resultPolygons.get(i).getRight());
				boolean bbNotIntersect = !doRectsIntersect(queryPolygons.get(i).getMiddle(),resultPolygons.get(j).getMiddle());
				if(bbNotIntersect)// || areasNotEqual)
					continue;
				if(PolyEqual(queryPolygons.get(i).getLeft(),resultPolygons.get(j).getLeft()))
					intersection.add(queryPolygons.get(i).getLeft());
			}
		}

		jaccardIndex = intersection.size() / union.size();

		return jaccardIndex;

	}

	private static boolean doRectsIntersect(Polygon qr, Polygon rr){
		Epsilon eps = epsilon();
		List<List<double[]>> reg = PolyBool.intersect(eps,qr, rr).getRegions();

		if(reg.size() != 0)
			System.out.println("INTERSECTION FOUND!");
		return reg.size() != 0;
	}

	private static boolean PolyEqual(Polygon qp, Polygon rp){
		List<List<double[]>> qpRegions = qp.getRegions();
		List<List<double[]>> rpRegions = rp.getRegions();

		Set<List<double[]>> qpset = new HashSet<>(qpRegions);
		qpRegions.clear();
		qpRegions.addAll(qpset);

		Set<List<double[]>> rpset = new HashSet<>(rpRegions);
		rpRegions.clear();
		rpRegions.addAll(rpset);

		return equalRegions(qpRegions, rpRegions);
	}

	public static boolean equalRegions(List<List<double[]>> one, List<List<double[]>> two){
		if (one == null && two == null){
			return true;
		}

		if((one == null && two != null)
				|| one != null && two == null
				|| one.size() != two.size()){
			return false;
		}

		for(List<double[]> entryOne : one)
			for(List<double[]> entryTwo : two)
				if(EqualLists(entryOne, entryTwo))
					return true;
		return false;
	}

	public static boolean EqualLists(List<double[]> one, List<double[]> two){
		if (one == null && two == null){
			return true;
		}

		if((one == null && two != null)
				|| one != null && two == null
				|| one.size() != two.size()){
			return false;
		}

		boolean left = checkContents(one, two); boolean right = checkContents(two, one);
		return left && right;
	}

	public static boolean checkContents(List<double[]> one, List<double[]> two){
		List<double[]> cloneOne = new ArrayList<>();
		List<double[]> cloneTwo = new ArrayList<>();

		cloneOne.addAll(one);
		cloneTwo.addAll(two);

		for(double[] oneEntry : one)
			for(double[] twoEntry : two)
				if(EqualArrays(oneEntry, twoEntry))
					cloneOne.remove(oneEntry);

		return cloneOne.size() == 0;
	}

	public static boolean EqualArrays(double[] one, double[] two){
		boolean result = (one[0] == two[0] && one[1] == two[1]);
		return result;
	}

	//TODO: check "gesture" similarity concept


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

	public static Vector<Rect> detectContours(Mat outmat, int index, int frameNumber, Mat originalFrame) {
		//Tex extraction
		JsonArray texts = new JsonArray();

		if(INCLUDE_TEXT) {
			Mat cpy = originalFrame.clone();
			Mat cpyy = OpenCVPreProcessing.doTextExtractionPreProcessing(cpy);
			TextExtractor te = new TextExtractor();
			List<Rect> textRects = te.getTextRects(cpyy);


			for (Rect tr : textRects) {
				JsonArray textRect = new JsonArray();
				//TODO: RECONCILE THE WAY POINTS ARE ADDED => TOO TIRED NOW
				JsonObject pointTopLeft = new JsonObject();
				JsonObject pointTopRight = new JsonObject();
				JsonObject pointBottomLeft = new JsonObject();
				JsonObject pointBottomRight = new JsonObject();
				pointTopLeft.addProperty("X", tr.tl().x);
				pointTopLeft.addProperty("Y", tr.tl().y);
				textRect.add(pointTopLeft);
				pointTopRight.addProperty("X", tr.tl().x + tr.width);
				pointTopRight.addProperty("Y", tr.tl().y);
				textRect.add(pointTopRight);
				pointBottomLeft.addProperty("X", tr.tl().x);
				pointBottomLeft.addProperty("Y", tr.tl().y + tr.height);
				textRect.add(pointBottomLeft);
				pointBottomRight.addProperty("X", tr.br().x);
				pointBottomRight.addProperty("Y", tr.br().y);
				textRect.add(pointBottomRight);
				texts.add(textRect);
			}
		}
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
				JsonArray bb = new JsonArray();
				//TODO: RECONCILE THE WAY POINTS ARE ADDED => TOO TIRED NOW
				JsonObject pointTopLeft= new JsonObject(); JsonObject pointTopRight= new JsonObject(); JsonObject pointBottomLeft= new JsonObject(); JsonObject pointBottomRight = new JsonObject();
				pointTopLeft.addProperty("X", r.tl().x); pointTopLeft.addProperty("Y",r.tl().y); bb.add(pointTopLeft);
				pointTopRight.addProperty("X",r.tl().x + r.width); pointTopRight.addProperty("Y", r.tl().y); bb.add(pointTopRight);
				pointBottomLeft.addProperty("X", r.tl().x); pointBottomLeft.addProperty("Y", r.tl().y + r.height); bb.add(pointBottomLeft);
				pointBottomRight.addProperty("X",r.br().x); pointBottomRight.addProperty("Y", r.br().y); bb.add(pointBottomRight);

				JsonArray polygonArray = new JsonArray();
				for(org.vitrivr.cineast.core.mms.Helper.Point p : simplifiedPolygon){
					JsonObject point = new JsonObject();
					point.addProperty("X", p.x);
					point.addProperty("Y", p.y);

					polygonArray.add(point);
				}
				JsonObject element = new JsonObject();
				element.addProperty("FrameNumber", frameNumber);
				element.addProperty("Index", index);
				element.addProperty("Area", contourarea);
				element.add("Polygon", polygonArray);
				element.add("Rect", bb);
				if(CONFIG.INCLUDE_TEXT)
					element.add("Texts", texts);
				volumeElements.add(element);

				/**
				 * END Serialize JSON
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

