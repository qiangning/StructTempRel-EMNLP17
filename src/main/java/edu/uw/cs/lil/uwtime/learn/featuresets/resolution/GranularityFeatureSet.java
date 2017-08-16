package edu.uw.cs.lil.uwtime.learn.featuresets.resolution;

import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalDuration;
import edu.uw.cs.lil.uwtime.learn.featuresets.TemporalResolutionFeatureSet;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext;
import edu.uw.cs.utils.composites.Pair;

public class GranularityFeatureSet extends TemporalResolutionFeatureSet {
	private static final long serialVersionUID = -8004273529927172644L;

	@Override
	public String getFeatureTag() {
		return "GRANULARITY";
	}

	@Override
	protected IHashVectorImmutable setMentionFeats(MentionResult result,
			IHashVector feats, TemporalJointChunk chunk) {
		if (result.getContext().getGranularity() != TemporalContext.ShiftGranularity.NONE) {
			for (Pair<TemporalSequence, TemporalDuration> shiftArguments : result.getExecutionHistory().getShiftArguments()) {
				feats.set(new KeyArgs(
						getFeatureTag(),
						shiftArguments.first().getDeepestNode().getName(),
						shiftArguments.second().getName(),
						result.getContext().getReferenceType(),
						result.getContext().getGranularity().toString()), 1);
			}
		}
		return feats;
	}
}
