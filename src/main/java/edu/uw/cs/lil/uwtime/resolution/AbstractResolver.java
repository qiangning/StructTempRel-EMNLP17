package edu.uw.cs.lil.uwtime.resolution;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.utils.composites.Pair;

public abstract class AbstractResolver<DI extends IDataItem<?>, DET extends IDataItem<?>, RES> {
	public abstract void train(IDataCollection<DI> trainData);
	public abstract List<Pair<DI, List<Pair<DET, RES>>>> resolveMentions(List<Pair<DI, List<DET>>> outputs);
    public abstract IHashVector getModel();
	public abstract void setThreshold(double resolutionThreshold);
	public abstract double getThresold();
	public abstract void saveModel(String filename) throws IOException;
	public abstract void loadModel(InputStream is) throws IOException, ClassNotFoundException;
}