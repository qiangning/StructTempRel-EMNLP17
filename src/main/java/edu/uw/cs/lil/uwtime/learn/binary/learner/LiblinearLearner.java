package edu.uw.cs.lil.uwtime.learn.binary.learner;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.utils.IValidator;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryModel;
import edu.uw.cs.utils.composites.Pair;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

public class LiblinearLearner<DI extends IDataItem<?>> extends AbstractBinaryLearner<DI> {	
	private final double c ;
	private final double eps;
	private final SolverType solver;
	protected LiblinearLearner(
			IDataCollection<DI> trainingData,
			IValidator<DI, Boolean> validator,
			SolverType solverType,
			double c,
			double eps) {
		super(trainingData, validator);
		this.c = c;
		this.eps = eps;
		this.solver = solverType;
	}

	@Override
	public void train(IBinaryModel<DI> model) {
		final Set<KeyArgs> featureSpace = new HashSet<KeyArgs>();
		for (DI dataItem : trainingData) {
			IHashVectorImmutable sampleFeatures = model.computeFeatures(dataItem);
			for (final Pair<KeyArgs, Double> entry : sampleFeatures)
				featureSpace.add(entry.first());
		}
		final Map<KeyArgs, Integer> featureIndexes = new HashMap<KeyArgs, Integer>();
		final Map<Integer, KeyArgs> inverseFeatureIndexes = new HashMap<Integer, KeyArgs>();
		int k = 1;
		for(KeyArgs key : featureSpace) {
			featureIndexes.put(key,  k);
			inverseFeatureIndexes.put(k, key);
			k++;
		}

		Feature[][] allFeatures = new Feature[trainingData.size()][];
		double[] labels = new double[trainingData.size()];
		int i = 0;
		for (DI dataItem : trainingData) {
			IHashVectorImmutable sampleFeatures = model.computeFeatures(dataItem);
			List<Feature> currentFeatures = new LinkedList<Feature>();
			for (final Pair<KeyArgs, Double> entry : sampleFeatures) {
				currentFeatures.add(new Feature() {
					@Override
					public void setValue(double arg0) {
					}

					@Override
					public double getValue() {
						return entry.second();
					}

					@Override
					public int getIndex() {
						return featureIndexes.get(entry.first());
					}
				});
			}
			Collections.sort(currentFeatures, new Comparator<Feature>() {
				@Override
				public int compare(Feature arg0, Feature arg1) {
					return Integer.valueOf(arg0.getIndex()).compareTo(arg1.getIndex());
				}
			});
			allFeatures[i] = currentFeatures.toArray(new Feature[0]);
			labels[i] = validator.isValid(dataItem, true) ? 1 : 0;
			i++;
		}
		Problem problem = new Problem();
		problem.l = trainingData.size();
		problem.n = featureIndexes.size();
		problem.x = allFeatures;
		problem.y = labels;

		Parameter parameter = new Parameter(solver, c, eps);
		Model liblinearModel = Linear.train(problem, parameter);
		int flip = liblinearModel.getLabels()[0] > 0 ? 1 : -1;
		for (int f = 0 ; f < liblinearModel.getFeatureWeights().length; f++) {
			if (liblinearModel.getFeatureWeights()[f] != 0)
				model.getTheta().set(inverseFeatureIndexes.get(f + 1), flip * liblinearModel.getFeatureWeights()[f]);
		}
	}

	public static class Builder<DI extends IDataItem<?>> {
		private final IDataCollection<DI>		trainingData;
		private final IValidator<DI, Boolean>	validator;
		private SolverType                      solver = SolverType.L1R_LR;
		private double 							c = 1.0;
		private double 							eps = 0.01;

		public Builder(IDataCollection<DI> trainingData,
				IValidator<DI, Boolean> validator) {
			this.trainingData = trainingData;
			this.validator = validator;
		}

		public LiblinearLearner<DI> build() {
			return new LiblinearLearner<DI>	(trainingData, validator, solver, c, eps);
		}
		
		public Builder<DI> setC(double c) {
			this.c = c;
			return this;
		}
		
		public Builder<DI> setEps(double eps) {
			this.eps = eps;
			return this;
		}
		
		public Builder<DI> setSolverType(SolverType solverType) {
			this.solver = solverType;
			return this;
		}
	}
}
