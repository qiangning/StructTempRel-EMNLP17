package edu.uw.cs.lil.uwtime.learn.featuresets.resolution;

import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.learn.featuresets.TemporalResolutionFeatureSet;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;

public class EntityStructureFeatureSet extends TemporalResolutionFeatureSet {
	private static final long serialVersionUID = -8004273529927172644L;
	
	@Override
	public String getFeatureTag() {
		return "ENTITY_STRUCTURE";
	}

	@Override
	protected IHashVectorImmutable setMentionFeats(MentionResult result,
			IHashVector feats, TemporalJointChunk chunk) {
		feats.set(new KeyArgs(
				getFeatureTag(),
				result.getEntity().getStructure()), 1);
		return feats;
	}
}
