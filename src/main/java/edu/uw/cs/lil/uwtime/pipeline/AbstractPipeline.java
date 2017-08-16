package edu.uw.cs.lil.uwtime.pipeline;

import java.io.IOException;
import java.util.List;

import edu.uw.cs.lil.tiny.data.IDataItem;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.uwtime.analysis.AbstractAnalysis;
import edu.uw.cs.lil.uwtime.annotation.AbstractAnnotator;
import edu.uw.cs.lil.uwtime.detection.AbstractDetector;
import edu.uw.cs.lil.uwtime.resolution.AbstractResolver;

public abstract class AbstractPipeline<DI extends IDataItem<?>, DET extends IDataItem<?>, RES> extends Thread {
	final protected IDataCollection<DI> trainData;
	final protected IDataCollection<DI> testData;

	public AbstractPipeline(IDataCollection<DI> trainData, 
			IDataCollection<DI> testData) {
		this.trainData = trainData;
		this.testData = testData;
	}

	abstract AbstractDetector<DI, DET, RES> getDetector();
	abstract AbstractResolver<DI, DET, RES> getResolver();
	abstract AbstractAnalysis<DI, DET, RES> getAnalysis();
	abstract List<AbstractAnnotator<DI, DET, RES>> getAnnotators();

	@Override
	public void run(){
		getResolver().train(trainData);
		getDetector().train(trainData, getResolver());
		getAnalysis().analyze(getResolver().resolveMentions(getDetector().detectMentions(testData)));
		for (AbstractAnnotator<DI, DET, RES> annotator : getAnnotators())
			try {
				annotator.annotate(testData, getDetector(), getResolver());
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
}