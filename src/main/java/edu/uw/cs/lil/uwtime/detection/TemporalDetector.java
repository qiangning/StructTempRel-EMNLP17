package edu.uw.cs.lil.uwtime.detection;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;

import de.bwaldvogel.liblinear.SolverType;
import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.data.utils.IValidator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphOutput;
import edu.uw.cs.lil.uwtime.learn.binary.learner.AbstractBinaryLearner;
import edu.uw.cs.lil.uwtime.learn.binary.learner.LiblinearLearner;
import edu.uw.cs.lil.uwtime.learn.featuresets.detection.AgeReferenceFeatureSet;
import edu.uw.cs.lil.uwtime.learn.featuresets.detection.DeterminerFeatureSet;
import edu.uw.cs.lil.uwtime.learn.featuresets.detection.EntityStructureDetectionFeatureSet;
import edu.uw.cs.lil.uwtime.learn.featuresets.detection.LexicalPOSFeatureSet;
import edu.uw.cs.lil.uwtime.learn.featuresets.detection.QuotationFeatureSet;
import edu.uw.cs.lil.uwtime.learn.featuresets.detection.TemporalLexicalOriginFeatureSet;
import edu.uw.cs.lil.uwtime.learn.featuresets.detection.TimePrepositionsFeatureSet;
import edu.uw.cs.lil.uwtime.resolution.AbstractResolver;
import edu.uw.cs.lil.uwtime.stats.EndToEndStatistics;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.uwtime.analysis.TemporalAnalysis;
import edu.uw.cs.lil.uwtime.chunking.AbstractChunkerOutput;
import edu.uw.cs.lil.uwtime.chunking.TemporalChunker;
import edu.uw.cs.lil.uwtime.chunking.TemporalChunkerOutput;
import edu.uw.cs.lil.uwtime.chunking.TemporalChunkerOutputDataset;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalSentence;
import edu.uw.cs.lil.uwtime.learn.binary.IBinaryModel;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.learn.temporal.TemporalBinaryModel;
import edu.uw.cs.lil.uwtime.learn.temporal.TemporalClassifier;
import edu.uw.cs.lil.uwtime.parsing.grammar.TemporalGrammar;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.composites.Triplet;

public class TemporalDetector extends AbstractDetector<TemporalSentence ,AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> {
	private final static KeyArgs THRESHOLD_KEY = new KeyArgs("detection_threshold");
	private final TemporalClassifier classifier;
	private final TemporalChunker chunker;
	private final IBinaryModel<TemporalChunkerOutput> filteringModel;
	private final IValidator<TemporalChunkerOutput, Boolean> strictValidator;
	public TemporalDetector(TemporalGrammar grammar) {
		this.classifier = new TemporalClassifier(0.5);
		
		this.filteringModel = new TemporalBinaryModel.Builder()
		.addFeatureSet(new LexicalPOSFeatureSet())
		.addFeatureSet(new QuotationFeatureSet(2))
		.addFeatureSet(new TemporalLexicalOriginFeatureSet())
		.addFeatureSet(new TimePrepositionsFeatureSet())
		.addFeatureSet(new AgeReferenceFeatureSet())
		.addFeatureSet(new DeterminerFeatureSet())
		.addFeatureSet(new EntityStructureDetectionFeatureSet())
		.build();

		this.chunker = new TemporalChunker(grammar, classifier, filteringModel);

		new IValidator<TemporalChunkerOutput, Boolean>() {
			@Override
			public boolean isValid(TemporalChunkerOutput dataItem, Boolean label) {
				return dataItem.getChunk().isRelaxedMatch() == label;
			}
		};

		this.strictValidator = new IValidator<TemporalChunkerOutput, Boolean>() {
			@Override
			public boolean isValid(TemporalChunkerOutput dataItem, Boolean label) {
				return dataItem.getChunk().isStrictMatch() == label;
			}
		};
	}

	private TemporalChunkerOutputDataset createChunkingDataset(IDataCollection<TemporalSentence> dataset) {
		TemporalChunkerOutputDataset outputDataset = new TemporalChunkerOutputDataset();

		int count = 0;
		long startTime = System.currentTimeMillis();
		for (TemporalSentence sentence : dataset) {
			if (count % 100 == 0)
				TemporalLog.printf ("progress","Chunking %d/%d sentences...\n\n", count + 1, dataset.size());
			List<TemporalChunkerOutput> sentenceOutputs = chunker.chunk(sentence).second();
            List<TemporalChunkerOutput> goodExamples = new LinkedList<TemporalChunkerOutput>();
			for (TemporalChunkerOutput chunkOutput : sentenceOutputs)
				if (!chunkOutput.getChunk().isStrictSubmentionMatch())
				    goodExamples.add(chunkOutput);
			outputDataset.addChunks(sentence, goodExamples);
			count++;
		}

		TemporalLog.printf ("progress", "%.3fs per sentence parse\n", (System.currentTimeMillis() - startTime)/(1000.0 * count));
		return outputDataset;
	}

	@Override
	public void train(IDataCollection<TemporalSentence> trainData,
			AbstractResolver<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> resolver) {
		TemporalChunkerOutputDataset outputDataset = createChunkingDataset(trainData);
		TemporalLog.printf ("progress","Learning detection model from %d samples...\n\n", trainData.size());
		AbstractBinaryLearner<TemporalChunkerOutput> detectionLearner;
		detectionLearner = 
				new LiblinearLearner.Builder<TemporalChunkerOutput>(outputDataset, strictValidator)
				.setC(10.0)
				.setEps(0.01)
				.setSolverType(SolverType.L1R_LR)
				.build();
		detectionLearner.train(filteringModel);
		Pair<Double, List<Pair<TemporalSentence, List<AbstractChunkerOutput<TemporalJointChunk>>>>> bestDetection = getBestDetectionThreshold(outputDataset);
		classifier.setThreshold(bestDetection.first());
		setBestResolutionThreshold(bestDetection.second(), resolver);
	}

	private Pair<Double,List<Pair<TemporalSentence, List<AbstractChunkerOutput<TemporalJointChunk>>>>> getBestDetectionThreshold(TemporalChunkerOutputDataset outputDataset) {
		double bestP = -1;
		double bestF1 = -1;
		List<Pair<TemporalSentence, List<AbstractChunkerOutput<TemporalJointChunk>>>> bestOutputs = null;

		for (double p = 0.0 ; p < 1.0 ; p += 0.01) {
			EndToEndStatistics stats = new EndToEndStatistics();
			List<Pair<TemporalSentence, List<AbstractChunkerOutput<TemporalJointChunk>>>> currentOutputs =
					new LinkedList<Pair<TemporalSentence, List<AbstractChunkerOutput<TemporalJointChunk>>>>();
			for (Pair<TemporalSentence, List<TemporalChunkerOutput>> sentenceOutputs : outputDataset.getOutputsBySentence()) {
				List<TemporalJointChunk> remainingGoldChunks = new LinkedList<TemporalJointChunk>();
				for(TemporalJointChunk goldChunk : sentenceOutputs.first().getLabel()) {
					remainingGoldChunks.add(goldChunk);
					stats.incrementGoldMentions(0);
				}
				List<TemporalChunkerOutput> thresholdedOutputs = new LinkedList<TemporalChunkerOutput>();
				for (TemporalChunkerOutput output : sentenceOutputs.second()) {
					if (output.getClassifierOutput().getProbability(true) >= p)
						thresholdedOutputs.add(output);
				}
				List<AbstractChunkerOutput<TemporalJointChunk>> filteredSentenceOutputs =
						new LinkedList<AbstractChunkerOutput<TemporalJointChunk>>();
				for (TemporalChunkerOutput suppressedOutput : TemporalChunker.suppressChunks(thresholdedOutputs)) {
					stats.incrementPredictedMentions(0, 1.0, 1.0);
					filteredSentenceOutputs.add(suppressedOutput);
					for (TemporalJointChunk candidateGoldChunk : remainingGoldChunks) {
						if (candidateGoldChunk.strictlyMatches(suppressedOutput.getChunk())) {
							stats.incrementCorrectMentions(0, 1.0, 1.0);
							remainingGoldChunks.remove(candidateGoldChunk);
							break;
						}
					}
				}
				currentOutputs.add(Pair.of(sentenceOutputs.first(), filteredSentenceOutputs));
			}
			double f1 = stats.getF1(Triplet.of(0, 0.0, 0.0));
			if (f1 > bestF1) {
				bestF1 = f1;
				bestP = p;
				bestOutputs = currentOutputs;
			}
		}
		return Pair.of(bestP, bestOutputs);
	}

	private void setBestResolutionThreshold(
			List<Pair<TemporalSentence, List<AbstractChunkerOutput<TemporalJointChunk>>>> bestOutputs,
			AbstractResolver<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> resolver) {
		EndToEndStatistics stats = new EndToEndStatistics();
		TemporalAnalysis analysis = new TemporalAnalysis(null, 0);
		analysis.analyze(resolver.resolveMentions(bestOutputs), stats, TemporalAnalysis.Type.RELAXED);
		resolver.setThreshold(stats.searchThreshold(false));
	}

	@Override
	public List<Pair<TemporalSentence, List<AbstractChunkerOutput<TemporalJointChunk>>>> detectMentions(IDataCollection<TemporalSentence> testData) {
		TemporalLog.printf ("progress","Testing %d samples...\n\n", testData.size());

		List<Pair<TemporalSentence, List<AbstractChunkerOutput<TemporalJointChunk>>>> allOutputs = new LinkedList<Pair<TemporalSentence, List<AbstractChunkerOutput<TemporalJointChunk>>>>();
		for (TemporalSentence sentence : testData) {
			List<AbstractChunkerOutput<TemporalJointChunk>> sentenceOutputs = 
					new LinkedList<AbstractChunkerOutput<TemporalJointChunk>>();
			Pair<List<TemporalChunkerOutput>, List<TemporalChunkerOutput>> detectionOutput = chunker.chunk(sentence);
			for (TemporalChunkerOutput filteredOutput : detectionOutput.first())
				sentenceOutputs.add(filteredOutput);
			allOutputs.add(Pair.of(sentence, sentenceOutputs));
		}
		return allOutputs;
	}

	@Override
	public IHashVector getModel() {
		return filteringModel.getTheta();
	}

	@Override
	public void saveModel(String filename) throws IOException {
		filteringModel.getTheta().set(THRESHOLD_KEY, classifier.getThreshold());
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
		out.writeObject(filteringModel.getTheta());
		out.close();
	}

	@Override
	public void loadModel(InputStream is) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(is);
		filteringModel.getTheta().clear();
		((IHashVector) ois.readObject()).addTimesInto(1.0, filteringModel.getTheta());
		classifier.setThreshold(filteringModel.getTheta().get(THRESHOLD_KEY));
		ois.close();
	}
}
