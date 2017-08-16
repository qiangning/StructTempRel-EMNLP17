package edu.uw.cs.lil.uwtime.learn.featuresets.detection;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.uwtime.chunking.TemporalChunkerOutput;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.learn.featuresets.TemporalDetectionFeatureSet;
import edu.uw.cs.lil.uwtime.utils.DependencyUtils;

public class DeterminerFeatureSet extends TemporalDetectionFeatureSet {
	@Override
	protected String getFeatureTag() {
		return "DETERMINER";
	}

	@Override
	protected IHashVectorImmutable setMentionFeats(
			TemporalChunkerOutput dataItem, IHashVector feats) {
		TemporalJointChunk chunk = dataItem.getChunk();
		int index = DependencyUtils.getFirstDeterminerParent(chunk);
		if (index != -1)
			feats.set(new KeyArgs(getFeatureTag(), index >= chunk.getStart() && index <= chunk.getEnd() ? "inside" : "outside"), 1);
		return feats;
	}
}
