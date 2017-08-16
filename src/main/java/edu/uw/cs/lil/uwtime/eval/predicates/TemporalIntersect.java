package edu.uw.cs.lil.uwtime.eval.predicates;

import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.learn.temporal.TemporalExecutionHistory;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext;


public class TemporalIntersect extends TemporalPredicate {
	@Override
	public Object evaluate(Object[] args, TemporalContext context, TemporalExecutionHistory executionHistory) {
		/*
		 * args[0]: s1 (TemporalSequence)
		 * args[1]: s2 (TemporalSequence)
		 * 
		 * e.g. 4th of July (<s1> of <s2>)
		 */
		TemporalSequence s1 = (TemporalSequence) args[0];
		TemporalSequence s2 = (TemporalSequence) args[1];

		executionHistory.addIntersectionArguments(s1, s2);
		
		return s1.intersect(s2);
	}
}
