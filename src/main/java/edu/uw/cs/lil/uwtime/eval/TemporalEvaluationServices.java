package edu.uw.cs.lil.uwtime.eval;

import java.util.List;

import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.lambda.exec.naive.AbstractEvaluationServices;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalReference;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.eval.predicates.TemporalPredicate;
import edu.uw.cs.lil.uwtime.learn.temporal.TemporalExecutionHistory;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext;

public class TemporalEvaluationServices extends AbstractEvaluationServices<Object> {	
	private TemporalEvaluationConstants constants;
	private TemporalContext context;
	private TemporalExecutionHistory executionHistory;

	public TemporalEvaluationServices(TemporalContext context, 
			TemporalExecutionHistory executionHistory,
			TemporalEvaluationConstants constants) {
		this.context = context;
		this.executionHistory = executionHistory;
		this.constants = constants;
	}

	@Override
	public Object evaluateConstant(LogicalConstant constant) {
		Object constantObject = constants.getConstantObject(constant);
		if (constantObject != null) {
			if (constantObject instanceof TemporalReference)
				constantObject = context.getReference() == null ? null : new TemporalSequence(context.getReference());
			return constantObject;
		}
		else
			return super.evaluateConstant(constant);
	}

	@Override
	public Object evaluateLiteral(LogicalExpression expression, Object[] args) {
		TemporalPredicate evaluator = constants.getPredicate(expression);
		if (evaluator == null) {
			return null;
		}
		for (Object obj : args)
			if (obj == null)
				return null;
		Object result = evaluator.evaluate(args, context, executionHistory);
		if (result != null && result instanceof TemporalSequence) {
			TemporalSequence sequence = (TemporalSequence) result;
			if(sequence.isRange() && sequence.getStartJodaTime() == null) {
				return null;
			}
		}
		return result;
	}

	@Override
	public boolean isInterpretable(LogicalConstant constant) {
		return super.isInterpretable(constant)
				|| evaluateConstant(constant) != null
				|| constants.getPredicate(constant) != null;
	}

	@Override
	public List<?> getAllDenotations(Variable variable) {
		return null;
	}

	@Override
	public boolean isDenotable(Variable variable) {
		return false;
	}

	@Override
	protected Object currentState() {
		return null;
	}
}
