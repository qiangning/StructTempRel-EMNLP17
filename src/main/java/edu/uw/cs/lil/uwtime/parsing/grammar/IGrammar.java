package edu.uw.cs.lil.uwtime.parsing.grammar;

import edu.uw.cs.lil.tiny.ccg.lexicon.ILexicon;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.parser.ccg.cky.AbstractCKYParser;
import edu.uw.cs.lil.tiny.parser.ccg.model.IModelImmutable;
import edu.uw.cs.utils.filter.IFilter;

public interface IGrammar<MR> {
	ILexicon<MR> getLexicon();
	IFilter<MR> getFilter();
	AbstractCKYParser<MR> getParser();
	IModelImmutable<Sentence, MR> getModel();
}