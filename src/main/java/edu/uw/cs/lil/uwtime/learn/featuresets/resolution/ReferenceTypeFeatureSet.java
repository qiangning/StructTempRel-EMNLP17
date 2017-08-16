package edu.uw.cs.lil.uwtime.learn.featuresets.resolution;

import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.learn.featuresets.TemporalResolutionFeatureSet;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;

public class ReferenceTypeFeatureSet extends TemporalResolutionFeatureSet {
    private static final long serialVersionUID = -8004273529927172644L;

    @Override
	public String getFeatureTag() {
        return "REFERENCE_TYPE";
    }

    @Override
    protected IHashVectorImmutable setMentionFeats(MentionResult result,
            IHashVector feats, TemporalJointChunk chunk) {
        if (result.getEntity() instanceof TemporalSequence) {
            TemporalSequence sequence = ((TemporalSequence) result.getEntity());
            feats.set(new KeyArgs(getFeatureTag(), 
                    result.getContext().getReferenceType(), 
                    "SEQUENCE_GRANULARITY=" + sequence.getDeepestNode().getName()), 1);
            if (result.getContext().getReference() != null) {
                feats.set(new KeyArgs(getFeatureTag(), 
                        result.getContext().getReferenceType(), 
                        "REFERENCE_GRANULARITY=" + result.getContext().getReference().getDeepestNode().getName()), 1);
                feats.set(new KeyArgs(getFeatureTag(), 
                        result.getContext().getReferenceType(), 
                        "REFERENCE_GRANULARITY=" + result.getContext().getReference().getDeepestNode().getName(), 
                        "SEQUENCE_GRANULARITY=" + sequence.getDeepestNode().getName()), 1);
            }
        }
        for (LexicalEntry<LogicalExpression> entry : result.getDerivation().getMaxLexicalEntries())
            feats.set(new KeyArgs(getFeatureTag(), entry.toString(), result.getContext().getReferenceType()), 1);
        return feats;
    }
}
