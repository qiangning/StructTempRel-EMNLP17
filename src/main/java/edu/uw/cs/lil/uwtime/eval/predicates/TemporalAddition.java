package edu.uw.cs.lil.uwtime.eval.predicates;

import edu.uw.cs.lil.uwtime.learn.temporal.TemporalExecutionHistory;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext;


public class TemporalAddition extends TemporalPredicate {
	@Override
	public Object evaluate(Object[] args, TemporalContext context, TemporalExecutionHistory executionHistory) {
		/*
		 * args[0]: x (Integer)
		 * args[1]: y (Integer)
		 */
		int x = objToInt(args[0]);
		int y = objToInt(args[1]);
		return x + y;
	}
	
	private static int objToInt(Object obj) {
		return obj instanceof Integer ? (Integer) obj : ((Double) obj).intValue();
	}
}
