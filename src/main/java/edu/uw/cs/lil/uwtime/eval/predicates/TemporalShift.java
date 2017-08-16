package edu.uw.cs.lil.uwtime.eval.predicates;

import edu.uw.cs.lil.uwtime.eval.TemporalModifier;
import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalDuration;
import edu.uw.cs.lil.uwtime.learn.temporal.TemporalExecutionHistory;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext;

public class TemporalShift extends TemporalPredicate {
	@Override
	public Object evaluate(Object[] args, TemporalContext context, TemporalExecutionHistory executionHistory) {
		/*
		 * args[0]: sequence (TemporalSequence)
		 * args[1]: duration (TemporalDuration)
		 */
		TemporalSequence sequence = (TemporalSequence) args[0];
		TemporalDuration duration = (TemporalDuration) args[1];
		
		executionHistory.addShiftArguments(sequence, duration);
		TemporalSequence shiftedSequence = sequence.shift(duration);
		if (context.getGranularity() == TemporalContext.ShiftGranularity.DELTA)
			shiftedSequence = shiftedSequence.withGranularity(duration);
		if (shiftedSequence != null && duration.hasQuantifier(TemporalQuantifier.SOME)) {
			if (duration.getN() > 0)
				shiftedSequence = shiftedSequence.withModifier(TemporalModifier.FUTURE);
			else if (duration.getN() < 0)
				shiftedSequence = shiftedSequence.withModifier(TemporalModifier.PAST);
			else
				shiftedSequence = shiftedSequence.withModifier(TemporalModifier.PRESENT);
		}
		return shiftedSequence;
	}
}