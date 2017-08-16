package edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron;

import edu.illinois.cs.cogcomp.lbjava.classify.ScoreSet;
import edu.illinois.cs.cogcomp.lbjava.learn.Softmax;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ee.ee_perceptron;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 1/20/17.
 */
public class MultiClassifiers implements ScoringFunc{
    private double lambda;
    public List<ee_perceptron> classifiers = new ArrayList<>();
    public MultiClassifiers(double lambda){
        this.lambda = lambda;
    }
    public void addClassifier(ee_perceptron classifier){
        classifiers.add(classifier);
    }
    public void dropClassifier(){
        int n = classifiers.size();
        classifiers.remove(classifiers.get(n-1));
    }
    @Override
    public ScoreSet scores(LearningObj obj){
        ScoreSet scores = classifiers.get(0).scores(obj);
        for(int i=1;i<classifiers.size();i++){
            ScoreSet score = classifiers.get(i).scores(obj);
            scores = mergeScores(scores,score,lambda);
        }
        return scores;
    }
    public ScoreSet mergeScores(ScoreSet s1, ScoreSet s2, double l){
        ScoreSet scores = new ScoreSet();
        for(Object v:s1.values()){
            if(s2.values().contains((String)v)) {
                double avg_score = s1.get((String) v) * (1 - l) + s2.get((String) v) * l;
                scores.put((String) v, avg_score);
            }
            else{
                //System.out.println("Inconsistent keys in two scoresets.");
                scores.put((String)v, s1.get((String)v));
            }
        }
        return scores;
    }
}
