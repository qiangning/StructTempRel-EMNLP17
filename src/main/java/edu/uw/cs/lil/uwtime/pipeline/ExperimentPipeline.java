package edu.uw.cs.lil.uwtime.pipeline;

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphOutput;
import edu.uw.cs.lil.uwtime.analysis.AbstractAnalysis;
import edu.uw.cs.lil.uwtime.analysis.TemporalAnalysis;
import edu.uw.cs.lil.uwtime.annotation.AbstractAnnotator;
import edu.uw.cs.lil.uwtime.annotation.ModelAnnotator;
import edu.uw.cs.lil.uwtime.annotation.TimeMLAnnotator;
import edu.uw.cs.lil.uwtime.chunking.AbstractChunkerOutput;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDataset;
import edu.uw.cs.lil.uwtime.data.TemporalSentence;
import edu.uw.cs.lil.uwtime.detection.AbstractDetector;
import edu.uw.cs.lil.uwtime.detection.GoldDetector;
import edu.uw.cs.lil.uwtime.detection.TemporalDetector;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.parsing.grammar.TemporalGrammar;
import edu.uw.cs.lil.uwtime.resolution.AbstractResolver;
import edu.uw.cs.lil.uwtime.resolution.TemporalResolver;
import edu.uw.cs.lil.uwtime.stats.TemporalStatistics;
import edu.uw.cs.lil.uwtime.utils.TemporalConfig;

public class ExperimentPipeline extends AbstractPipeline<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> {
	private final AbstractDetector<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> detector;
	private final AbstractResolver<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> resolver;
	private final List<AbstractAnnotator<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>>> annotators;
	private final AbstractAnalysis<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> analysis;

	public ExperimentPipeline(TemporalDataset trainData, TemporalDataset testData, TemporalStatistics stats, int fold) {
		super(trainData, testData);
		TemporalGrammar grammar = new TemporalGrammar();
		detector = TemporalConfig.getInstance().goldMentions ? new GoldDetector() : new TemporalDetector(grammar);
		resolver = new TemporalResolver(grammar); 
		analysis = new TemporalAnalysis(stats, fold);
		annotators = new LinkedList<AbstractAnnotator<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>>>();
		annotators.add(new TimeMLAnnotator(TemporalConfig.getInstance().outputDir));
		annotators.add(new ModelAnnotator(TemporalConfig.getInstance().modelDir + TemporalConfig.getInstance().name + "/"));
	}

	@Override
	public AbstractDetector<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> getDetector() {
		return detector;
	}

	@Override
	public AbstractResolver<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> getResolver() {
		return resolver;
	}

	@Override
	public List<AbstractAnnotator<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>>> getAnnotators() {
		return annotators;
	}

	@Override
	public AbstractAnalysis<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> getAnalysis() {
		return analysis;
	}
}
