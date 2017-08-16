package edu.uw.cs.lil.uwtime.stats;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.uw.cs.lil.uwtime.utils.TemporalLog;
import edu.uw.cs.utils.composites.Triplet;


public class EndToEndStatistics extends AbstractStatistics{
	private final List<Triplet<Integer,Double,Double>> predictedMentions, goldMentions, correctMentions, correctValues;
	private final Set<Integer> folds;
	
	public EndToEndStatistics() {
		correctMentions = new LinkedList<Triplet<Integer,Double,Double>>();
		predictedMentions = new LinkedList<Triplet<Integer,Double,Double>>();
		goldMentions = new LinkedList<Triplet<Integer,Double,Double>>();
		correctValues = new LinkedList<Triplet<Integer,Double,Double>>();
		folds = new HashSet<Integer>();
	}

	public synchronized void incrementCorrectMentions(int fold, double detectionProbability, double derivationProbability) {
		correctMentions.add(Triplet.of(fold, detectionProbability, derivationProbability));
		folds.add(fold);
	}

	public synchronized void incrementGoldMentions(int fold) {
		goldMentions.add(Triplet.of(fold, 1.0, 1.0));
		folds.add(fold);
	}

	public synchronized void incrementPredictedMentions(int fold ,double detectionProbability, double derivationProbability) {
		predictedMentions.add(Triplet.of(fold, detectionProbability, derivationProbability));
		folds.add(fold);
	}

	public synchronized void incrementCorrectValues(int fold, double detectionProbability, double derivationProbability) {
		correctValues.add(Triplet.of(fold, detectionProbability, derivationProbability));
		folds.add(fold);
	}

	public synchronized int getNumCorrectMentions(Triplet<Integer,Double,Double> params) {
		int sumCorrect = 0;
		for (Triplet<Integer, Double, Double> p : correctMentions)
			if (params.first() < 0 || params.first() == p.first())
				if (p.second() >= params.second() && p.third() >= params.third())
					sumCorrect++;
		return sumCorrect;
	}
	
	public synchronized int getNumGoldMentions(Triplet<Integer,Double,Double> params) {
		int sumGold = 0;
		for (Triplet<Integer, Double, Double> p : goldMentions)
			if (params.first() < 0 || params.first() == p.first())
				if (p.second() >= params.second() && p.third() >= params.third())
					sumGold++;
		return sumGold;
	}

	public synchronized int getNumPredictedMentions(Triplet<Integer,Double,Double> params) {
		int sumPredicted = 0;
		for (Triplet<Integer, Double, Double> p : predictedMentions)
			if (params.first() < 0 || params.first() == p.first())
				if (p.second() >= params.second() && p.third() >= params.third())
					sumPredicted++;
		return sumPredicted;
	}

	public synchronized int getNumCorrectValues(Triplet<Integer,Double,Double> params) {
		int sumCorrect = 0;
		for (Triplet<Integer, Double, Double> p : correctValues)
			if (params.first() < 0 || params.first() == p.first())
				if (p.second() >= params.second() && p.third() >= params.third())
					sumCorrect++;
		return sumCorrect;
	}

	public synchronized double getRecall(Triplet<Integer,Double,Double> params) {
		return ((double) getNumCorrectMentions(params)) / getNumGoldMentions(params);
	}

	public synchronized double getPrecision(Triplet<Integer,Double,Double> params) {
		return ((double) getNumCorrectMentions(params)) / getNumPredictedMentions(params);
	}

	public synchronized double getF1(Triplet<Integer,Double,Double> params) {
		double r = getRecall(params);
		double p = getPrecision(params);
		return 2*r*p/(r+p);
	}

	public synchronized double getValueAccuracy(Triplet<Integer,Double,Double> params) {
		return ((double) getNumCorrectValues(params)) / getNumCorrectMentions(params);
	}
	
	public synchronized double getF1StandardDeviation(Triplet<Integer,Double,Double> params) {
		double[] f1s = new double[folds.size()];
		int count = 0;
		double mean = 0;
		for (int i : folds) {
			f1s[count] = getF1(Triplet.of(i, params.second(),  params.third()));
			mean += f1s[count];
			count++;
		}
		mean /= f1s.length;
		double sum = 0;
		for (double f1 : f1s)
			sum += (f1 - mean) * (f1 - mean);
		return Math.sqrt(sum/f1s.length);
	}

	@Override
	public String formatStats() {
		return formatStats(Triplet.of(-1, 0.0, 0.0));
	}

	public String formatStats(Triplet<Integer,Double,Double> params) {
		return "Resolution threshold: " + params.third() + "\n" +
				detectionToString(params) + 
				resolutionToString(params);
	}

	private String detectionToString(Triplet<Integer,Double,Double> params) {
		String s = "";
		s += String.format("Detection recall:    %.2f%% (%d/%d)\n", 100 * getRecall(params), getNumCorrectMentions(params), getNumGoldMentions(params));
		s += String.format("Detection precision: %.2f%% (%d/%d)\n", 100 * getPrecision(params), getNumCorrectMentions(params), getNumPredictedMentions(params));
		s += String.format("Detection F1:        %.2f%%\n", 100 * getF1(params));
		return s;
	}

	private String resolutionToString(Triplet<Integer,Double,Double> params){
		String s = "";
		double valueAccuracy = getValueAccuracy(params);
		s += String.format("Value accuracy:  %.2f%% (%d/%d)\n", 100 * valueAccuracy, getNumCorrectValues(params), getNumCorrectMentions(params));
		s += String.format("Value F1:        %.2f%% (stddev=%f)\n", 100 * getF1(params) * valueAccuracy, getF1StandardDeviation(params));
		return s;
	}

	public synchronized double searchThreshold(boolean printPRCurve) {
		double bestF1 = -1;
		double bestP = -1;
		for(double p = 0; p <= 1; p += 0.02) {
			Triplet<Integer, Double, Double> params = Triplet.of(-1, 0.0, p);
			double valueAccuracy = getValueAccuracy(params);
			double recall = getRecall(params) * valueAccuracy;
			double precision = getPrecision(params) * valueAccuracy;
			double f1 = getF1(params) * valueAccuracy;

			if (printPRCurve && !Double.isNaN(recall) && !Double.isNaN(precision))
				TemporalLog.printf("pr_curve", "%f,%f,%f\n", p, precision, recall);
			if (!Double.isNaN(f1) && f1 > bestF1) {
				bestF1 = f1;
				bestP = p;
			}
		}
		return bestP;
	}
}
