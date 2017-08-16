package edu.uw.cs.lil.uwtime.annotation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphOutput;
import edu.uw.cs.lil.uwtime.chunking.AbstractChunkerOutput;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDataset;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import edu.uw.cs.lil.uwtime.data.TemporalSentence;
import edu.uw.cs.lil.uwtime.detection.AbstractDetector;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.resolution.AbstractResolver;
import edu.uw.cs.utils.composites.Pair;

public class TimeMLAnnotator extends AbstractAnnotator <TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>>{
	private final String outputDir;
	
	public TimeMLAnnotator (String outputDir) {
		this.outputDir = outputDir;
	}
	
	@Override
	public void annotate(
			IDataCollection<TemporalSentence> testData,
			AbstractDetector<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> detector,
			AbstractResolver<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> resolver) {
		TemporalDataset datasetByDocument = (TemporalDataset) testData;
		String datasetOutputDir = outputDir + datasetByDocument.getName() + "/";
		new File(datasetOutputDir).mkdirs();
		for (TemporalDocument document : datasetByDocument.getDocuments()) {
			List<Pair<TemporalSentence, TemporalJointChunk>> allPredictions = new LinkedList<Pair<TemporalSentence, TemporalJointChunk>>();
			for (Pair<TemporalSentence, List<Pair<AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>>>> sentenceOutputs : resolver.resolveMentions(detector.detectMentions(document))) {
				for (Pair<AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> outputs : sentenceOutputs.second()) {
					if (!outputs.second().getMaxDerivations().isEmpty())
						allPredictions.add(Pair.of(sentenceOutputs.first(), outputs.first().getChunk()));
				}
			}
			try {
				PrintStream ps = new PrintStream(new File(datasetOutputDir + document.getDocID() + ".tml"));
				ps.print(document.annotatePredictions(allPredictions));
				ps.close();
			} catch (FileNotFoundException e) {
				System.err.println("Unable to open file");
			}
		}
	}
}
