package edu.uw.cs.lil.uwtime.learn.binary;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;

public interface IBinaryFeatureSet<DI extends IDataItem<?>> {
	public double score(DI dataItem, IHashVector theta);
	public void setFeats(DI dataItem, IHashVector feats);
}
