package org.vitrivr.cineast.core.mms.Tr;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.javatuples.Quartet;
import org.vitrivr.cottontail.grpc.*;

import java.util.*;

import static org.vitrivr.cineast.core.mms.Tr.CONFIG.IS_DEVELOPMENT;

public class DatabaseHelper {
    /**
     * Cottontail DB gRPC channel; adjust Cottontail DB host and port according to your needs.
     */
    private static final ManagedChannel CHANNEL = ManagedChannelBuilder.forAddress("127.0.0.1", 1865).usePlaintext().build();

    /**
     * Cottontail DB Stub for DDL operations (e.g. create a new Schema or Entity).
     */
    private static final DDLGrpc.DDLBlockingStub DDL_SERVICE = DDLGrpc.newBlockingStub(CHANNEL);

    /**
     * Cottontail DB Stub for DML operations (i.e. inserting Data).
     */
    private static final DMLGrpc.DMLBlockingStub DML_SERVICE = DMLGrpc.newBlockingStub(CHANNEL);

    /**
     * Cottontail DB Stub for DQL operations (i.e. issuing queries).
     */
    private static final DQLGrpc.DQLBlockingStub DQL_SERVICE = DQLGrpc.newBlockingStub(CHANNEL);

    /**
     * Cottontail DB Stub for Transaction management.
     */
    private static final TXNGrpc.TXNBlockingStub TXN_SERVICE = TXNGrpc.newBlockingStub(CHANNEL);

    /**
     * Name of the Cottontail DB Schema.
     */
    private static String SCHEMA_NAME = "segmentation";

    public DatabaseHelper() { }

	public DatabaseHelper(String schemaName){
    	SCHEMA_NAME = schemaName;
	}


	public static List<Quartet<String, Integer, Integer, List<Float>>> insertPolyData(org.vitrivr.cineast.core.mms.Algorithms.Polygons.Algos.models.Polygon pol, int frame){

		List<Quartet<String, Integer, Integer, List<Float>>> dbStructuredPolygon = new ArrayList<Quartet<String, Integer, Integer, List<Float>>>();

		for(int i=0; i<pol.getRegions().size(); ++i){
			String guid = UUID.randomUUID().toString();
			int regionId = i;
			List<Float> points = new ArrayList<Float>(Collections.<Float>nCopies(500, (float)-1));
			List<double[]> region = pol.getRegions().get(i);

			int k=0;
			for(int j = 0; j<region.size(); ++j){
				points.set(k, (float) region.get(j)[0]);
				++k;
				points.set(k, (float) region.get(j)[1]);
				++k;
			}
			dbStructuredPolygon.add(new Quartet(guid, frame, regionId, points));
			insertToDb(guid, frame, regionId,  points);
		}

		if(!IS_DEVELOPMENT) {
			System.out.println(polyDataToString(dbStructuredPolygon));
		}

		return dbStructuredPolygon;
	}

	public static void initializeSchema(){
		final CottontailGrpc.CreateSchemaMessage schemaDefinitionMessage = CottontailGrpc.CreateSchemaMessage.
				newBuilder().
				setSchema(CottontailGrpc.SchemaName.newBuilder().setName(SCHEMA_NAME)).build();
		DDL_SERVICE.createSchema(schemaDefinitionMessage);
		System.out.println("Schema '" + SCHEMA_NAME + "' created successfully.");
	}

	public static void initializeBBSchema(){
		final CottontailGrpc.CreateSchemaMessage schemaDefinitionMessage = CottontailGrpc.CreateSchemaMessage.
				newBuilder().
				setSchema(CottontailGrpc.SchemaName.newBuilder().setName("BB")).build();
		DDL_SERVICE.createSchema(schemaDefinitionMessage);
		System.out.println("Schema '" + "BB" + "' created successfully.");
	}
	public static void initializeBBEntitites(){
		final CottontailGrpc.TransactionId txId = TXN_SERVICE.begin(Empty.getDefaultInstance());
		final CottontailGrpc.EntityDefinition definition = CottontailGrpc.EntityDefinition.newBuilder()
				.setEntity(CottontailGrpc.EntityName.newBuilder().setName("BB").setSchema(CottontailGrpc.SchemaName.newBuilder().setName("BB"))) /* Name of entity and schema it belongs to. */
				.addColumns(CottontailGrpc.ColumnDefinition.newBuilder().setType(CottontailGrpc.Type.STRING).setName("id").setEngine(CottontailGrpc.Engine.MAPDB).setNullable(false)) /* 1st column: id (String) */
				.addColumns(CottontailGrpc.ColumnDefinition.newBuilder().setType(CottontailGrpc.Type.STRING).setName("filename").setEngine(CottontailGrpc.Engine.MAPDB).setNullable(false)) /* 1st column: id (String) */
				.addColumns(CottontailGrpc.ColumnDefinition.newBuilder().setType(CottontailGrpc.Type.FLOAT_VEC).setName("features").setEngine(CottontailGrpc.Engine.MAPDB).setNullable(false).setLength(1000000))  /* 4th column poly vector*/
				.build();

		DDL_SERVICE.createEntity(CottontailGrpc.CreateEntityMessage.newBuilder().setTxId(txId).setDefinition(definition).build());

		TXN_SERVICE.commit(txId);
		System.out.println("Entity '" + "BB" + "." + "BB" + "' created successfully.");
	}

	public static void initializePolyEntitites(){
		final CottontailGrpc.TransactionId txId = TXN_SERVICE.begin(Empty.getDefaultInstance());
		final CottontailGrpc.EntityDefinition definition = CottontailGrpc.EntityDefinition.newBuilder()
				.setEntity(CottontailGrpc.EntityName.newBuilder().setName("poly").setSchema(CottontailGrpc.SchemaName.newBuilder().setName(SCHEMA_NAME))) /* Name of entity and schema it belongs to. */
				.addColumns(CottontailGrpc.ColumnDefinition.newBuilder().setType(CottontailGrpc.Type.STRING).setName("id").setEngine(CottontailGrpc.Engine.MAPDB).setNullable(false)) /* 1st column: id (String) */
				.addColumns(CottontailGrpc.ColumnDefinition.newBuilder().setType(CottontailGrpc.Type.INTEGER).setName("frame").setEngine(CottontailGrpc.Engine.MAPDB).setNullable(false)) /* 2nd column frame number*/
				.addColumns(CottontailGrpc.ColumnDefinition.newBuilder().setType(CottontailGrpc.Type.INTEGER).setName("region").setEngine(CottontailGrpc.Engine.MAPDB).setNullable(false)) /* 3rd column region index*/
				.addColumns(CottontailGrpc.ColumnDefinition.newBuilder().setType(CottontailGrpc.Type.FLOAT_VEC).setName("points").setEngine(CottontailGrpc.Engine.MAPDB).setNullable(false).setLength(500))  /* 4th column poly vector*/
				.build();

		DDL_SERVICE.createEntity(CottontailGrpc.CreateEntityMessage.newBuilder().setTxId(txId).setDefinition(definition).build());

		TXN_SERVICE.commit(txId);
		System.out.println("Entity '" + SCHEMA_NAME + "." + "poly" + "' created successfully.");
	}
	public static void initializePolyVolumeSchema(){
		final CottontailGrpc.CreateSchemaMessage schemaDefinitionMessage = CottontailGrpc.CreateSchemaMessage.
				newBuilder().
				setSchema(CottontailGrpc.SchemaName.newBuilder().setName("PV")).build();
		DDL_SERVICE.createSchema(schemaDefinitionMessage);
		System.out.println("Schema '" + "PV" + "' created successfully.");
	}
	public static void initializePolyVolumeEntitites(){
		final CottontailGrpc.TransactionId txId = TXN_SERVICE.begin(Empty.getDefaultInstance());
		final CottontailGrpc.EntityDefinition definition = CottontailGrpc.EntityDefinition.newBuilder()
				.setEntity(CottontailGrpc.EntityName.newBuilder().setName("PV").setSchema(CottontailGrpc.SchemaName.newBuilder().setName("PV"))) /* Name of entity and schema it belongs to. */
				.addColumns(CottontailGrpc.ColumnDefinition.newBuilder().setType(CottontailGrpc.Type.STRING).setName("id").setEngine(CottontailGrpc.Engine.MAPDB).setNullable(false)) /* 1st column: id (String) */
				.addColumns(CottontailGrpc.ColumnDefinition.newBuilder().setType(CottontailGrpc.Type.STRING).setName("filename").setEngine(CottontailGrpc.Engine.MAPDB).setNullable(false)) /* 1st column: id (String) */
				.addColumns(CottontailGrpc.ColumnDefinition.newBuilder().setType(CottontailGrpc.Type.FLOAT_VEC).setName("features").setEngine(CottontailGrpc.Engine.MAPDB).setNullable(false).setLength(1000000))  /* 4th column poly vector*/
				.build();

		DDL_SERVICE.createEntity(CottontailGrpc.CreateEntityMessage.newBuilder().setTxId(txId).setDefinition(definition).build());

		TXN_SERVICE.commit(txId);
		System.out.println("Entity '" + SCHEMA_NAME + "." + "poly" + "' created successfully.");
	}

	public static void initializeJSONSchema(){
		final CottontailGrpc.CreateSchemaMessage schemaDefinitionMessage = CottontailGrpc.CreateSchemaMessage.
				newBuilder().
				setSchema(CottontailGrpc.SchemaName.newBuilder().setName("PVJ")).build();
		DDL_SERVICE.createSchema(schemaDefinitionMessage);
		System.out.println("Schema '" + "PVJ" + "' created successfully.");
	}
	public static void initializeJSONEntitites(){
		final CottontailGrpc.TransactionId txId = TXN_SERVICE.begin(Empty.getDefaultInstance());
		final CottontailGrpc.EntityDefinition definition = CottontailGrpc.EntityDefinition.newBuilder()
				.setEntity(CottontailGrpc.EntityName.newBuilder().setName("PVJ").setSchema(CottontailGrpc.SchemaName.newBuilder().setName("PVJ"))) /* Name of entity and schema it belongs to. */
				.addColumns(CottontailGrpc.ColumnDefinition.newBuilder().setType(CottontailGrpc.Type.STRING).setName("id").setEngine(CottontailGrpc.Engine.MAPDB).setNullable(false)) /* 1st column: id (String) */
				.addColumns(CottontailGrpc.ColumnDefinition.newBuilder().setType(CottontailGrpc.Type.STRING).setName("filename").setEngine(CottontailGrpc.Engine.MAPDB).setNullable(false)) /* 1st column: id (String) */
				.addColumns(CottontailGrpc.ColumnDefinition.newBuilder().setType(CottontailGrpc.Type.STRING).setName("features").setEngine(CottontailGrpc.Engine.MAPDB).setNullable(false))  /* 4th column poly json*/
				.build();

		DDL_SERVICE.createEntity(CottontailGrpc.CreateEntityMessage.newBuilder().setTxId(txId).setDefinition(definition).build());

		TXN_SERVICE.commit(txId);
		System.out.println("Entity '" + "PVJ" + "." + "PVJ" + "' created successfully.");
	}

	public static void insertJSONToDb(String guid, String fname, String jObject){
		//initializeBBSchema();
		//initializeBBEntitites();

		/* Start a transaction per INSERT. */
		final CottontailGrpc.TransactionId txId = TXN_SERVICE.begin(Empty.getDefaultInstance());


		/* prepare for insert */
		final CottontailGrpc.FloatVector.Builder vector = CottontailGrpc.FloatVector.newBuilder();
		final CottontailGrpc.Literal id = CottontailGrpc.Literal.newBuilder().setStringData(guid).build();
		final CottontailGrpc.Literal filename = CottontailGrpc.Literal.newBuilder().setStringData(fname).build();
		final CottontailGrpc.Literal jsonObject = CottontailGrpc.Literal.newBuilder().setStringData(jObject).build();

		/* do insert */

		/* Prepare INSERT message. */
		final CottontailGrpc.InsertMessage insertMessage = CottontailGrpc.InsertMessage.newBuilder()
				.setTxId(txId)
				.setFrom(CottontailGrpc.From.newBuilder().setScan(CottontailGrpc.Scan.newBuilder().setEntity(CottontailGrpc.EntityName.newBuilder().setName("PVJ").setSchema(CottontailGrpc.SchemaName.newBuilder().setName("PVJ"))))) /* Entity the data should be inserted into. */
				.addElements(CottontailGrpc.InsertMessage.InsertElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("id")).setValue(id).build())
				.addElements(CottontailGrpc.InsertMessage.InsertElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("filename")).setValue(filename).build())
				.addElements(CottontailGrpc.InsertMessage.InsertElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("features")).setValue(jsonObject).build())
				.build();

		/* Send INSERT message. */
		DML_SERVICE.insert(insertMessage);

		TXN_SERVICE.commit(txId);
	}


	/** Name of the Cottontail DB Schema and dimension of its vector column. */
	public static void insertBBToDb(String guid, String fname, List<Float> points){
		//initializeBBSchema();
		//initializeBBEntitites();

		/* Start a transaction per INSERT. */
		final CottontailGrpc.TransactionId txId = TXN_SERVICE.begin(Empty.getDefaultInstance());


		/* prepare for insert */
		final CottontailGrpc.FloatVector.Builder vector = CottontailGrpc.FloatVector.newBuilder();
		vector.addAllVector(points);
		final CottontailGrpc.Literal id = CottontailGrpc.Literal.newBuilder().setStringData(guid).build();
		final CottontailGrpc.Literal filename = CottontailGrpc.Literal.newBuilder().setStringData(fname).build();
		final CottontailGrpc.Literal pvec = CottontailGrpc.Literal.newBuilder().setVectorData(CottontailGrpc.Vector.newBuilder().setFloatVector(vector)).build();

		/* do insert */

		/* Prepare INSERT message. */
		final CottontailGrpc.InsertMessage insertMessage = CottontailGrpc.InsertMessage.newBuilder()
				.setTxId(txId)
				.setFrom(CottontailGrpc.From.newBuilder().setScan(CottontailGrpc.Scan.newBuilder().setEntity(CottontailGrpc.EntityName.newBuilder().setName("BB").setSchema(CottontailGrpc.SchemaName.newBuilder().setName("BB"))))) /* Entity the data should be inserted into. */
				.addElements(CottontailGrpc.InsertMessage.InsertElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("id")).setValue(id).build())
				.addElements(CottontailGrpc.InsertMessage.InsertElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("filename")).setValue(filename).build())
				.addElements(CottontailGrpc.InsertMessage.InsertElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("features")).setValue(pvec).build())
				.build();

		/* Send INSERT message. */
		DML_SERVICE.insert(insertMessage);

		TXN_SERVICE.commit(txId);
	}
	public static void insertPVToDb(String guid, String fname, List<Float> points){
		//initializePolyVolumeSchema();
		//initializePolyVolumeEntitites();

		/* Start a transaction per INSERT. */
		final CottontailGrpc.TransactionId txId = TXN_SERVICE.begin(Empty.getDefaultInstance());


		/* prepare for insert */
		final CottontailGrpc.FloatVector.Builder vector = CottontailGrpc.FloatVector.newBuilder();
		vector.addAllVector(points);
		final CottontailGrpc.Literal id = CottontailGrpc.Literal.newBuilder().setStringData(guid).build();
		final CottontailGrpc.Literal filename = CottontailGrpc.Literal.newBuilder().setStringData(fname).build();
		final CottontailGrpc.Literal pvec = CottontailGrpc.Literal.newBuilder().setVectorData(CottontailGrpc.Vector.newBuilder().setFloatVector(vector)).build();

		/* do insert */

		/* Prepare INSERT message. */
		final CottontailGrpc.InsertMessage insertMessage = CottontailGrpc.InsertMessage.newBuilder()
				.setTxId(txId)
				.setFrom(CottontailGrpc.From.newBuilder().setScan(CottontailGrpc.Scan.newBuilder().setEntity(CottontailGrpc.EntityName.newBuilder().setName("PV").setSchema(CottontailGrpc.SchemaName.newBuilder().setName("PV"))))) /* Entity the data should be inserted into. */
				.addElements(CottontailGrpc.InsertMessage.InsertElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("id")).setValue(id).build())
				.addElements(CottontailGrpc.InsertMessage.InsertElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("filename")).setValue(filename).build())
				.addElements(CottontailGrpc.InsertMessage.InsertElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("features")).setValue(pvec).build())
				.build();

		/* Send INSERT message. */
		DML_SERVICE.insert(insertMessage);

		TXN_SERVICE.commit(txId);
	}
	public static void insertToDb(String guid, int frame, int regionId, List<Float> points){
		//initializePolyEntitites();

		/* Start a transaction per INSERT. */
		final CottontailGrpc.TransactionId txId = TXN_SERVICE.begin(Empty.getDefaultInstance());


		/* prepare for insert */
		final CottontailGrpc.FloatVector.Builder vector = CottontailGrpc.FloatVector.newBuilder();
		vector.addAllVector(points);
		final CottontailGrpc.Literal id = CottontailGrpc.Literal.newBuilder().setStringData(guid).build();
		final CottontailGrpc.Literal fn = CottontailGrpc.Literal.newBuilder().setIntData(frame).build();
		final CottontailGrpc.Literal reg = CottontailGrpc.Literal.newBuilder().setIntData(regionId).build();
		final CottontailGrpc.Literal pvec = CottontailGrpc.Literal.newBuilder().setVectorData(CottontailGrpc.Vector.newBuilder().setFloatVector(vector)).build();

		/* do insert */

		/* Prepare INSERT message. */
		final CottontailGrpc.InsertMessage insertMessage = CottontailGrpc.InsertMessage.newBuilder()
				.setTxId(txId)
				.setFrom(CottontailGrpc.From.newBuilder().setScan(CottontailGrpc.Scan.newBuilder().setEntity(CottontailGrpc.EntityName.newBuilder().setName("poly").setSchema(CottontailGrpc.SchemaName.newBuilder().setName(SCHEMA_NAME))))) /* Entity the data should be inserted into. */
				.addElements(CottontailGrpc.InsertMessage.InsertElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("id")).setValue(id).build())
				.addElements(CottontailGrpc.InsertMessage.InsertElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("frame")).setValue(fn).build())
				.addElements(CottontailGrpc.InsertMessage.InsertElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("region")).setValue(reg).build())
				.addElements(CottontailGrpc.InsertMessage.InsertElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("points")).setValue(pvec).build())
				.build();

		/* Send INSERT message. */
		DML_SERVICE.insert(insertMessage);

		TXN_SERVICE.commit(txId);
	}

	public static String polyDataToString(List<Quartet<String, Integer, Integer, List<Float>>> polyData){
		StringBuilder sb = new StringBuilder();
		sb.append("----- POLYGON -----" + "\n");
		for(Quartet<String, Integer, Integer, List<Float>> q : polyData){
			sb.append("id: " + q.getValue0() + "\n");
			sb.append("frame: " + q.getValue1() + "\n");
			sb.append("region idx: " + q.getValue2() + "\n");
			String list = "Points:";
			for(int i = 0; i<q.getValue3().size(); ++i){
				list += " " + q.getValue3().get(i).toString();
			}
			sb.append(list + "\n");

			sb.append("----- END POLYGON -----" + "\n");
		}

		return sb.toString();
	}

	public static Iterator<CottontailGrpc.QueryResponseMessage> executeNearestNeighborQuery(CottontailGrpc.FloatVector.Builder vector, String entityName, String schemaName)  {
		/* Number of entries to return. */
		final int k = 10;

			/* Prepare kNN query vector. */
		final CottontailGrpc.QueryMessage query = CottontailGrpc.QueryMessage.newBuilder().setQuery(
				CottontailGrpc.Query.newBuilder().setFrom(CottontailGrpc.From.newBuilder().setScan(
						CottontailGrpc.Scan.newBuilder().setEntity(CottontailGrpc.EntityName.newBuilder().setName(entityName).setSchema(CottontailGrpc.SchemaName.newBuilder().setName(schemaName)))).build() /* Entity to select data from. */
				)
						.setKnn(CottontailGrpc.Knn.newBuilder().setK(k).setAttribute(CottontailGrpc.ColumnName.newBuilder().setName("features")).setDistance(CottontailGrpc.Knn.Distance.L2SQUARED).setQuery(CottontailGrpc.Vector.newBuilder().setFloatVector(vector))) /* kNN predicate on the column 'feature' with k = 10 and cosine distance. */
						.setProjection(CottontailGrpc.Projection.newBuilder()
								.addColumns(
								CottontailGrpc.Projection.ProjectionElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("id")) /* Star projection. */
								)
								.addColumns(
										CottontailGrpc.Projection.ProjectionElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("filename")) /* Star projection. */
								)
								.addColumns(
								CottontailGrpc.Projection.ProjectionElement.newBuilder().setColumn(CottontailGrpc.ColumnName.newBuilder().setName("distance")) /* Star projection. */
								))
				).build();

		/* Execute query. */
		final Iterator<CottontailGrpc.QueryResponseMessage> results = DQL_SERVICE.query(query);

		/* Print results. */
		//System.out.println("Results of query:");
		//results.forEachRemaining(r -> r.getTuplesList().forEach(t -> System.out.println(t.toString())));

		return results;
	}

}
