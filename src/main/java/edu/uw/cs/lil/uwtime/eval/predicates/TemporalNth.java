package edu.uw.cs.lil.uwtime.eval.predicates;

import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.learn.temporal.TemporalExecutionHistory;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext;


public class TemporalNth extends TemporalPredicate {
	@Override
	public Object evaluate(Object[] args, TemporalContext context, TemporalExecutionHistory executionHistory) {
		/*
		 * args[0]: n (Integer)
		 * args[1]: sequence (TemporalSequence)
		 */
		int n;
		if (args[0] instanceof Double)
			n = ((Double) args[0]).intValue();
		else
			n = (Integer) args[0];
		TemporalSequence sequence = (TemporalSequence) args[1];
		return sequence.fixMostGranular(n);
	}
}
