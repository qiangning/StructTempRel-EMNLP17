package edu.uw.cs.lil.uwtime.parsing;

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.situated.ISituatedDataItem;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYDerivation;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYParserOutput;
import edu.uw.cs.lil.tiny.parser.ccg.model.IDataItemModel;
import edu.uw.cs.lil.tiny.parser.graph.IGraphDerivation;
import edu.uw.cs.lil.tiny.parser.graph.IGraphParserOutput;
import edu.uw.cs.lil.tiny.parser.joint.IEvaluation;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphParser;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphOutput;
import edu.uw.cs.lil.tiny.parser.joint.graph.JointGraphOutput;
import edu.uw.cs.lil.tiny.parser.joint.injective.graph.DeterministicEvalResultWrapper;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointDataItemModel;
import edu.uw.cs.lil.uwtime.chunking.chunks.Chunk;
import edu.uw.cs.lil.uwtime.chunking.chunks.IChunk;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalSentence;
import edu.uw.cs.lil.uwtime.eval.TemporalEvaluation;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalEntity;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.learn.temporal.TemporalExecutionHistory;
import edu.uw.cs.lil.uwtime.parsing.grammar.TemporalGrammar;
import edu.uw.cs.lil.uwtime.utils.TemporalConfig;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.filter.IFilter;

public class TemporalJointParser implements IJointGraphParser<ISituatedDataItem<Sentence, TemporalJointChunk>, LogicalExpression, MentionResult, MentionResult> {
	private final LinkedList<Pair<TemporalSequence,TemporalJointChunk>> temporalAnchors;
	private final TemporalGrammar grammar;

	public TemporalJointParser(TemporalGrammar grammar) {
		this.grammar = grammar;
		temporalAnchors = new LinkedList<Pair<TemporalSequence,TemporalJointChunk>>();
	}

	private List<TemporalContext> getContexts(LogicalExpression l, Pair<TemporalSequence,TemporalJointChunk> nearestAnchor, TemporalSentence sentence) {
		List<Pair<TemporalSequence, TemporalJointChunk>> references = new LinkedList<Pair<TemporalSequence, TemporalJointChunk>>();
		references.add(Pair.of((TemporalSequence) null, (TemporalJointChunk) null));
		references.add(Pair.of(new TemporalSequence(sentence.getReferenceTime()), (TemporalJointChunk) null));
		if (nearestAnchor != null && TemporalConfig.getInstance().useContext)
			references.add(nearestAnchor);
		return TemporalContext.getPossibleContexts(l, references);
	}

	private MentionResult resolveChunk (TemporalSentence sentence, IChunk<LogicalExpression> baseChunk, TemporalJointChunk originalChunk, TemporalContext context) {
		TemporalExecutionHistory executionHistory = new TemporalExecutionHistory();
		Object evaluationOutput = TemporalEvaluation.of(baseChunk.getDerivation().getSemantics(), context, executionHistory, grammar.getConstants(), grammar.getCategoryServices());

		if (evaluationOutput == null || !(evaluationOutput instanceof TemporalEntity))
			return null;

		TemporalEntity groundedEntity = (TemporalEntity) evaluationOutput;
		String type = groundedEntity.getType();
		String value = groundedEntity.getValue();
		String mod = groundedEntity.getMod();

		if (value == null)
			return null;

		return new MentionResult(originalChunk, type, value, mod, baseChunk.getDerivation(), context, executionHistory, groundedEntity);
	}

	private List<MentionResult> resolveChunk (TemporalSentence sentence, 
			IChunk<LogicalExpression> baseChunk, 
			TemporalJointChunk originalChunk, 
			Pair<TemporalSequence,TemporalJointChunk> nearestAnchor) {
		List<MentionResult> results = new LinkedList<MentionResult>();
		for (TemporalContext context : getContexts(baseChunk.getDerivation().getSemantics(), nearestAnchor, sentence)) {
			MentionResult result = resolveChunk(sentence, baseChunk, originalChunk, context);
			if (result != null)
				results.add(result);
		}
		return results;
	}

	public void resetTemporalAnchors() {
		temporalAnchors.clear();
	}

	@Override
	public IJointGraphOutput<LogicalExpression, MentionResult> parse(
			ISituatedDataItem<Sentence, TemporalJointChunk> dataItem,
			IJointDataItemModel<LogicalExpression, MentionResult> model,
			boolean allowWordSkipping, ILexicon<LogicalExpression> tempLexicon,
			Integer beamSize) {

		TemporalJointChunk chunk = dataItem.getState();
		TemporalSentence sentence = chunk.getSentence();
		Pair<TemporalSequence,TemporalJointChunk> nearestAnchor = null;
		for (Pair<TemporalSequence,TemporalJointChunk> anchor : temporalAnchors) {
			if (!sentence.getDocID().equals(anchor.second().getSentence().getDocID())) {
				temporalAnchors.clear();
				break;
			}
			else if (anchor.second().getSentence() != sentence || !anchor.second().overlapsWith(chunk)) {
				nearestAnchor = anchor;
				break;
			}
		}

		CKYParserOutput<LogicalExpression> baseParserOutput = grammar.getParser().parse(dataItem.getSample().getSample(), model, allowWordSkipping);

		JointGraphOutput.Builder<LogicalExpression, MentionResult> jointOutputBuilder = 
				new JointGraphOutput.Builder<LogicalExpression, MentionResult>(baseParserOutput, 0)
				.setExactEvaluation(true);

		for (CKYDerivation<LogicalExpression> baseParse : baseParserOutput.getAllParses()) {
			if (grammar.getFilter().isValid(baseParse.getSemantics())) {
				IChunk<LogicalExpression> newBaseChunk = new Chunk<LogicalExpression> (chunk.getBaseChunk().getStart(), chunk.getBaseChunk().getEnd(), baseParse);
				for(MentionResult result : resolveChunk(sentence, newBaseChunk, chunk, nearestAnchor)){
					jointOutputBuilder.addInferencePair(Pair.of((IGraphDerivation<LogicalExpression>) baseParse, (IEvaluation<MentionResult>) new DeterministicEvalResultWrapper<MentionResult>(model,result)));
				}
			}
		}
		JointGraphOutput<LogicalExpression, MentionResult> jointOutput = jointOutputBuilder.build();
		if (!jointOutput.getMaxDerivations().isEmpty()) {
			MentionResult bestResult = jointOutput.getMaxDerivations().get(0).getResult();
			if (bestResult.getEntity() instanceof TemporalSequence) {
				TemporalSequence sequence = (TemporalSequence) bestResult.getEntity();
				if (sequence.isRange())
					temporalAnchors.push(Pair.of(sequence,chunk));
			}
		}
		return jointOutput;
	}

	@Override
	public IJointGraphOutput<LogicalExpression, MentionResult> parse(
			ISituatedDataItem<Sentence, TemporalJointChunk> dataItem,
			IJointDataItemModel<LogicalExpression, MentionResult> model) {
		return parse(dataItem, model, false);
	}

	@Override
	public IJointGraphOutput<LogicalExpression, MentionResult> parse(
			ISituatedDataItem<Sentence, TemporalJointChunk> dataItem,
			IJointDataItemModel<LogicalExpression, MentionResult> model,
			boolean allowWordSkipping) {
		return parse(dataItem, model, allowWordSkipping, null);
	}

	@Override
	public IJointGraphOutput<LogicalExpression, MentionResult> parse(
			ISituatedDataItem<Sentence, TemporalJointChunk> dataItem,
			IJointDataItemModel<LogicalExpression, MentionResult> model,
			boolean allowWordSkipping, ILexicon<LogicalExpression> tempLexicon) {
		return parse(dataItem, model, allowWordSkipping, tempLexicon, -1);
	}

	@Override
	public IGraphParserOutput<LogicalExpression> parse(ISituatedDataItem<Sentence, TemporalJointChunk> dataItem, 
			IDataItemModel<LogicalExpression> model) {
		return parse(dataItem, model, false);
	}

	@Override
	public IGraphParserOutput<LogicalExpression> parse(ISituatedDataItem<Sentence, TemporalJointChunk> dataItem, 
			IDataItemModel<LogicalExpression> model,
			boolean allowWordSkipping) {
		return parse(dataItem, model, allowWordSkipping, null);
	}

	@Override
	public IGraphParserOutput<LogicalExpression> parse(ISituatedDataItem<Sentence, TemporalJointChunk> dataItem, 
			IDataItemModel<LogicalExpression> model,
			boolean allowWordSkipping, ILexicon<LogicalExpression> tempLexicon) {
		return parse(dataItem, model, allowWordSkipping, tempLexicon, null);
	}

	@Override
	public IGraphParserOutput<LogicalExpression> parse(ISituatedDataItem<Sentence, TemporalJointChunk> dataItem, 
			IDataItemModel<LogicalExpression> model,
			boolean allowWordSkipping, ILexicon<LogicalExpression> tempLexicon,
			Integer beamSize) {
		return parse(dataItem, null, model, allowWordSkipping, tempLexicon,
				beamSize);
	}

	@Override
	public IGraphParserOutput<LogicalExpression> parse(ISituatedDataItem<Sentence, TemporalJointChunk> dataItem, 
			IFilter<LogicalExpression> pruningFilter,
			IDataItemModel<LogicalExpression> model) {
		return parse(dataItem, pruningFilter, model, false);
	}

	@Override
	public IGraphParserOutput<LogicalExpression> parse(ISituatedDataItem<Sentence, TemporalJointChunk> dataItem, 
			IFilter<LogicalExpression> pruningFilter,
			IDataItemModel<LogicalExpression> model, boolean allowWordSkipping) {
		return parse(dataItem, pruningFilter, model, allowWordSkipping, null);
	}

	@Override
	public IGraphParserOutput<LogicalExpression> parse(ISituatedDataItem<Sentence, TemporalJointChunk> dataItem, 
			IFilter<LogicalExpression> pruningFilter,
			IDataItemModel<LogicalExpression> model, boolean allowWordSkipping,
			ILexicon<LogicalExpression> tempLexicon) {
		return parse(dataItem, pruningFilter, model, allowWordSkipping, tempLexicon, -1);
	}

	@Override
	public IGraphParserOutput<LogicalExpression> parse(
			ISituatedDataItem<Sentence, TemporalJointChunk> dataItem,
			IFilter<LogicalExpression> pruningFilter,
			IDataItemModel<LogicalExpression> model, boolean allowWordSkipping,
			ILexicon<LogicalExpression> tempLexicon, Integer beamSize) {
		return grammar.getParser().parse(dataItem.getSample(), model, allowWordSkipping);
	}
}
