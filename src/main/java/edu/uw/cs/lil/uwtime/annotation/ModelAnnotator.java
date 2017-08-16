package edu.uw.cs.lil.uwtime.annotation;

import java.io.File;
import java.io.IOException;

import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphOutput;
import edu.uw.cs.lil.uwtime.chunking.AbstractChunkerOutput;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalSentence;
import edu.uw.cs.lil.uwtime.detection.AbstractDetector;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.resolution.AbstractResolver;

public class ModelAnnotator extends AbstractAnnotator <TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>>{
	private final String dir;
	
	public ModelAnnotator(String dir) {
		this.dir = dir;
	}
	
	@Override
	public void annotate(
			IDataCollection<TemporalSentence> testData,
			AbstractDetector<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> detector,
			AbstractResolver<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> resolver) throws IOException {
		new File(dir).mkdirs();
		
		detector.saveModel(dir + "detection.ser");
		resolver.saveModel(dir + "resolution.ser");
	}
}