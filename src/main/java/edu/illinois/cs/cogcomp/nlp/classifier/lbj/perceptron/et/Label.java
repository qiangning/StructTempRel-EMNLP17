// Modifying this comment will cause the next execution of LBJava to overwrite this file.
// F1B88000000000000000B49CC2E4E2A4D294550F94C4A4DC1D0F94D4C2ACBCCCB47FF4AC258CF4AC2D450B1D558A6500A282D2AC30908E5A7A69405A6E4269466E7E9686A5B24D20003567EEF804000000

package edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.et;

import edu.illinois.cs.cogcomp.lbjava.classify.*;
import edu.illinois.cs.cogcomp.lbjava.infer.*;
import edu.illinois.cs.cogcomp.lbjava.io.IOUtilities;
import edu.illinois.cs.cogcomp.lbjava.learn.*;
import edu.illinois.cs.cogcomp.lbjava.parse.*;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.LearningObj;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ParamLBJ;


public class Label extends Classifier
{
  public Label()
  {
    containingPackage = "edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.et";
    name = "Label";
  }

  public String getInputType() { return "edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.LearningObj"; }
  public String getOutputType() { return "discrete"; }


  public FeatureVector classify(Object __example)
  {
    return new FeatureVector(featureValue(__example));
  }

  public Feature featureValue(Object __example)
  {
    String result = discreteValue(__example);
    return new DiscretePrimitiveStringFeature(containingPackage, name, "", result, valueIndexOf(result), (short) allowableValues().length);
  }

  public String discreteValue(Object __example)
  {
    if (!(__example instanceof LearningObj))
    {
      String type = __example == null ? "null" : __example.getClass().getName();
      System.err.println("Classifier 'Label(LearningObj)' defined on line 16 of et_perceptron.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    LearningObj obj = (LearningObj) __example;

    return "" + (obj.getRelation());
  }

  public FeatureVector[] classify(Object[] examples)
  {
    if (!(examples instanceof LearningObj[]))
    {
      String type = examples == null ? "null" : examples.getClass().getName();
      System.err.println("Classifier 'Label(LearningObj)' defined on line 16 of et_perceptron.lbj received '" + type + "' as input.");
      new Exception().printStackTrace();
      System.exit(1);
    }

    return super.classify(examples);
  }

  public int hashCode() { return "Label".hashCode(); }
  public boolean equals(Object o) { return o instanceof Label; }
}

