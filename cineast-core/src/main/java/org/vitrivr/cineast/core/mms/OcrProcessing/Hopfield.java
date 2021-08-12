package org.vitrivr.cineast.core.mms.OcrProcessing;

import java.util.*;

/**
 * Hopfield neural network
 *
 * <p/>
 * Copyright 1998-2012 by Mark Watson. All rights reserved.
 * <p/>
 * This software is can be used under either of the following licenses:
 * <p/>
 * 1. LGPL v3<br/>
 * 2. Apache 2
 * <p/>
 */
public class Hopfield 
{
    
    public static void main(String[] args)
    {
        
        float[] trainingData1 = {1,1,1,-1,1,1,-1,1};
        float[] trainingData2 = {-1, 1, -1, 1, -1, 1, -1, 1};
        Hopfield h = new Hopfield(8);
        h.addTrainingData(trainingData1);
        h.addTrainingData(trainingData2);
        h.train();
        float[] distorted = {1,1,1,-1,1,-1,-1,1};
        float[] corrected = h.recall(distorted, 20);
        for(float f : corrected)
            System.out.print(f + ", ");
        
    }

    public Hopfield(int numInputs) {
        this.numInputs = numInputs;
        weights = new float[numInputs][numInputs];
        inputCells = new float[numInputs];
        tempStorage = new float[numInputs];
    }

    public void addTrainingData(float[] data) {
        trainingData.add(data);
    }

    public void train() {
        for (int j = 1; j < numInputs; j++) {
            for (int i = 0; i < j; i++) {
                for (int n = 0; n < trainingData.size(); n++) {
                    float[] data = (float[]) trainingData.get(n);
                    float temp1 = adjustInput(data[i]) * adjustInput(data[j]);
                    float temp = truncate(temp1 + weights[j][i]);
                    weights[i][j] = weights[j][i] = temp;
                }
            }
        }
        for (int i = 0; i < numInputs; i++) {
            tempStorage[i] = 0.0f;
            for (int j = 0; j < i; j++) {
                tempStorage[i] += weights[i][j];
            }
        }
    }

    public float[] recall(float[] pattern, int numIterations) {
        for (int i = 0; i < numInputs; i++) inputCells[i] = pattern[i];
        for (int ii = 0; ii < numIterations; ii++) {
            for (int i = 0; i < numInputs; i++) {
                if (deltaEnergy(i) > 0.0f) {
                    inputCells[i] = 1.0f;
                } else {
                    inputCells[i] = -1.0f;
                }
            }
        }
        return inputCells;
    }
    
    /** Load input -- ONLY for recallSingle and recallSync
     * @param data */
    public void loadInputs(float[] data)
    {
       for (int i = 0; i < numInputs; i++) inputCells[i] = data[i];
    }
    
    public float[] recallSingle(int input)
    {
        if(deltaEnergy(input) > 0.0f)
            inputCells[input] = 1.0f;
        else
            inputCells[input] = -1.0f;
        return inputCells;
    }
    
    public float[] recallSync(int[] inputs)
    {
        float[] newInputCells = new float[numInputs];
        System.arraycopy(inputCells, 0, newInputCells, 0, numInputs);
        for (int input : inputs) 
        {
            if (deltaEnergy(input) > 0.0f)
                newInputCells[input] = 1.0f;
            else 
                newInputCells[input] = -1.0f;
        }
        inputCells = newInputCells;
        return inputCells;
    }

    private float adjustInput(float x) {
        if (x < 0.0f) return -1.0f;
        return 1.0f;
    }

    private float truncate(float x) {
        //return Math.round(x);
        int i = (int) x;
        return (float) i;
    }

    private float deltaEnergy(int index) {
        float temp = 0.0f;
        for (int j = 0; j < numInputs; j++) {
            temp += weights[index][j] * inputCells[j];
        }
        return 2.0f * temp - tempStorage[index];
    }

    int numInputs;
    ArrayList<float[]> trainingData = new ArrayList<>();
    float[][] weights;
    float[] tempStorage;
    float[] inputCells;
}
