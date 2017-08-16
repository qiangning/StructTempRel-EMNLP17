package edu.uw.cs.lil.uwtime.eval.predicates.iota;

import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;

public class TemporalPrevious extends TemporalIota {
	@Override
	TemporalSequence select(TemporalSequence s, TemporalSequence referenceTime) {
		// TODO Auto-generated method stub
		return s.getFixedInstance(referenceTime, -1);
	}
}
