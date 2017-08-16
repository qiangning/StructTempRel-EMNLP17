package edu.uw.cs.lil.uwtime.learn.featuresets;

import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.uwtime.chunking.TemporalChunkerOutput;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryFeatureSet;

public abstract class TemporalDetectionFeatureSet implements IBinaryFeatureSet<TemporalChunkerOutput>{
	abstract protected IHashVectorImmutable setMentionFeats(TemporalChunkerOutput dataItem, IHashVector feats);
	abstract protected String getFeatureTag();

	@Override
	public double score(TemporalChunkerOutput dataItem, IHashVector theta) {
		return setMentionFeats(dataItem, HashVectorFactory.create()).vectorMultiply(theta);
	}
	@Override
	public void setFeats(TemporalChunkerOutput dataItem,
			IHashVector feats) {
		setMentionFeats(dataItem, feats);
	}
}
