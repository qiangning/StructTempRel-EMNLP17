package edu.uw.cs.lil.uwtime.learn.featuresets.resolution;

import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.learn.featuresets.TemporalResolutionFeatureSet;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.utils.composites.Pair;

public class IntersectionsFeatureSet extends TemporalResolutionFeatureSet {
	private static final long serialVersionUID = -7286014181176354361L;

	@Override
	protected IHashVectorImmutable setMentionFeats(MentionResult result, IHashVector feats,
			TemporalJointChunk chunk) {

		for (Pair<TemporalSequence, TemporalSequence> arg : result.getExecutionHistory().getIntersectionArguments()) {
			feats.set(new KeyArgs(
					getFeatureTag(),
					arg.first().getDeepestNode().getName() + "^" + arg.second().getDeepestNode().getName()), 1);
		}
		return feats;
	}

	@Override
	public String getFeatureTag() {
		return "INTERSECTION";
	}
}
