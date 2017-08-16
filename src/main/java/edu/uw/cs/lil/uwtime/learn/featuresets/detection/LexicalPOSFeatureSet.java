package edu.uw.cs.lil.uwtime.learn.featuresets.detection;

import java.util.List;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.uwtime.chunking.TemporalChunkerOutput;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.learn.featuresets.TemporalDetectionFeatureSet;
import edu.uw.cs.lil.uwtime.utils.DependencyUtils.DependencyParseToken;

public class LexicalPOSFeatureSet extends TemporalDetectionFeatureSet {
	@Override
	protected String getFeatureTag() {
		return "LEXICAL_POS";
	}

	@Override
	protected IHashVectorImmutable setMentionFeats(
			TemporalChunkerOutput dataItem, IHashVector feats) {
		TemporalJointChunk chunk = dataItem.getChunk();
		List<DependencyParseToken> dependencyParse = chunk.getSentence().getDependencyParse();
		for (int i = chunk.getStart(); i <= chunk.getEnd(); i++) {
			DependencyParseToken token = dependencyParse.get(i);
			feats.set(new KeyArgs(getFeatureTag(), token.getWord(), token.getPOS()), 1);
		}
		return feats;
	}
}
