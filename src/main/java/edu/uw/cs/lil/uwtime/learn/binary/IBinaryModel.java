package edu.uw.cs.lil.uwtime.learn.binary;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;

public interface IBinaryModel<DI extends IDataItem<?>> {
	IHashVectorImmutable computeFeatures(DI dataItem);
	double score(DI dataItem);
	IHashVector getTheta();
}
