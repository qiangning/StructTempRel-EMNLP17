package edu.uw.cs.lil.uwtime.learn.featuresets.detection;

import java.util.List;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.uwtime.chunking.TemporalChunkerOutput;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.learn.featuresets.TemporalDetectionFeatureSet;

public class AgeReferenceFeatureSet extends TemporalDetectionFeatureSet {
	@Override
	protected String getFeatureTag() {
		return "AGE_REFERENCE";
	}

	@Override
	protected IHashVectorImmutable setMentionFeats(
			TemporalChunkerOutput dataItem, IHashVector feats) {
		TemporalJointChunk chunk = dataItem.getChunk();
		List<String> tokens = chunk.getSentence().getTokens();
		if (tokens.size() > chunk.getEnd() + 1 && tokens.get(chunk.getEnd() + 1).equals("old"))
			feats.set(getFeatureTag(), 1);
		return feats;
	}
}
