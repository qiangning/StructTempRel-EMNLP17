package edu.uw.cs.lil.uwtime.eval.predicates.iota;

import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;


public class TemporalFollowing extends TemporalIota {
	@Override
	TemporalSequence select(TemporalSequence s, TemporalSequence referenceTime) {
		return s.getNearestInstance(referenceTime, false);
	}
}
