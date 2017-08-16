package edu.uw.cs.lil.uwtime.analysis;

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphDerivation;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphOutput;
import edu.uw.cs.lil.uwtime.chunking.AbstractChunkerOutput;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalSentence;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.resolution.TemporalResolver;
import edu.uw.cs.lil.uwtime.stats.EndToEndStatistics;
import edu.uw.cs.lil.uwtime.stats.TemporalStatistics;
import edu.uw.cs.utils.composites.Pair;

public class TemporalAnalysis extends AbstractAnalysis<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> {
	private final int fold;
	private final TemporalStatistics allStats;

	public enum Type {
		RELAXED,STRICT,ORACLE;
		@Override
		public String toString() {
			switch(this) {
			case RELAXED:
				return "relaxed";
			case STRICT:
				return "strict";
			case ORACLE:
				return "oracle";
			default:
				return null;
			}
		}
	};

	public TemporalAnalysis(TemporalStatistics allStats, int fold) {
		this.allStats = allStats;
		this.fold = fold;
	}

	@Override
	public void analyze(
			List<Pair<TemporalSentence, List<Pair<AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>>>>> allOutputs) {
		analyze(allOutputs, allStats.getRelaxed(), Type.RELAXED);
		analyze(allOutputs, allStats.getStrict(), Type.STRICT);
		analyze(allOutputs, allStats.getOracle(), Type.ORACLE);
	}

	public void analyze(
			List<Pair<TemporalSentence, List<Pair<AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>>>>> allOutputs,
			EndToEndStatistics stats,
			Type t) {
		for (Pair<TemporalSentence, List<Pair<AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>>>> sentenceOutputs : allOutputs) {
			TemporalSentence sentence = sentenceOutputs.first();
			List<TemporalJointChunk> remainingGoldChunks = new LinkedList<TemporalJointChunk>();
			for(TemporalJointChunk goldChunk : sentence.getLabel()) {
				remainingGoldChunks.add(goldChunk);
				stats.incrementGoldMentions(fold);
			}
			for (Pair<AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> outputs : sentenceOutputs.second()) {
				AbstractChunkerOutput<TemporalJointChunk> chunkerOutput = outputs.first();
				IJointGraphOutput<LogicalExpression, MentionResult> parserOutput = outputs.second();
				IJointGraphDerivation<LogicalExpression, MentionResult> maxDerivation = parserOutput.getMaxDerivations().isEmpty() ? null : parserOutput.getMaxDerivations().get(0);

				double detectionProbability = chunkerOutput.getClassifierOutput().getProbability(true);
				double derivationProbability = TemporalResolver.getDerivationProbability(maxDerivation, parserOutput);
				stats.incrementPredictedMentions(fold, detectionProbability, derivationProbability);
				TemporalJointChunk goldChunk = null;
				for (TemporalJointChunk candidateGoldChunk : remainingGoldChunks) {
					if (t == Type.STRICT ? 
							candidateGoldChunk.strictlyMatches(chunkerOutput.getChunk()):
								candidateGoldChunk.overlapsWith(chunkerOutput.getChunk())){
						goldChunk = candidateGoldChunk;
						break;
					}
				}

				if (goldChunk != null) {
					stats.incrementCorrectMentions(fold, detectionProbability, derivationProbability);
					remainingGoldChunks.remove(goldChunk);

					TemporalJointChunk predictedChunk = chunkerOutput.getChunk();
					MentionResult predictedResult = predictedChunk.getResult();
					MentionResult goldResult = goldChunk.getResult();
					if (predictedResult != null) {
						if (t == Type.ORACLE) {
							for (IJointGraphDerivation<LogicalExpression, MentionResult> derivation : parserOutput.getDerivations()) {
								if (goldResult.getValue().equals(derivation.getResult().getValue())) {
									stats.incrementCorrectValues(fold, detectionProbability, derivationProbability);	
									break;
								}
							}
						}
						else {
							if (goldResult.getValue().equals(predictedResult.getValue()))
								stats.incrementCorrectValues(fold, detectionProbability, derivationProbability);	
						}
					}
				}
			}
		}
	}
}
