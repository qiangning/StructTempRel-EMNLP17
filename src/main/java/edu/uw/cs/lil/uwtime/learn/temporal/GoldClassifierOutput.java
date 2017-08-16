package edu.uw.cs.lil.uwtime.learn.temporal;

import edu.uw.cs.lil.tiny.base.hashvector.HashVectorFactory;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.uwtime.chunking.GoldChunkerOutput;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryClassifierOutput;

public class GoldClassifierOutput implements IBinaryClassifierOutput<GoldChunkerOutput> {
	@Override
	public boolean getBinaryClass() {
		return true;
	}
	
	@Override
	public double getScore() {
		return 1;
	}
	
	@Override
	public double getProbability(boolean label) {
		return label ? 1 : 0;
	}
	
	@Override
	public String toString() {
		return "Gold mention";
	}

	@Override
	public IHashVectorImmutable getFeatures() {
		return HashVectorFactory.create();
	}
}
