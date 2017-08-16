package edu.uw.cs.lil.uwtime.learn.temporal;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.uwtime.chunking.TemporalChunkerOutput;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryClassifierOutput;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryModel;

public class TemporalClassifierOutput implements IBinaryClassifierOutput <TemporalChunkerOutput> {
	private final IBinaryModel<TemporalChunkerOutput> model;
	private final double score;
	private final double probability;
	private final TemporalClassifier classifier;
	private final IHashVectorImmutable features;

	public TemporalClassifierOutput(TemporalChunkerOutput dataItem, IBinaryModel<TemporalChunkerOutput> model, TemporalClassifier classifier) {
		this.model = model;
		this.score = model.score(dataItem);
		this.probability = 1/(1 + Math.exp(-score));
		this.features = model.computeFeatures(dataItem);
		this.classifier = classifier;
	}
	
	@Override
	public boolean getBinaryClass() {
		//return score >= 0;
		return probability >= classifier.getThreshold();
	}
	
	@Override
	public double getScore() {
		return score;
	}
	
	@Override
	public double getProbability(boolean label) {
		return label ? probability : 1 - probability;
	}
	
	@Override
	public String toString() {
		return String.format("%s[%f]", model.getTheta().printValues(features), probability);
	}

	@Override
	public IHashVectorImmutable getFeatures() {
		return features;
	}
}
