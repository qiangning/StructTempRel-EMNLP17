package edu.uw.cs.lil.uwtime.resolution;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.situated.ISituatedDataItem;
import edu.uw.cs.lil.tiny.data.utils.IValidator;
import edu.uw.cs.lil.tiny.learn.ILearner;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphDerivation;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphOutput;
import edu.uw.cs.lil.tiny.parser.joint.model.IJointDataItemModel;
import edu.uw.cs.lil.tiny.parser.joint.model.JointModel;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.uwtime.chunking.AbstractChunkerOutput;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalChunkDataset;
import edu.uw.cs.lil.uwtime.data.TemporalSentence;
import edu.uw.cs.lil.uwtime.learn.SituatedValidationAdaptiveStocGrad;
import edu.uw.cs.lil.uwtime.learn.featuresets.resolution.DirectionStructureFeatureSet;
import edu.uw.cs.lil.uwtime.learn.featuresets.resolution.DistanceFeatureSet;
import edu.uw.cs.lil.uwtime.learn.featuresets.resolution.EntityStructureFeatureSet;
import edu.uw.cs.lil.uwtime.learn.featuresets.resolution.GovernorVerbFeatureSet;
import edu.uw.cs.lil.uwtime.learn.featuresets.resolution.GranularityFeatureSet;
import edu.uw.cs.lil.uwtime.learn.featuresets.resolution.IntersectionsFeatureSet;
import edu.uw.cs.lil.uwtime.learn.featuresets.resolution.ReferenceTypeFeatureSet;
import edu.uw.cs.lil.uwtime.learn.featuresets.resolution.WordingSkippingFeatureSet;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.parsing.TemporalJointParser;
import edu.uw.cs.lil.uwtime.parsing.grammar.TemporalGrammar;
import edu.uw.cs.lil.uwtime.utils.TemporalConfig;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;
import edu.uw.cs.utils.composites.Pair;

public class TemporalResolver extends AbstractResolver<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> {
	private final static KeyArgs THRESHOLD_KEY = new KeyArgs("resolution_threshold");
	private final TemporalGrammar grammar;
	private final TemporalJointParser jointParser;
	private final JointModel<ISituatedDataItem<Sentence, TemporalJointChunk>, LogicalExpression, MentionResult> parsingModel;
	private final IValidator<TemporalJointChunk, MentionResult> resolutionValidator;
	private double resolutionThreshold;

	public TemporalResolver(TemporalGrammar grammar) {
		this.grammar = grammar;
		this.parsingModel = createParsingModel();
		this.jointParser = new TemporalJointParser(grammar);
		this.resolutionValidator = new IValidator<TemporalJointChunk, MentionResult> () {
			@Override
			public boolean isValid(TemporalJointChunk dataItem,	MentionResult label) {
				return label.equals(dataItem.getResult());
			}
		};
		this.resolutionThreshold = 0;
	}

	private JointModel<ISituatedDataItem<Sentence, TemporalJointChunk>, LogicalExpression, MentionResult> createParsingModel() {
		JointModel<ISituatedDataItem<Sentence, TemporalJointChunk>, LogicalExpression, MentionResult> model =
				new JointModel.Builder<ISituatedDataItem<Sentence, TemporalJointChunk>, LogicalExpression, MentionResult>()
				.addJointFeatureSet(new GovernorVerbFeatureSet())
				.addJointFeatureSet(new DistanceFeatureSet())
				.addJointFeatureSet(new GranularityFeatureSet())
				.addJointFeatureSet(new ReferenceTypeFeatureSet())
				.addJointFeatureSet(new EntityStructureFeatureSet())
				.addJointFeatureSet(new DirectionStructureFeatureSet())
				.addJointFeatureSet(new IntersectionsFeatureSet())
				.addLexicalFeatureSet(new WordingSkippingFeatureSet(grammar.getCategoryServices().getEmptyCategory(), -1000.0))
				.setLexicon(grammar.getLexicon())
				.build();
		model.addLexEntries(grammar.getLexicon().toCollection());
		return model;
	}

	@Override
	public void train(IDataCollection<TemporalSentence> trainData) {
		TemporalLog.printf ("progress","Learning parsing model from %d samples...\n\n", trainData.size());
		ILearner<ISituatedDataItem<Sentence, TemporalJointChunk>, TemporalJointChunk, JointModel<ISituatedDataItem<Sentence, TemporalJointChunk>, LogicalExpression, MentionResult>> resolutionLearner;

		resolutionLearner = new SituatedValidationAdaptiveStocGrad.Builder<ISituatedDataItem<Sentence, TemporalJointChunk>, LogicalExpression, MentionResult, MentionResult, TemporalJointChunk>(TemporalChunkDataset.getGoldChunkDataset(trainData), jointParser, resolutionValidator)
				.setAlpha0(0.25)
				.setC(0.0)
				.setNumIterations(5)
				.build();

		resolutionLearner.train(parsingModel);
	}

	@Override
	public List<Pair<TemporalSentence, List<Pair<AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>>>>> resolveMentions(
			List<Pair<TemporalSentence, List<AbstractChunkerOutput<TemporalJointChunk>>>> outputs) {
		jointParser.resetTemporalAnchors();
		List<Pair<TemporalSentence, List<Pair<AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>>>>> allResolvedOutputs = 
				new LinkedList<Pair<TemporalSentence, List<Pair<AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>>>>>();
		for (Pair<TemporalSentence, List<AbstractChunkerOutput<TemporalJointChunk>>> sentenceOutputs : outputs) {
			List<Pair<AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>>> resolvedSentenceOutputs =
					new LinkedList<Pair<AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>>>();
			for (AbstractChunkerOutput<TemporalJointChunk> detectionOutput : sentenceOutputs.second()) {
				IJointDataItemModel<LogicalExpression, MentionResult> parsingDataItemModel = parsingModel.createJointDataItemModel(detectionOutput.getChunk().getSample());				
				IJointGraphOutput<LogicalExpression, MentionResult> parserOutput = jointParser.parse(detectionOutput.getChunk().getSample(), parsingDataItemModel, TemporalConfig.getInstance().goldMentions ? true : false);
				IJointGraphDerivation<LogicalExpression, MentionResult> maxDerivation = parserOutput.getMaxDerivations().isEmpty() ? null : parserOutput.getMaxDerivations().get(0);
				if (maxDerivation != null)
					detectionOutput.getChunk().setResult(maxDerivation.getResult());
				if (TemporalConfig.getInstance().goldMentions || getDerivationProbability(maxDerivation, parserOutput) >= resolutionThreshold)
					resolvedSentenceOutputs.add(Pair.of(detectionOutput, parserOutput));
			}
			allResolvedOutputs.add(Pair.of(sentenceOutputs.first(), resolvedSentenceOutputs));
		}
		return allResolvedOutputs;
	}

	@Override
	public IHashVector getModel() {
		return parsingModel.getTheta();
	}

	@Override
	public void setThreshold(double resolutionThreshold) {
		this.resolutionThreshold = resolutionThreshold;
	}

	@Override
	public double getThresold() {
		return resolutionThreshold;
	}

	public static double getDerivationProbability(IJointGraphDerivation<LogicalExpression, MentionResult> derivation,
			IJointGraphOutput<LogicalExpression, MentionResult> parserOutput) {
		return derivation == null || parserOutput.logNorm() == Double.NEGATIVE_INFINITY ? 0.0 : Math.exp(derivation.getLogInsideScore() - parserOutput.logNorm());
	}

	@Override
	public void saveModel(String filename) throws IOException {
		parsingModel.getTheta().set(THRESHOLD_KEY, resolutionThreshold);
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
		out.writeObject(parsingModel.getTheta());
		out.close();
	}

	@Override
	public void loadModel(InputStream is) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(is);
		parsingModel.getTheta().clear();
		((IHashVector) ois.readObject()).addTimesInto(1.0, parsingModel.getTheta());
		resolutionThreshold = parsingModel.getTheta().get(THRESHOLD_KEY);
		ois.close();
	}
}
