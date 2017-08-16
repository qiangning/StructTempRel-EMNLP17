package edu.uw.cs.lil.uwtime.parsing.typeshifting;

import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.rules.lambda.typeshifting.AbstractUnaryRuleForThreading;

/**
 * N => NP
 * 
 * @author Kenton Lee
 */
public class NounTypeShifting extends AbstractUnaryRuleForThreading {
	private static final String TYPE_SHIFTING_NAME = "shift_noun";
	private static final Syntax SOURCE_SYNTAX = Syntax.N;
	private static final Syntax	TARGET_SYNTAX = Syntax.NP;
	
	public NounTypeShifting() {
		this(TYPE_SHIFTING_NAME);
	}
	
	public NounTypeShifting(String name) {
		super(name, SOURCE_SYNTAX);
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
		return sem;
	}
}