package edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron;

import edu.illinois.cs.cogcomp.lbjava.classify.ScoreSet;

/**
 * Created by qning2 on 1/21/17.
 */
public interface ScoringFunc {
    ScoreSet scores(LearningObj obj);
}
