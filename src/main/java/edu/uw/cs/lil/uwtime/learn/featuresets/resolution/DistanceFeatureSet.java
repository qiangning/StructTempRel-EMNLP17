package edu.uw.cs.lil.uwtime.learn.featuresets.resolution;

import org.joda.time.Days;
import org.joda.time.LocalDateTime;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalYear;
import edu.uw.cs.lil.uwtime.learn.featuresets.TemporalResolutionFeatureSet;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext;

public class DistanceFeatureSet extends TemporalResolutionFeatureSet {
	private static final long serialVersionUID = -8004273529927172644L;
	private static final int[] BINS = {-200, -100, -32, -8, 0, 1, 8, 32, 100, 200};
	private static final int MINIMUM_GRANULARITY = new TemporalYear(0).getGranularity();
	
	@Override
	public String getFeatureTag() {
		return "DISTANCE";
	}
	
	private void setDistanceFeature(IHashVector feats, LocalDateTime from, LocalDateTime to, String label) {
		try {
			int delta = Days.daysBetween(from, to).getDays();
			int binIndex = BINS.length; // last bin
			for (int i = 0 ; i < BINS.length ; i++)
				if (delta < BINS[i]) {
					binIndex = i;
					break;
				}
			feats.set(new KeyArgs(getFeatureTag(), label, "bin" + binIndex), 1);
		}
		catch (ArithmeticException e) {
		}
	}

	@Override
	protected IHashVectorImmutable setMentionFeats(MentionResult result,
			IHashVector feats, TemporalJointChunk chunk) {
		if (result.getEntity() instanceof TemporalSequence && result.getContext().getDirection() != TemporalContext.TemporalDirection.NONE) {
			TemporalSequence sequence = ((TemporalSequence) result.getEntity()).normalize();
			if (sequence.isRange()) {
				if (sequence.getDeepestNode().getGranularity() > MINIMUM_GRANULARITY) {
					setDistanceFeature(feats, result.getContext().getReference().getStartJodaTime(), sequence.getStartJodaTime(), "FROM_REF");
				}
			}
			else {
				feats.set(new KeyArgs(getFeatureTag(), "NOT_FULLY_SPECIFIED"), 1);
			}
		}
		return feats;
	}
}
