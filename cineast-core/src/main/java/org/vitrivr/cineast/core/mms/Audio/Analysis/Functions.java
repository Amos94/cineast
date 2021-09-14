package org.vitrivr.cineast.core.mms.Audio.Analysis;
import java.util.*;
public class Functions{
	public static double max(double[] data){
		double[] temp = (double[]) data.clone();
		Arrays.sort(temp);
		return temp[temp.length-1];
	}
}
