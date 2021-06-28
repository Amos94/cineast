package org.vitrivr.cineast.core.mms.Tr;


import java.util.Vector;

public class AssignmentOptimal {
	public double Solve(double[][] DistMatrix,
			Vector<Integer> Assignment) {
		int N = DistMatrix.length; // number of columns (tracks)
		int M = DistMatrix[0].length; // number of rows (measurements)
		int dim =  Math.max(N, M);

		// Init
		int[] match = new int[dim];

		Hungarian b = new Hungarian(DistMatrix);
		match = b.execute();
		
		// form result
		Assignment.clear();
		for (int x = 0; x < N; x++) {
			Assignment.add(match[x]);
		}
		
		return b.computeCost(DistMatrix, match);
	}
}
