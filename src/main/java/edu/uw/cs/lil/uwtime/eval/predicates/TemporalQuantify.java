package edu.uw.cs.lil.uwtime.eval.predicates;

import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalDuration;
import edu.uw.cs.lil.uwtime.learn.temporal.TemporalExecutionHistory;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext;

public class TemporalQuantify extends TemporalPredicate {
	private TemporalQuantifier quantifier;
	public TemporalQuantify(TemporalQuantifier quantifier) {
		this.quantifier = quantifier;
	}
	@Override
	public Object evaluate(Object[] args, TemporalContext context, TemporalExecutionHistory executionHistory) {
		/*
		 * args[0]: sequence (TemporalSequence) | duration (TemporalDuration)
		 */
		if (args[0] instanceof TemporalSequence) {
			TemporalSequence sequence = (TemporalSequence) args[0];
			return sequence.withQuantifier(quantifier);
		}
		else if (args[0] instanceof TemporalDuration) {
			TemporalDuration duration = (TemporalDuration) args[0];
			return duration.withQuantifier(quantifier);
		}
		throw new UnsupportedOperationException();
	}
}