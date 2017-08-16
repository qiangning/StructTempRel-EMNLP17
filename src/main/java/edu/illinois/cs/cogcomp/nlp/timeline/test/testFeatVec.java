package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.sl.util.FeatureVectorBuffer;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 12/28/16.
 */
public class testFeatVec {
    public static void main(String[] args){
        Lexiconer lm = new Lexiconer();
        String f11="f1:1";
        String f12="f1:12";
        String f2="f2:2";
        lm.addFeature(f11);
        lm.addFeature(f12);
        lm.addFeature(f2);

        FeatureVectorBuffer fv = new FeatureVectorBuffer();
        fv.addFeature(lm.getFeatureId(f11),1.0f);
        fv.addFeature(lm.getFeatureId(f12),1.0f);
        fv.addFeature(lm.getFeatureId(f2),1.0f);
        fv.addFeature(lm.getFeatureId(f2),1.0f);
        System.out.println(fv.toFeatureVector());
    }
}
