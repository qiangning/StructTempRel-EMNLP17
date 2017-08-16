package edu.uw.cs.lil.uwtime.learn.featuresets.detection;

import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.uwtime.chunking.TemporalChunkerOutput;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalApproximateReference;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalEntity;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalDuration;
import edu.uw.cs.lil.uwtime.learn.featuresets.TemporalDetectionFeatureSet;

public class EntityStructureDetectionFeatureSet extends TemporalDetectionFeatureSet {
	@Override
	protected String getFeatureTag() {
		return "ENTITY_STRUCTURE";
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
			return null;
	}

	@Override
	protected IHashVectorImmutable setMentionFeats(
			TemporalChunkerOutput dataItem, IHashVector feats) {
		feats.set(new KeyArgs(
				getFeatureTag(),
				getStructure(dataItem.getChunk().getResult().getEntity())),1);
		return feats;
	}
}
