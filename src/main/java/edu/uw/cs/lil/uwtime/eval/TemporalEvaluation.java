package edu.uw.cs.lil.uwtime.eval;

import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.uw.cs.lil.tiny.mr.lambda.exec.naive.Evaluation;
import edu.uw.cs.lil.tiny.mr.lambda.visitor.LambdaWrapped;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.learn.temporal.TemporalExecutionHistory;
import edu.uw.cs.lil.uwtime.parsing.TemporalContext;

public class TemporalEvaluation extends Evaluation {
	protected TemporalEvaluation(TemporalContext context, 
			TemporalExecutionHistory executionHistory,
			TemporalEvaluationConstants constants) {
		super(new TemporalEvaluationServices(context, executionHistory, constants));
	}

	public static Object of(LogicalExpression exp, TemporalContext context, TemporalExecutionHistory executionHistory, TemporalEvaluationConstants constants, LogicalExpressionCategoryServices categoryServices) {
		final TemporalEvaluation visitor = new TemporalEvaluation(context, executionHistory, constants);
		LogicalExpression directionWrappedExpression = context.applyTemporalDirection(LambdaWrapped.of(exp), categoryServices);
		if (directionWrappedExpression != null) {
			visitor.visit(directionWrappedExpression);
			if (visitor.result instanceof TemporalSequence)
				return ((TemporalSequence) visitor.result).normalize();
			else
				return visitor.result;
		}
		else {
			return null;
		}
	}
}
