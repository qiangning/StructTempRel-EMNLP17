package edu.uw.cs.lil.uwtime.parsing.typeshifting;

import edu.uw.cs.lil.tiny.ccg.categories.syntax.ComplexSyntax;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Slash;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.uw.cs.lil.tiny.mr.language.type.Type;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.AbstractUnaryRuleForThreading;

/**
 * C => C/C
 *  * 
 * @author Kenton Lee
 */
public class IntegerAdditionTypeShifting extends AbstractUnaryRuleForThreading {
	private static final String TYPE_SHIFTING_NAME = "shift_integer_addition";
	private static final Syntax SOURCE_SYNTAX = Syntax.C;
	private static final Syntax	TARGET_SYNTAX = new ComplexSyntax(Syntax.C, Syntax.C, Slash.FORWARD);
	private static final String RAISED_SEMANTICS_STRING = "(lambda $0:n (lambda $1:n (+:<n,<n,n>> $0 $1)))";

	private LogicalExpressionCategoryServices categoryServices;
	private LogicalExpression raiseSemantics;
	
	public IntegerAdditionTypeShifting(LogicalExpressionCategoryServices categoryServices) {
		this(TYPE_SHIFTING_NAME, categoryServices);
	}
	
	public IntegerAdditionTypeShifting(String name, LogicalExpressionCategoryServices categoryServices) {
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
		if (!semType.isComplex() && semType.getName().equals("n"))
			return categoryServices.apply(raiseSemantics, sem);
		return null;
	}
}