package edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron;

import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ParamLBJ;
/**
 * Created by qning2 on 11/27/16.
 */
public class LearningObj {

    protected String featString;
    protected String label;
    protected String[] features = null;
    public LearningObj(String featString, String label) {
        this.featString = featString;
        this.label = label;
    }

    public String[] getAllFeatures() {
        String[] feats = featString.split(ParamLBJ.FEAT_DELIMITER);
        int n = feats.length;
        if (n < 1) {
            System.out.println("ERROR: Cannot get features! (" + featString + ")");
        }
        this.features = new String[n];
        for (int i = 0; i < n; i++) {
            this.features[i] = feats[i];
        }
        return this.features;
    }

    public String getRelation() {
        return label;
    }
}

