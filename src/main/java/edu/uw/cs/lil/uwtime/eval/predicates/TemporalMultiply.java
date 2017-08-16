package edu.uw.cs.lil.uwtime.eval.predicates;

import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalDuration;
import edu.uw.cs.lil.uwtime.learn.temporal.TemporalExecutionHistory;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext;

public class TemporalMultiply extends TemporalPredicate {
	@Override
	public Object evaluate(Object[] args, TemporalContext context, TemporalExecutionHistory executionHistory) {
		/*
		 * args[0]: x (Integer)
		 * args[1]: duration (TemporalDuration) | y (Integer)
		 */
		int x = objToInt(args[0]);
		if (args[1] instanceof TemporalDuration) {
			TemporalDuration duration = (TemporalDuration) args[1];
			TemporalDuration result = duration.clone();
			if (x != 1 && x != -1)
				result.setQuantifier(TemporalQuantifier.CARDINAL);
			result.setN(result.getN() * x);
			return result;
		}
		else {
			int y = objToInt(args[1]);
			return x * y;
		}
	}
	
	private static int objToInt(Object obj) {
		return obj instanceof Integer ? (Integer) obj : ((Double) obj).intValue();
	}
}
