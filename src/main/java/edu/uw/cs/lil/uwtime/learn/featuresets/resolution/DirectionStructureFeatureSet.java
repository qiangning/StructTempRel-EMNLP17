package edu.uw.cs.lil.uwtime.learn.featuresets.resolution;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalApproximateReference;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalEntity;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalDuration;
import edu.uw.cs.lil.uwtime.learn.featuresets.TemporalResolutionFeatureSet;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;

public class DirectionStructureFeatureSet extends TemporalResolutionFeatureSet {
	private static final long serialVersionUID = -8004273529927172644L;
	
	@Override
	public String getFeatureTag() {
		return "DIRECTION_STRUCTURE";
	}
	
	private String getStructure (TemporalEntity entity) {
		if (entity instanceof TemporalSequence) {
			return ((TemporalSequence) entity).getStructuralCopy().toString();
		}
		else if (entity instanceof TemporalDuration) {
			return ((TemporalDuration) entity).getStructuralCopy().toString();
		}
		else if (entity instanceof TemporalApproximateReference) {
			return entity.getValue();
		}
		else 
			return "null";
	}

	@Override
	protected IHashVectorImmutable setMentionFeats(MentionResult result,
			IHashVector feats, TemporalJointChunk chunk) {
		feats.set(new KeyArgs(
				getFeatureTag(),
				"ref=" + getStructure(result.getContext().getReference()),
				"result=" + getStructure(result.getEntity()),
				result.getContext().getDirection().toString()),1);
		return feats;
	}
}
