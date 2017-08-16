package edu.uw.cs.lil.uwtime.eval.predicates.iota;

import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.eval.predicates.TemporalPredicate;
import edu.uw.cs.lil.uwtime.learn.temporal.TemporalExecutionHistory;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext;

public abstract class TemporalIota extends TemporalPredicate {
	abstract TemporalSequence select(TemporalSequence s, TemporalSequence referenceTime);

	@Override
	public Object evaluate(Object[] args, TemporalContext context, TemporalExecutionHistory executionHistory) {
        TemporalSequence referenceTime = (TemporalSequence) args[1];
        if (referenceTime == null)
            return null;
		TemporalSequence s = ((TemporalSequence) args[0]).normalize();
		if (s == null || s.isRange() || s.hasQuantifier(TemporalQuantifier.EVERY))
			return null;
		return select(s, referenceTime);
	}
}
