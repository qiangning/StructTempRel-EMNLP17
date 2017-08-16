package edu.uw.cs.lil.uwtime.detection;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.data.collection.IDataCollection;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.joint.graph.IJointGraphOutput;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.uwtime.chunking.AbstractChunkerOutput;
import edu.uw.cs.lil.uwtime.chunking.GoldChunkerOutput;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalSentence;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.resolution.AbstractResolver;
import edu.uw.cs.utils.composites.Pair;

public class GoldDetector extends AbstractDetector<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> {
    @Override
    public void train(IDataCollection<TemporalSentence> trainData,
			AbstractResolver<TemporalSentence, AbstractChunkerOutput<TemporalJointChunk>, IJointGraphOutput<LogicalExpression, MentionResult>> resolver) {
    }

    @Override
    public List<Pair<TemporalSentence, List<AbstractChunkerOutput<TemporalJointChunk>>>> detectMentions(IDataCollection<TemporalSentence> testData) {
        List<Pair<TemporalSentence, List<AbstractChunkerOutput<TemporalJointChunk>>>> allOutputs = new LinkedList<Pair<TemporalSentence, List<AbstractChunkerOutput<TemporalJointChunk>>>>();
        for (TemporalSentence sentence : testData) {
            List<AbstractChunkerOutput<TemporalJointChunk>> sentenceOutputs = 
                    new LinkedList<AbstractChunkerOutput<TemporalJointChunk>>();
            for (TemporalJointChunk chunk : sentence.getLabel())
                sentenceOutputs.add(new GoldChunkerOutput(chunk));
            allOutputs.add(Pair.of(sentence, sentenceOutputs));
        }
        return allOutputs;
    }

    @Override
    public IHashVector getModel() {
        return null;
    }

	@Override
	public void saveModel(String string) {
	}

	@Override
	public void loadModel(InputStream is) {
	}
}
