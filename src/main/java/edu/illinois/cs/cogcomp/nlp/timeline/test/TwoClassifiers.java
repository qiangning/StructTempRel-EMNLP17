package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.lbjava.classify.ScoreSet;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.LearningObj;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ee.ee_perceptron;

/**
 * Created by qning2 on 1/18/17.
 */
public class TwoClassifiers {
    private double lambda;
    private ee_perceptron classifier1;
    private ee_perceptron classifier2;
    public TwoClassifiers(ee_perceptron classifier1, ee_perceptron classifier2, double lambda){
        this.classifier1 = classifier1;
        this.classifier2 = classifier2;
        this.lambda = lambda;
    }
    public ScoreSet scores(LearningObj obj){
        ScoreSet scores1 = classifier1.scores(obj);
        ScoreSet scores2 = classifier2.scores(obj);
        ScoreSet scores = new ScoreSet();
        for(Object v:scores1.values()){
            double avg_score = scores1.get((String)v)*(1-lambda)+scores2.get((String)v)*lambda;
            scores.put((String)v,avg_score);
        }
        return scores;
    }
}
