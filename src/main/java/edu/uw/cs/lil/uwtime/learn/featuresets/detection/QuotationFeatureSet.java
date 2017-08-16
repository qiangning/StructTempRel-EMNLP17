package edu.uw.cs.lil.uwtime.learn.featuresets.detection;

import java.util.List;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.uwtime.chunking.TemporalChunkerOutput;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.learn.featuresets.TemporalDetectionFeatureSet;
import edu.uw.cs.lil.uwtime.utils.DependencyUtils.DependencyParseToken;

public class QuotationFeatureSet extends TemporalDetectionFeatureSet {
	private final int window;
	
	public QuotationFeatureSet(int window) {
		this.window = window;
	}
	
	@Override
	protected String getFeatureTag() {
		return "QUOTATION";
	}
	
	private static boolean isQuotation(String s) {
		return s.equals("``") || s.equals("''") || s.equals("\"");
	}

	@Override
	protected IHashVectorImmutable setMentionFeats(
			TemporalChunkerOutput dataItem, IHashVector feats) {
		TemporalJointChunk chunk = dataItem.getChunk();
		List<DependencyParseToken> tokens = chunk.getSentence().getDependencyParse();
		boolean foundStartQuotation = false;
		for (int i = Math.max(0, chunk.getStart() - window) ; i < chunk.getStart() ; i++)
			if (isQuotation(tokens.get(i).getWord())) {
				foundStartQuotation = true;
				break;
			}
		if(foundStartQuotation) {
			for (int i = chunk.getEnd() + 1 ; i < Math.min(chunk.getEnd() + 1 + window, chunk.getSentence().getNumTokens()) ; i++)
				if (isQuotation(tokens.get(i).getWord())) {
					feats.set(new KeyArgs(getFeatureTag()), 1);
					break;
				}
		}
		return feats;
	}
}
