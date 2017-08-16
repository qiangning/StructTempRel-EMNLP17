package edu.uw.cs.lil.uwtime.learn.temporal;

import edu.uw.cs.lil.uwtime.chunking.TemporalChunkerOutput;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryClassifier;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryClassifierOutput;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryModel;

public class TemporalClassifier implements IBinaryClassifier<TemporalChunkerOutput> {
	private double threshold;
	public TemporalClassifier (double threshold) {
		this.threshold = threshold;
	}
	@Override
	public IBinaryClassifierOutput<TemporalChunkerOutput> classify(
			TemporalChunkerOutput dataItem,
			IBinaryModel<TemporalChunkerOutput> model) {
		return new TemporalClassifierOutput(dataItem, model, this);
	}
	
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	public double getThreshold() {
		return threshold;
	}
}