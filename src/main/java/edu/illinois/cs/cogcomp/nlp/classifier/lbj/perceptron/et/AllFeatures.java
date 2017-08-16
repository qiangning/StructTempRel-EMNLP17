// Modifying this comment will cause the next execution of LBJava to overwrite this file.
// F1B88000000000000000D4C81BA038040144F756A9087844257EC41962550C225A85862BE5E4E8516F6DA4CF77738D4AA18973F6E313FB5849E48B7A4F0AE5711AC5C39A71E8C1A9162CC3C4E05F91B2E5A26D5BD164BD56CDE78AA04AFF6A3F817614119511D6271F615F164598838E7DA92B476F799833D18AD8D1EA05958C363C6B301AA3B5EC69000000

package edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.et;

import edu.illinois.cs.cogcomp.lbjava.classify.*;
import edu.illinois.cs.cogcomp.lbjava.infer.*;
import edu.illinois.cs.cogcomp.lbjava.io.IOUtilities;
import edu.illinois.cs.cogcomp.lbjava.learn.*;
import edu.illinois.cs.cogcomp.lbjava.parse.*;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.LearningObj;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ParamLBJ;


public class AllFeatures extends Classifier
{
  public AllFeatures()
  {
    containingPackage = "edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.et";
    name = "AllFeatures";
  }

  public String getInputType() { return "edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.LearningObj"; }
  public String getOutputType() { return "discrete%"; }

  public FeatureVector classify(Object __example)
  {
    if (!(__example instanceof LearningObj))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'AllFeatures(LearningObj)' defined on line 8 of et_perceptron.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    LearningObj obj = (LearningObj) __example;

    FeatureVector __result;
    __result = new FeatureVector();
    String __id;
    String __value;

    String[] feats = obj.getAllFeatures();
    for (int i = 0; i < feats.length; i++)
    {
      __id = "" + (feats[i]);
      __value = "true";
      __result.addFeature(new DiscretePrimitiveStringFeature(this.containingPackage, this.name, __id, __value, valueIndexOf(__value), (short) 0));
    }
    return __result;
  }

  public FeatureVector[] classify(Object[] examples)
  {
    if (!(examples instanceof LearningObj[]))
    {
      String type = examples == null ? "null" : examples.getClass().getName();
      System.err.println("Classifier 'AllFeatures(LearningObj)' defined on line 8 of et_perceptron.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    return super.classify(examples);
  }

  public int hashCode() { return "AllFeatures".hashCode(); }
  public boolean equals(Object o) { return o instanceof AllFeatures; }
}

