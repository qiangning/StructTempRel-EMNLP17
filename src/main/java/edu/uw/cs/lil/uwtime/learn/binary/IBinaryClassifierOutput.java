package edu.uw.cs.lil.uwtime.learn.binary;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;

public interface IBinaryClassifierOutput <DI extends IDataItem<?>> {
	boolean getBinaryClass();
	double getScore();
	double getProbability(boolean label);
	IHashVectorImmutable getFeatures();
}
