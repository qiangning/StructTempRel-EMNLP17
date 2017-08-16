package edu.illinois.cs.cogcomp.nlp.classifier;

import edu.illinois.cs.cogcomp.lbjava.classify.ScoreSet;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.LearningObj;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ScoringFunc;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ee.ee_perceptron;

/**
 * Created by qning2 on 1/21/17.
 */
public class my_ee_perceptron implements ScoringFunc {
    public ee_perceptron ee_classifier;

    public my_ee_perceptron(ee_perceptron ee_classifier) {
        this.ee_classifier = ee_classifier;
    }

    @Override
    public ScoreSet scores(LearningObj obj){
        return ee_classifier.scores(obj);
    }
}
