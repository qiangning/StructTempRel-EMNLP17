package edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron;

import edu.illinois.cs.cogcomp.lbjava.learn.SupportVectorMachine;

/**
 * Created by qning2 on 11/27/16.
 */
public class ParamLBJ {
    public static double[] learningRates = new double[] { 0.1, 0.2, 0.3, 0.4,
            0.5 };
    public static double[] thicknesses = new double[] { 0, 0.4, 0.8, 1.2, 1.6,
            2.0, 2.4, 2.8, 3.2, 3.6, 4.0 };
    public static int[] learningRounds = new int[] { 10, 30, 50, 100, 200, 500,
            1000 };

    public static double etLearningRate = 0.1;
    public static double etThickness = 0.4;
    public static int etLearningRound = 30;

    public static double eeLearningRate = 0.1;
    public static double eeThickness = 0.4;
    public static int eeLearningRound = 30;

    public static double trainingFraction = 0.8;
    public static double validationFraction = 0.25;

    public static double svmC = 0.1;
    public static double svmE = 0.1;
    public static double svmB = SupportVectorMachine.defaultBias;

    public static String FEAT_DELIMITER = " ";
}
