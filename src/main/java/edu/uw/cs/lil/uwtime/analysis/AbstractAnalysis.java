package edu.uw.cs.lil.uwtime.analysis;

import java.util.List;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.utils.composites.Pair;

public abstract class AbstractAnalysis<DI extends IDataItem<?>, DET, RES> {
	public abstract void analyze(List<Pair<DI, List<Pair<DET, RES>>>> outputs);
}
