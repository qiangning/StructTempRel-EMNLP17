package edu.uw.cs.lil.uwtime.learn.featuresets.detection;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.uwtime.chunking.TemporalChunkerOutput;
import edu.uw.cs.lil.uwtime.learn.featuresets.TemporalDetectionFeatureSet;

public class TemporalLexicalOriginFeatureSet extends TemporalDetectionFeatureSet {

	@Override
	protected String getFeatureTag() {
		return "TEMPORAL_LEXICAL_ORIGIN";
	}

	@Override
	protected IHashVectorImmutable setMentionFeats(
			TemporalChunkerOutput dataItem, IHashVector feats) {
		for (LexicalEntry<LogicalExpression> entry : dataItem.getCollapsedMaxLexicalEntries())
			feats.set(new KeyArgs(getFeatureTag(), entry.getOrigin()), 1);
		return feats;
	}
}
