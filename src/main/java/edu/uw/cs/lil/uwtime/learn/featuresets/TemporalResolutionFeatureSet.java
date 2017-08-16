package edu.uw.cs.lil.uwtime.learn.featuresets;

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.situated.ISituatedDataItem;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointFeatureSet;
import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.composites.Triplet;

public abstract class TemporalResolutionFeatureSet implements IJointFeatureSet<ISituatedDataItem<Sentence, TemporalJointChunk>, MentionResult> {
	private static final long serialVersionUID = -1674194313235949820L;

	public abstract String getFeatureTag();
	protected abstract IHashVectorImmutable setMentionFeats(MentionResult executionStep, IHashVector feats, TemporalJointChunk chunk);
	
	public List<Triplet<KeyArgs, Double, String>> getFeatureWeights(IHashVector theta) {
		final List<Triplet<KeyArgs, Double, String>> weights = new LinkedList<Triplet<KeyArgs, Double, String>>();
		for (final Pair<KeyArgs, Double> feature : theta.getAll(getFeatureTag())) {
			weights.add(Triplet.of(feature.first(), feature.second(),
					(String) null));
		}
		return weights;
	}

	@Override
	public boolean isValidWeightVector(IHashVectorImmutable update) {
		// No protected features
		return true;
	}
	
	@Override
	public double score(MentionResult executionStep, IHashVector theta,
			ISituatedDataItem<Sentence, TemporalJointChunk> dataItem) {
		return setMentionFeats(executionStep, HashVectorFactory.create(), dataItem.getState()).vectorMultiply(theta);
	}
	
	@Override
	public void setFeats(MentionResult executionStep, IHashVector feats,
			ISituatedDataItem<Sentence, TemporalJointChunk> dataItem) {
		setMentionFeats(executionStep, feats, dataItem.getState());
	}
}
