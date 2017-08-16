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
package edu.uw.cs.lil.uwtime.learn.featuresets.resolution;

import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.ccg.categories.Category;
import edu.uw.cs.lil.tiny.ccg.lexicon.LexicalEntry;
import edu.uw.cs.lil.tiny.data.sentence.Sentence;
import edu.uw.cs.lil.tiny.data.situated.ISituatedDataItem;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.parser.ccg.model.lexical.AbstractLexicalFeatureSet;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVector;
import edu.uw.cs.lil.tiny.base.hashvector.IHashVectorImmutable;
import edu.uw.cs.lil.tiny.base.hashvector.KeyArgs;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.utils.composites.Pair;
import edu.uw.cs.utils.composites.Triplet;

public class WordingSkippingFeatureSet extends AbstractLexicalFeatureSet<ISituatedDataItem<Sentence, TemporalJointChunk>, LogicalExpression> {
	private static final long serialVersionUID = -7076837466568116808L;
	private static final String FEATURE_TAG = "LEX";
	private final Category<LogicalExpression> emptyCategory;
	private final double skippingCost;
	
	public WordingSkippingFeatureSet(Category<LogicalExpression> emptyCategory, double skippingCost) {
		this.emptyCategory = emptyCategory;
		this.skippingCost = skippingCost;
	}
	
	@Override
	public boolean addEntry(LexicalEntry<LogicalExpression> entry, IHashVector parametersVector) {
		return false;
	}

	@Override
	public List<Triplet<KeyArgs, Double, String>> getFeatureWeights(IHashVector theta) {
		final List<Triplet<KeyArgs, Double, String>> weights = new LinkedList<Triplet<KeyArgs, Double, String>>();
		for (final Pair<KeyArgs, Double> feature : theta.getAll(FEATURE_TAG))
			weights.add(Triplet.of(feature.first(), feature.second(), (String) null));
		return weights;
	}

	@Override
	public boolean isValidWeightVector(IHashVectorImmutable vector) {
		return true;
	}
	
	@Override
	public double score(LexicalEntry<LogicalExpression> lexicalEntry, IHashVector theta) {
		return lexicalEntry.getCategory().equals(emptyCategory) ? skippingCost : 0;
	}

	@Override
	public void setFeats(LexicalEntry<LogicalExpression> lexicalEntry, IHashVector features) {
		if (lexicalEntry.getCategory().equals(emptyCategory))
			features.set(new KeyArgs(FEATURE_TAG, "EMPTY"), 1);
	}
}
