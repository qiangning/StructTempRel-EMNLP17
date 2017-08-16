package edu.uw.cs.lil.uwtime.learn.featuresets.detection;

import java.util.List;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.uwtime.chunking.TemporalChunkerOutput;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.learn.featuresets.TemporalDetectionFeatureSet;

public class TimePrepositionsFeatureSet extends TemporalDetectionFeatureSet {
	private String[] timePrepositions = {"in", "on", "at", "over", "for", "during", "by", "until"};

	@Override
	protected String getFeatureTag() {
		return "TIME_PREPOSITION";
	}

	@Override
	protected IHashVectorImmutable setMentionFeats(
			TemporalChunkerOutput dataItem, IHashVector feats) {
		TemporalJointChunk chunk = dataItem.getChunk();
		List<String> tokens = dataItem.getChunk().getSentence().getTokens();
		if (chunk.getStart() - 1 >= 0) {
			String previousToken = tokens.get(chunk.getStart() - 1);

			for (String s : timePrepositions){
				if (previousToken.equals(s)) {
					feats.set(new KeyArgs(getFeatureTag()), 1);
					break;
				}
			}
		}
		feats.set("DETECTION_BIAS",  1);
		return feats;
	}
}
