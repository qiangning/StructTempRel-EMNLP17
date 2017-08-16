package edu.uw.cs.lil.uwtime.parsing.typeshifting;

import edu.uw.cs.lil.tiny.ccg.categories.syntax.ComplexSyntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Slash;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.AbstractUnaryRuleForThreading;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;

/**
 * N => N/N
 * 
 * @author Kenton Lee
 */
public class DurationNounModifierTypeShifting extends AbstractUnaryRuleForThreading {
	private static final String TYPE_SHIFTING_NAME = "shift_duration_noun_modifier";
	private static final Syntax SOURCE_SYNTAX = Syntax.N;
	private static final Syntax	TARGET_SYNTAX = new ComplexSyntax(Syntax.N, Syntax.N, Slash.FORWARD);
	private static final String RAISED_SEMANTICS_STRING = "(lambda $0:s (lambda $1:d (id:<d,d> $1)))";

	private LogicalExpressionCategoryServices categoryServices;
	private LogicalExpression raiseSemantics;
	
	public DurationNounModifierTypeShifting(LogicalExpressionCategoryServices categoryServices) {
		this(TYPE_SHIFTING_NAME, categoryServices);
	}
	
	public DurationNounModifierTypeShifting(String name, LogicalExpressionCategoryServices categoryServices) {
		super(name, SOURCE_SYNTAX);
		this.categoryServices = categoryServices;
		this.raiseSemantics = categoryServices.parseSemantics(RAISED_SEMANTICS_STRING);
	}
	
	@Override
	protected Syntax getSourceSyntax() {
		return SOURCE_SYNTAX;
	}
	
	@Override
	protected Syntax getTargetSyntax() {
		return TARGET_SYNTAX;
	}
	
	@Override
	protected LogicalExpression typeShiftSemantics(LogicalExpression sem) {
		final Type semType = sem.getType();
		if (!semType.isComplex() && semType.isExtending(TemporalSequence.LOGICAL_TYPE))
			return categoryServices.apply(raiseSemantics, sem);
		return null;
	}
}