package edu.uw.cs.lil.uwtime.data;

import java.util.LinkedList;
import java.util.List;

import org.joda.time.LocalDateTime;

import edu.uw.cs.lil.tiny.data.ILabeledDataItem;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.uwtime.chunking.ChunkSequence;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.corrections.AnnotationCorrections;
import edu.uw.cs.lil.uwtime.corrections.MentionCorrection;
import edu.uw.cs.lil.uwtime.utils.DependencyUtils;
import edu.uw.cs.lil.uwtime.utils.DependencyUtils.DependencyParseToken;
import edu.uw.cs.utils.collections.ListUtils;
import edu.uw.cs.utils.composites.Pair;

public class TemporalSentence implements java.io.Serializable, ILabeledDataItem<Pair<Sentence, TemporalSentence>, ChunkSequence<TemporalJointChunk, LogicalExpression>>{
    private static final long serialVersionUID = 2013931525176952047L;
    private LocalDateTime referenceTime;
    private ChunkSequence<TemporalJointChunk, LogicalExpression> goldChunkSequence;
    private List<String> tokens;
    private List<DependencyParseToken> dependencyParse;
    private Pair<Sentence, TemporalSentence> sample = null;
    private TemporalDocument document;

    public TemporalSentence(TemporalDocument document, String referenceTime) {
        this.document = document;
        this.tokens = new LinkedList<String>();
        try {
            this.referenceTime = new LocalDateTime(referenceTime);
        }
        catch (IllegalArgumentException e) {
            this.referenceTime = null;
        }
        goldChunkSequence = new ChunkSequence<TemporalJointChunk, LogicalExpression>();
    }

    public List<String> getTokens() {
        return tokens;
    }

    public void insertToken(String token) {
        tokens.add(token);
    }

    public void insertGoldChunk(TemporalJointChunk chunk) {
        goldChunkSequence.insertChunk(chunk);
    }

    public int getNumTokens() {
        return tokens.size();
    }

    public String getDocID() {
        return document.getDocID();
    }

    public TemporalDocument getDocument() {
        return document;
    }

    public List<DependencyParseToken> getDependencyParse() {
        return dependencyParse;
    }

    public void saveDependencyParse(String dpString) {
        this.dependencyParse = DependencyUtils.parseDependencyParse(dpString);
    }

    public String toString(int start, int end) {
        return ListUtils.join(tokens.subList(start, end), " ");
    }

    public String toString() {
        return toString(0, tokens.size());
    }
    
    public LocalDateTime getReferenceTime() {
        return referenceTime;
    }

    @Override
    public Pair<Sentence, TemporalSentence> getSample() {
        if (sample == null) {
            List<String> lowerCasedTokens = new LinkedList<String>();
            for (String s : tokens)
                lowerCasedTokens.add(s.toLowerCase());
            sample = Pair.of(new Sentence(lowerCasedTokens), this);
        }
        return sample;
    }

    @Override
    public ChunkSequence<TemporalJointChunk, LogicalExpression> getLabel() {
        return goldChunkSequence;
    }

    public int applyCorrections(AnnotationCorrections corrections) {
        ChunkSequence<TemporalJointChunk, LogicalExpression> correctedGoldChunkSequence = new ChunkSequence<TemporalJointChunk, LogicalExpression>();
        int maxID = -1;
        for (TemporalJointChunk originalChunk : goldChunkSequence) {
            correctedGoldChunkSequence.insertChunk(new TemporalJointChunk(originalChunk, corrections.getCorrection(getDocID(), originalChunk.getTID())));
            maxID = Math.max(maxID, originalChunk.getTID());
        }
        goldChunkSequence = correctedGoldChunkSequence;
        return maxID;
    }

    public void addCorrectedMention(MentionCorrection addition, int tid, int start, int end) {
    	goldChunkSequence.insertChunk(new TemporalJointChunk(this, tid, start, end, addition));
    }

    @Override
    public double calculateLoss(
            ChunkSequence<TemporalJointChunk, LogicalExpression> label) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean prune(ChunkSequence<TemporalJointChunk, LogicalExpression> y) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double quality() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCorrect(
            ChunkSequence<TemporalJointChunk, LogicalExpression> label) {
        throw new UnsupportedOperationException();
    }
}
