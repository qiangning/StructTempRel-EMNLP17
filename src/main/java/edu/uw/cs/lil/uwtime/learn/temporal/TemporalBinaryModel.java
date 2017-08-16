package edu.uw.cs.lil.uwtime.learn.temporal;

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.uwtime.chunking.TemporalChunkerOutput;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryFeatureSet;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryModel;

public class TemporalBinaryModel implements IBinaryModel<TemporalChunkerOutput>{
	private final IHashVector theta;
	private final List<IBinaryFeatureSet<TemporalChunkerOutput>> featureSets;

	public TemporalBinaryModel(List<IBinaryFeatureSet<TemporalChunkerOutput>> featureSets) {
		this.featureSets = featureSets;
		this.theta = HashVectorFactory.create();
	}

	@Override
	public IHashVectorImmutable computeFeatures(TemporalChunkerOutput dataItem) {
		final IHashVector features = HashVectorFactory.create();
		for (final IBinaryFeatureSet<TemporalChunkerOutput> featureSet : featureSets) {
			featureSet.setFeats(dataItem, features);
		}
		return features;
	}

	@Override
	public double score(TemporalChunkerOutput dataItem) {
		double score = 0.0;
		for (final IBinaryFeatureSet<TemporalChunkerOutput> featureSet : featureSets)
			score += featureSet.score(dataItem, getTheta());
		return score;
	}

	@Override
	public IHashVector getTheta() {
		return theta;
	}

	public static class Builder {
		private final List<IBinaryFeatureSet<TemporalChunkerOutput>> featureSets = 
				new LinkedList<IBinaryFeatureSet<TemporalChunkerOutput>>();

		public Builder addFeatureSet(IBinaryFeatureSet<TemporalChunkerOutput> featureSet) {
			featureSets.add(featureSet);
			return this;
		}
		public TemporalBinaryModel build() {
			return new TemporalBinaryModel(featureSets);
		}
	}
}
