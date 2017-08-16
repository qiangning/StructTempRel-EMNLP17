package edu.uw.cs.lil.uwtime.eval.predicates;

import edu.uw.cs.lil.uwtime.learn.temporal.TemporalExecutionHistory;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext;

public abstract class TemporalPredicate {
	abstract public Object evaluate(Object[] args, TemporalContext context, TemporalExecutionHistory executionHistory);
}
