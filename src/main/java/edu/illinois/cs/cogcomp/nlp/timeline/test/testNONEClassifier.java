package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.classifier.FeatureExtractor;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ee.ee_perceptron;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.ReaderConfigurator;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.timeline.LocalClassifier;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 1/21/17.
 */
public class testNONEClassifier extends LocalClassifier{
    private String modelPath;
    private String modelLexPath;
    public testNONEClassifier(String modelDirPath, String modelName){
        super(modelDirPath,modelName);
        modelPath = modelDirPath+modelName+".lc";
        modelLexPath = modelDirPath+modelName+".lex";
        classifier = new ee_perceptron(modelPath,modelLexPath);
    }
    public void setTrainDocs(List<TemporalDocument> traindocs){
        for(TemporalDocument doc:traindocs)
            addTrainFeats(doc);
    }
    public void setTestDocs(List<TemporalDocument> testdocs){
        for(TemporalDocument doc:testdocs)
            addTestFeats(doc);
    }
    @Override
    public List<String> ExtractFeatures(TemporalDocument doc) {
        int max_dist = 2;
        FeatureExtractor extractor = getFeatureExtractor(doc);
        List<EventChunk> eventChunks = doc.getBodyEventMentions();
        List<String> feats = new ArrayList<>();
        for(EventChunk ec1:eventChunks){
            for(EventChunk ec2:eventChunks){
                if(ec1==ec2)
                    continue;
                if(Math.abs(eventChunks.indexOf(ec1)-eventChunks.indexOf(ec2))>max_dist)
                    continue;
                TLINK tlink = doc.getTlink(ec1,ec2,true);
                String label = tlink==null? TLINK.TlinkType.UNDEF.toStringfull()
                        :tlink.getReducedRelType().toStringfull();
                if(!label.equals("undef"))
                    label = "before";
                feats.add(extractor.getFeatureString(extractor.extractEEfeats(ec1, ec2), label));
            }
        }
        return feats;
    }
    public static void main(String[] args) throws Exception{
        boolean retrain = false;
        ResourceManager rm = new ReaderConfigurator().getDefaultConfig();
        ResourceManager rm2 = new ClassifierConfigurator().getDefaultConfig();
        List<TemporalDocument> testDocs = TempEval3Reader.deserialize(
                rm.getString("ser_dir")+ File.separator+rm.getString("platinum_label"),
                Integer.MAX_VALUE,
                true);
        testNONEClassifier tester = new testNONEClassifier(rm2.getString("eeModelDirPath"),"eeNONEonly");
        tester.setTestDocs(testDocs);

        if(retrain){
            List<TemporalDocument> trainDocs = TempEval3Reader.deserialize(
                    rm.getString("ser_dir")+ File.separator+rm.getString("timebank_label"),
                    Integer.MAX_VALUE,
                    true);
            trainDocs.addAll(TempEval3Reader.deserialize(
                    rm.getString("ser_dir")+ File.separator+rm.getString("aquaint_label"),
                    Integer.MAX_VALUE,
                    true));
            tester.setTrainDocs(trainDocs);
            tester.trainClassifier(30);
            tester.writeModelsToDisk();
            tester.TestOnTrain();
        }
        tester.testClassifier();
    }
}
