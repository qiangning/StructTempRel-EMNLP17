package edu.uw.cs.lil.uwtime.learn.featuresets.resolution;

import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.learn.featuresets.TemporalResolutionFeatureSet;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.utils.DependencyUtils;
import edu.uw.cs.lil.uwtime.utils.TemporalConfig;

public class GovernorVerbFeatureSet extends TemporalResolutionFeatureSet {
	private static final long serialVersionUID = -8004273529927172644L;

	@Override
	public String getFeatureTag() {
		return "GOVERNOR_VERB";
	}

	@Override
	protected IHashVectorImmutable setMentionFeats(MentionResult result,
			IHashVector feats, TemporalJointChunk chunk) {	
		if (TemporalConfig.getInstance().useContext) {
			if (result.getEntity() instanceof TemporalSequence) {
				feats.set(new KeyArgs(getFeatureTag(), result.getContext().getDirection().toString(), DependencyUtils.getGovernorVerbFeature(chunk)), 1);
			}
		}
		else {
			feats.set(new KeyArgs(getFeatureTag(), result.getContext().getDirection().toString()), 1);
		}
		return feats;
	}
}
