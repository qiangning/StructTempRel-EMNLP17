package edu.uw.cs.lil.uwtime.chunking;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDateTime;

import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYDerivation;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.cky.chart.Cell;
import edu.uw.cs.lil.uwtime.data.TemporalSentence;
import edu.uw.cs.lil.uwtime.eval.TemporalEvaluation;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalEntity;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryClassifierOutput;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryModel;
import edu.uw.cs.lil.uwtime.learn.temporal.TemporalClassifier;
import edu.uw.cs.lil.uwtime.learn.temporal.TemporalExecutionHistory;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext.ShiftGranularity;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext.TemporalDirection;
import edu.uw.cs.lil.uwtime.parsing.grammar.TemporalGrammar;
import edu.uw.cs.utils.composites.Pair;

public class TemporalChunker {
	final private TemporalGrammar grammar;
	final private TemporalClassifier classifier;
	final private IBinaryModel<TemporalChunkerOutput> filteringModel;

	public TemporalChunker(TemporalGrammar grammar,
			TemporalClassifier classifier,
			IBinaryModel<TemporalChunkerOutput> filteringModel) {
		this.grammar = grammar;
		this.classifier = classifier;
		this.filteringModel = filteringModel;
	}
	
	/*
	 * First list is filtered. Second list is unfiltered.
	 */
	public Pair<List<TemporalChunkerOutput>,List<TemporalChunkerOutput>> chunk(TemporalSentence sentence) {		
		Map<Pair<Integer, Integer>,TemporalChunkerOutput> chunkMap = 
				new HashMap<Pair<Integer, Integer>,TemporalChunkerOutput>();

		Sentence baseSentence = sentence.getSample().first();
		CKYParserOutput<LogicalExpression> parserOutput = grammar.getParser().parse(baseSentence, grammar.getModel().createDataItemModel(baseSentence));

		for (Cell<LogicalExpression> c : parserOutput.getChart()) {
			if (grammar.getFilter().isValid(c.getCategory().getSem())) {
				TemporalEntity entity = contextIndependentExecution(c.getCategory().getSem(), sentence.getReferenceTime());
				if(entity != null) {
					Pair<Integer, Integer> span = Pair.of(c.getStart(), c.getEnd());
					TemporalChunkerOutput output;
					if (!chunkMap.containsKey(span)) {
						output = new TemporalChunkerOutput(sentence, span, entity, classifier, filteringModel);
						chunkMap.put(span, output);
					}
					else {
						output = chunkMap.get(span);
					}
					output.addDerivation(new CKYDerivation<LogicalExpression>(c));
				}
			}
		}

		List<TemporalChunkerOutput> filteredOutputList = new LinkedList<TemporalChunkerOutput>();
		List<TemporalChunkerOutput> unfilteredOutputList = new LinkedList<TemporalChunkerOutput>();
		for(TemporalChunkerOutput output : chunkMap.values()) {
			IBinaryClassifierOutput<TemporalChunkerOutput> classifierOutput = 
					classifier.classify(output, filteringModel);
			if (classifierOutput.getBinaryClass())
				filteredOutputList.add(output);
			unfilteredOutputList.add(output);
		}
		return Pair.of(suppressChunks(filteredOutputList), unfilteredOutputList);
	}
	private TemporalEntity contextIndependentExecution(LogicalExpression l, LocalDateTime jodaTime) {
		Object obj = TemporalEvaluation.of(l, new TemporalContext(
				TemporalDirection.NONE, 
				new TemporalSequence(jodaTime), 
				null, 
				ShiftGranularity.NONE), new TemporalExecutionHistory(), grammar.getConstants(), grammar.getCategoryServices());
		return obj instanceof TemporalEntity ? (TemporalEntity) obj : null;
	}

	public static List<TemporalChunkerOutput> suppressChunks(List<TemporalChunkerOutput> outputs) {
		List<TemporalChunkerOutput> suppressedOutput = new LinkedList<TemporalChunkerOutput>();
		Collections.sort(outputs, new Comparator<TemporalChunkerOutput>() {
			@Override
			public int compare(TemporalChunkerOutput arg0,
					TemporalChunkerOutput arg1) {
				int size1 = arg1.getChunk().getSpanSize();
				int size0 = arg0.getChunk().getSpanSize();
				if (size1 != size0 ||
						arg1.getClassifierOutput() == null ||
						arg0.getClassifierOutput() == null)
					return Integer.valueOf(size1).compareTo(size0);
				else
					return Double.valueOf(arg1.getClassifierOutput().getScore())
							.compareTo(arg0.getClassifierOutput().getScore());
			}
		});
		for(TemporalChunkerOutput output : outputs) {
			boolean foundOverlap = false;
			for (TemporalChunkerOutput existingOutput : suppressedOutput) {
				if (output.getChunk().overlapsWith(existingOutput.getChunk())) {
					foundOverlap = true;
					break;
				}
			}
			if (!foundOverlap) {
				suppressedOutput.add(output);
			}
		}
		sortChunks(suppressedOutput);
		return suppressedOutput;
	}

	private static void sortChunks(List<TemporalChunkerOutput> outputs) {
		Collections.sort(outputs, new Comparator<TemporalChunkerOutput>() {
			@Override
			public int compare(TemporalChunkerOutput arg0,
					TemporalChunkerOutput arg1) {
				return Integer.valueOf(arg0.getChunk().getStart()).compareTo(arg1.getChunk().getStart());
			}
		});
	}
}
