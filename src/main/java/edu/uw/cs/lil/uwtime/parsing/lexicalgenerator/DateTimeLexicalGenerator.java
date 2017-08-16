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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.joda.time.LocalDateTime;

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
public class DateTimeLexicalGenerator implements ISentenceLexiconGenerator<LogicalExpression> {
	private final static String DATETIME_LEXICAL_ORIGIN =  "datetime";
	private final ICategoryServices<LogicalExpression>	categoryServices;
	private final List<DateTimeParser> formats;

	public DateTimeLexicalGenerator(ICategoryServices<LogicalExpression> categoryServices) {
		this.categoryServices = categoryServices;
		formats = new LinkedList<DateTimeParser>();
		formats.add(new HMParser());
		formats.add(new HMSParser());
		formats.add(new YMDParser("yyyy-MM-dd"));
		formats.add(new YMDParser("MM-dd-yy"));
		formats.add(new YMDParser("MM-dd-yyyy"));
		formats.add(new YMDParser("MM/dd/yy"));
	}

	private static String intersect(String s1, String s2) {
		return String.format("(intersect:<s,<s,s>> %s %s)", s1, s2);
	}

	private static String nth(int n, String unit) {
		return String.format("(nth:<n,<s,s>> %d:n %s:s)", n, unit);
	}

	@Override
	public Set<LexicalEntry<LogicalExpression>> generateLexicon(Sentence sample, Sentence evidence) {
		final Set<LexicalEntry<LogicalExpression>> lexicalEntries = new HashSet<LexicalEntry<LogicalExpression>>();
		for (String token : evidence.getTokens()) {
			for (DateTimeParser p : formats) {
				LexicalEntry<LogicalExpression> entry = p.getLexicalEntry(token);
				if (entry != null)
					lexicalEntries.add(entry);
			}
		}
		return lexicalEntries;
	}

	private abstract class DateTimeParser {
		SimpleDateFormat format;
		DateTimeParser(String formatString) {
			format = new SimpleDateFormat(formatString);
		}

		abstract String getSemantics(String token) throws ParseException;

		public LexicalEntry<LogicalExpression> getLexicalEntry(String token) {
			String semantics;
			try {
				semantics = getSemantics(token);
			} catch (ParseException e) {
				return null;
			} catch (NumberFormatException e) {
				return null;
			} catch (ArrayIndexOutOfBoundsException e) {
				return null;
			}
			if (semantics != null)
				return new LexicalEntry<LogicalExpression>(Collections.singletonList(token), categoryServices.parse("NP : " + semantics), DATETIME_LEXICAL_ORIGIN);
			else
				return null;
		}
	}

	private class HMSParser extends DateTimeParser {
		HMSParser() {
			super("HH:mm:ss");
		}

		@Override
		String getSemantics(String token) throws ParseException {
			LocalDateTime joda = new LocalDateTime(format.parse(token));
			return intersect(intersect(
					nth(joda.getHourOfDay() + 1, "hour"), 
					nth(joda.getMinuteOfHour() + 1, "minute")),
					nth(joda.getSecondOfMinute() + 1, "second"));
		}
	}

	private class HMParser extends DateTimeParser {
		HMParser() {
			super("HH:mm");
		}

		@Override
		String getSemantics(String token) throws ParseException {
			LocalDateTime joda = new LocalDateTime(format.parse(token));
			return intersect(
					nth(joda.getHourOfDay() + 1, "hour"), 
					nth(joda.getMinuteOfHour() + 1, "minute"));
		}
	}

	private class YMDParser extends DateTimeParser {
		YMDParser(String formatString) {
			super(formatString);
		}

		@Override
		String getSemantics(String token) throws ParseException {
			LocalDateTime joda = new LocalDateTime(format.parse(token));
			return intersect(intersect(
					nth(joda.getYear(), "year"), 
					nth(joda.getMonthOfYear(), "month")), 
					nth(joda.getDayOfMonth(), "day"));
		}
	}
}
