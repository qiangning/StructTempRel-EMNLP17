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
 * NP => N/N
 * 
 * @author Kenton Lee
 */
public class SequenceNounPhraseModifierTypeShifting extends AbstractUnaryRuleForThreading {
	private static final String TYPE_SHIFTING_NAME = "shift_sequence_noun_phrase_modifier";
	private static final Syntax SOURCE_SYNTAX = Syntax.NP;
	private static final Syntax	TARGET_SYNTAX = new ComplexSyntax(Syntax.N, Syntax.N, Slash.FORWARD);
	private static final String RAISED_SEMANTICS_STRING = "(lambda $0:s (lambda $1:s (intersect:<s,<s,s>> $1 $0)))";

	private LogicalExpressionCategoryServices categoryServices;
	private LogicalExpression raiseSemantics;
	
	public SequenceNounPhraseModifierTypeShifting(LogicalExpressionCategoryServices categoryServices) {
		this(TYPE_SHIFTING_NAME, categoryServices);
	}
	
	public SequenceNounPhraseModifierTypeShifting(String name, LogicalExpressionCategoryServices categoryServices) {
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