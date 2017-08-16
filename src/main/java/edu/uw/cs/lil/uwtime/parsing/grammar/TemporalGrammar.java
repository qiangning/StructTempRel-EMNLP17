package edu.uw.cs.lil.uwtime.parsing.grammar;

import java.util.HashSet;
import java.util.Set;

import edu.uw.cs.lil.tiny.base.string.StubStringFilter;
import edu.uw.cs.lil.tiny.ccg.categories.syntax.Syntax;
import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.ccg.lexicon.Lexicon;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.SimpleFullParseFilter;
import edu.uw.cs.lil.tiny.parser.ccg.cky.AbstractCKYParser;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYBinaryParsingRule;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYUnaryParsingRule;
import edu.uw.cs.lil.tiny.parser.ccg.cky.SimpleWordSkippingLexicalGenerator;
import edu.uw.cs.lil.tiny.parser.ccg.cky.single.CKYParser;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.lil.tiny.parser.ccg.model.Model;
import edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.application.BackwardApplication;
import edu.uw.cs.lil.tiny.parser.ccg.rules.primitivebinary.application.ForwardApplication;
import edu.uw.cs.lil.tiny.parser.ccg.rules.skipping.BackwardSkippingRule;
import edu.uw.cs.lil.tiny.parser.ccg.rules.skipping.ForwardSkippingRule;
import edu.uw.cs.lil.uwtime.eval.TemporalEvaluationConstants;
import edu.uw.cs.lil.uwtime.parsing.lexicalgenerator.DateTimeLexicalGenerator;
import edu.uw.cs.lil.uwtime.parsing.lexicalgenerator.IntegerLexicalGenerator;
import edu.uw.cs.lil.uwtime.parsing.typeshifting.DurationNounModifierTypeShifting;
import edu.uw.cs.lil.uwtime.parsing.typeshifting.DurationNounPhraseModifierTypeShifting;
import edu.uw.cs.lil.uwtime.parsing.typeshifting.IntegerAdditionTypeShifting;
import edu.uw.cs.lil.uwtime.parsing.typeshifting.IntegerMultiplicationTypeShifting;
import edu.uw.cs.lil.uwtime.parsing.typeshifting.NounTypeShifting;
import edu.uw.cs.lil.uwtime.parsing.typeshifting.SequenceNounModifierTypeShifting;
import edu.uw.cs.lil.uwtime.parsing.typeshifting.SequenceNounPhraseModifierTypeShifting;
import edu.uw.cs.lil.uwtime.utils.FileUtils;
import edu.uw.cs.utils.filter.IFilter;

public class TemporalGrammar implements IGrammar<LogicalExpression> {
	private static String[] LEXICAL_GROUPS = {"base", "daysofmonth", "daysofweek", "decades", "hours", "months", "numbers", "ordinalnumbers", "seasons", "timesofday", "years"};

	private final LogicalExpressionCategoryServices categoryServices;
	private final ILexicon<LogicalExpression> lexicon;
	private final IModelImmutable<Sentence, LogicalExpression> model;
	private final AbstractCKYParser<LogicalExpression> parser;
	private final IFilter<LogicalExpression> filter;
	private final TemporalEvaluationConstants constants;

	public TemporalGrammar() {
		categoryServices = new LogicalExpressionCategoryServices(true);
		lexicon = createLexicon();
		model = createModel();
		parser = createParser();
		filter = createFilter();
		constants = new TemporalEvaluationConstants(categoryServices);
	}

	private ILexicon<LogicalExpression> createLexicon() {
		final ILexicon<LogicalExpression> lexicon = new Lexicon<LogicalExpression>();
		for (String group : LEXICAL_GROUPS) {
			lexicon.addEntriesFromFile(FileUtils.streamToFile(this.getClass().getResourceAsStream("/resources/lexicon/temporal." + group + ".lex"), group), 
					new StubStringFilter(), 
					categoryServices, 
					group);
		}
		return lexicon;
	}

	private IModelImmutable<Sentence, LogicalExpression> createModel() {
		return new Model.Builder<Sentence, LogicalExpression>()
				.setLexicon(lexicon)
				.build();
	}

	private AbstractCKYParser<LogicalExpression> createParser() {
		final Set<Syntax> syntaxSet = new HashSet<Syntax>();
		syntaxSet.add(Syntax.N);
		syntaxSet.add(Syntax.NP);
		syntaxSet.add(Syntax.AP);
		syntaxSet.add(Syntax.ADJ);
		final SimpleFullParseFilter parseFilter = new SimpleFullParseFilter(syntaxSet);
		return new CKYParser.Builder<LogicalExpression>(categoryServices, parseFilter)
				.addBinaryParseRule(new CKYBinaryParsingRule<LogicalExpression>(new ForwardApplication<LogicalExpression>(categoryServices)))
				.addBinaryParseRule(new CKYBinaryParsingRule<LogicalExpression>(new BackwardApplication<LogicalExpression>(categoryServices)))
				.addBinaryParseRule(new CKYUnaryParsingRule<LogicalExpression>(new SequenceNounModifierTypeShifting(categoryServices)))
				.addBinaryParseRule(new CKYUnaryParsingRule<LogicalExpression>(new SequenceNounPhraseModifierTypeShifting(categoryServices)))
				.addBinaryParseRule(new CKYUnaryParsingRule<LogicalExpression>(new DurationNounModifierTypeShifting(categoryServices)))
				.addBinaryParseRule(new CKYUnaryParsingRule<LogicalExpression>(new DurationNounPhraseModifierTypeShifting(categoryServices)))
				.addBinaryParseRule(new CKYUnaryParsingRule<LogicalExpression>(new NounTypeShifting()))
				.addBinaryParseRule(new CKYUnaryParsingRule<LogicalExpression>(new IntegerAdditionTypeShifting(categoryServices)))
				.addBinaryParseRule(new CKYUnaryParsingRule<LogicalExpression>(new IntegerMultiplicationTypeShifting(categoryServices)))
				.addBinaryParseRule(new CKYBinaryParsingRule<LogicalExpression>(new BackwardSkippingRule<LogicalExpression>(categoryServices)))
				.addBinaryParseRule(new CKYBinaryParsingRule<LogicalExpression>(new ForwardSkippingRule<LogicalExpression>(categoryServices)))
				.addSentenceLexicalGenerator(new IntegerLexicalGenerator(categoryServices))
				.addSentenceLexicalGenerator(new DateTimeLexicalGenerator(categoryServices))
				.setMaxNumberOfCellsInSpan(200)
				.setWordSkippingLexicalGenerator(new SimpleWordSkippingLexicalGenerator<LogicalExpression>(categoryServices))
				.build();
	}

	private IFilter<LogicalExpression> createFilter() {
		return new IFilter<LogicalExpression>() {
			@Override
			public boolean isValid(LogicalExpression e) {
				return !e.getType().isComplex() && !e.getType().getName().equals("n");
			}
		};
	}

	public LogicalExpressionCategoryServices getCategoryServices() {
		return categoryServices;
	}

	public TemporalEvaluationConstants getConstants() {
		return constants;
	}

	public ILexicon<LogicalExpression> getLexicon() {
		return lexicon;
	}

	@Override
	public IModelImmutable<Sentence, LogicalExpression> getModel() {
		return model;
	}

	public AbstractCKYParser<LogicalExpression> getParser() {
		return parser;
	}

	public IFilter<LogicalExpression> getFilter() {
		return filter;
	}
}