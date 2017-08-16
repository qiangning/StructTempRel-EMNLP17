package edu.uw.cs.lil.uwtime.annotation;

import java.io.IOException;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.uwtime.detection.AbstractDetector;
import edu.uw.cs.lil.uwtime.resolution.AbstractResolver;

public abstract class AbstractAnnotator <DI extends IDataItem<?>, DET extends IDataItem<?>, RES>  {
	public abstract void annotate(IDataCollection<DI> testData, AbstractDetector<DI, DET, RES> detector, AbstractResolver<DI, DET, RES> resolver) throws IOException;
}
