/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework
 * <p>
 * Copyright (C) 2013 Yoav Artzi
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 ******************************************************************************/
package edu.uw.cs.lil.uwtime.parsing.lexicalgenerator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.uw.cs.lil.tiny.ccg.categories.ICategoryServices;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ISentenceLexiconGenerator;

/**
 * Generate integer lexical entry for every integer token.
 * 
 * @author Kenton Lee
 */
public class IntegerLexicalGenerator implements	ISentenceLexiconGenerator<LogicalExpression> {
	private final static String INTEGER_FORMAT = "C : %d:n"; 
	private final static String INTEGER_LEXICAL_ORIGIN =  "integer";
	private final ICategoryServices<LogicalExpression>	categoryServices;

	public IntegerLexicalGenerator(ICategoryServices<LogicalExpression> categoryServices) {
		this.categoryServices = categoryServices;
	}

	@Override
	public Set<LexicalEntry<LogicalExpression>> generateLexicon(Sentence sample, Sentence evidence) {
		final Set<LexicalEntry<LogicalExpression>> lexicalEntries = new HashSet<LexicalEntry<LogicalExpression>>();
		for (String token : evidence.getTokens()) {
			try { 
				int n = Integer.parseInt(token);
				lexicalEntries.add(new LexicalEntry<LogicalExpression> (Collections.singletonList(token), categoryServices.parse(String.format(INTEGER_FORMAT, n)), INTEGER_LEXICAL_ORIGIN));
			} 
			catch(NumberFormatException e) { 
				continue;
			}
		}
		return lexicalEntries;
	}

}
