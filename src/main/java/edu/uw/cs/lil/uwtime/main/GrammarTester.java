package edu.uw.cs.lil.uwtime.main;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.mr.lambda.FlexibleTypeComparator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYDerivation;
import edu.uw.cs.lil.tiny.parser.ccg.cky.CKYParserOutput;
import edu.uw.cs.lil.uwtime.parsing.grammar.TemporalGrammar;
import edu.uw.cs.lil.uwtime.utils.FileUtils;

public class GrammarTester {
	public static List<String> getUnitTests() {
		List<String> phrases = new LinkedList<String>(); 
		phrases.add("a week");
		phrases.add("tomorrow");
		phrases.add("2013");
		phrases.add("nineteen eighty four");
		phrases.add("a year later");
		phrases.add("the year after 1999");
		return phrases;
	}
	
	public static void initServices() throws IOException {
		LogicLanguageServices.setInstance(
				new LogicLanguageServices.Builder(
						new TypeRepository(FileUtils.streamToFile(TemporalExperiment.class.getResourceAsStream("/resources/lexicon/temporal.types"), "types")), 
						new FlexibleTypeComparator())
				.setNumeralTypeName("n")
				.build());
	}
	
	public static void main (String[] args) throws IOException {
		initServices();
		TemporalGrammar grammar = new TemporalGrammar();
		for (String phrase : getUnitTests()) {
			System.out.println(phrase);
			Sentence s = new Sentence(phrase);
			CKYParserOutput<LogicalExpression> output = grammar.getParser().parse(s, grammar.getModel().createDataItemModel(s));
			for (CKYDerivation<LogicalExpression> parse  : output.getAllParses())
				System.out.println("\t" + parse.getSemantics());
		}
	}
}
