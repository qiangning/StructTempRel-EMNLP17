package edu.illinois.cs.cogcomp.nlp.timeline;

import de.bwaldvogel.liblinear.Feature;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.classifier.FeatureExtractor;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ParamLBJ;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ee.ee_perceptron;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.util.PrecisionRecallManager;
import edu.illinois.cs.cogcomp.nlp.util.TempDocEval;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Created by qning2 on 11/27/16.
 */
public class LocalEEClassifierExp extends LocalClassifier {
    private String modelPath;
    private String modelLexPath;
    public static double vague_portion = 0.01;
    public static boolean addVagueTlinks = true;
    public static boolean feat_filter_on = false;// feature with dist>ignore_edge won't be extracted
    public static boolean test_filter_on = true;
    public static boolean knownNONEs = true;
    public static int ignore_edge = 2;

    public LocalEEClassifierExp(List<TemporalDocument> traindocs, List<TemporalDocument> testdocs, String modelDirPath, String modelName) {
        super(modelDirPath,modelName);//@@addVagueTlinks hasn't been updated.
        modelPath = modelDirPath+ File.separator+modelName+".lc";
        modelLexPath = modelDirPath+ File.separator+modelName+".lex";
        classifier = new ee_perceptron(modelPath,modelLexPath);
        if(traindocs!=null) {
            setTrainDocs(traindocs);
        }
        if(testdocs!=null) {
            setTestDocs(testdocs);
        }
    }
    public void setTrainDocs(List<TemporalDocument> trainDocs){
        for (TemporalDocument doc : trainDocs) {
            addTrainFeats(doc);
        }
    }
    public void setTestDocs(List<TemporalDocument> testdocs){
        for (TemporalDocument doc : testdocs) {
            addTestFeats(doc);
        }
    }
    @Override
    public List<String> ExtractFeatures(TemporalDocument doc) {
        FeatureExtractor extractor = getFeatureExtractor(doc);
        List<String> allFeats = new ArrayList<>();
        List<TLINK> tlinks = doc.getBodyTlinks();
        for (TLINK tlink : tlinks) {
            if (!tlink.getSourceType().equals(TempEval3Reader.Type_Event) || !tlink.getTargetType().equals(TempEval3Reader.Type_Event))
                continue;
            int eiid1 = tlink.getSourceId();
            int eiid2 = tlink.getTargetId();
            EventChunk ec1 = doc.getEventMentionFromEIID(eiid1);
            EventChunk ec2 = doc.getEventMentionFromEIID(eiid2);
            if (ec1 == null || ec2 == null)
                continue;
            if(feat_filter_on) {
                if (Math.abs(doc.getSentId(ec1) - doc.getSentId(ec2)) >= ignore_edge)
                    continue;
            }
            try {
                allFeats.add(extractor.getFeatureString(extractor.extractEEfeats(ec1, ec2), tlink.getReducedRelType().toStringfull()));
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        if(addVagueTlinks) {
            List<EventChunk> mentions = doc.getBodyEventMentions();
            for(EventChunk o1:mentions){
                for(EventChunk o2:mentions){
                    if(feat_filter_on) {
                        if(Math.abs(doc.getSentId(o1) - doc.getSentId(o2)) >= ignore_edge)
                            continue;
                    }
                    TLINK tlink = doc.getTlink(o1,o2);
                    if(tlink==null){
                        if(Math.random()>vague_portion)
                            continue;
                        try {
                            allFeats.add(extractor.getFeatureString(extractor.extractEEfeats(o1, o2), TLINK.TlinkType.UNDEF.toStringfull()));
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return allFeats;
    }
    public String ExtractFeatures(TemporalDocument doc, EventChunk ec1, EventChunk ec2){
        FeatureExtractor extractor = getFeatureExtractor(doc);
        TLINK tlink = doc.getTlink(ec1,ec2);
        String label = tlink==null?TLINK.TlinkType.UNDEF.toStringfull():tlink.getReducedRelType().toStringfull();
        return extractor.getFeatureString(extractor.extractEEfeats(ec1,ec2),label);
    }
    public ee_perceptron getClassifier(){
        return (ee_perceptron)classifier;
    }
    public TemporalDocument testOnDoc(TemporalDocument docGold, String[] label_ignores){
        TemporalDocument docPred = new TemporalDocument(docGold);
        docPred.setBodyTlinks(null);
        List<TLINK> predTlinks = new ArrayList<>();
        int lid = 0;
        for(EventChunk ec1:docGold.getBodyEventMentions()){
            for(EventChunk ec2:docGold.getBodyEventMentions()){
                if(ec1.equals(ec2))
                    continue;
                if(knownNONEs){
                    TLINK tlink = docGold.getTlink(ec1,ec2);
                    if(tlink==null||tlink.getReducedRelType()== TLINK.TlinkType.UNDEF)
                        continue;
                }
                String feat = ExtractFeatures(docGold,ec1,ec2);
                Pair<String,String> result = testClassifier(feat);
                String goldLabel = result.getFirst();
                String predLabel = result.getSecond();
                /*Filter out E-E links that: 1. E1_FIRST=NO or 2. sent diff>=2 and E_between=yes*/
                if(test_filter_on&&
                        (feat.contains("E1_FIRST:NO")
                        ||(Math.abs(docGold.getSentId(ec1) - docGold.getSentId(ec2)) >= ignore_edge)&&feat.contains("E_BETWEEN:YES")))
                {
                    continue;
                }
                boolean is_ignore = false;
                for(String ignore:label_ignores){
                    if(!predLabel.equals(ignore))
                        continue;
                    is_ignore = true;
                    break;
                }
                if(is_ignore||predLabel.toLowerCase().equals("undef"))//don't store undefined tlinks
                    continue;
                TLINK tmp = new TLINK(lid++,"", TempEval3Reader.Type_Event,TempEval3Reader.Type_Event,ec1.getEiid(),ec2.getEiid(),
                        TLINK.TlinkType.str2TlinkType(predLabel));
                predTlinks.add(tmp);
            }
        }
        docPred.setBodyTlinks(predTlinks);
        return docPred;
    }
    public static void main(String[] args) throws Exception {
        boolean retrain = true;

        /*test set loading*/
        List<TemporalDocument> testset = TempEval3Reader.deserialize(TempEval3Reader.label2dir("platinum"));

        List<TemporalDocument> trainset;
        if (retrain) {
            trainset = TempEval3Reader.deserialize(new String[]{
                    TempEval3Reader.label2dir("timebank",false),
                    TempEval3Reader.label2dir("aquaint",false)});
            for(TemporalDocument doc:trainset){
                doc.saturateTlinks(1,false,true);
            }
        } else
            trainset = null;

        ResourceManager rm = new ClassifierConfigurator().getDefaultConfig();
        String modelname = LocalEEClassifierExp.addVagueTlinks?
                (rm.getString("eeModelName_none")+"_"+LocalEEClassifierExp.vague_portion)
                :rm.getString("eeModelName");
        modelname += "noBethardChambers";
        LocalEEClassifierExp tester = new LocalEEClassifierExp(trainset, testset,rm.getString("eeModelDirPath"),modelname);
        if (retrain) {
            tester.trainClassifier(ParamLBJ.eeLearningRound);
            tester.writeModelsToDisk();
            tester.TestOnTrain();
        }
        tester.testClassifier();

        /*For official evaluator*/
        /*for(TemporalDocument doc:testset){
            TemporalDocument docPred = tester.testOnDoc(doc,TLINK.ignore_tlink);
            //----Don't add E-T links
            docPred.removeETlinks();
            docPred.removeTTlinks();
            doc.removeETlinks();
            doc.removeTTlinks();
            //----Set gold E-T links
            //docPred.getBodyTlinks().addAll(doc.getETlinks());
            docPred.temporalDocumentToText("./output/pred/"+docPred.getDocID()+".tml");
            doc.temporalDocumentToText("./output/gold/"+doc.getDocID()+".tml");
        }*/

        /*Saturate predicted documents then compare*/
        /*PrecisionRecallManager evaluator = new PrecisionRecallManager();
        for(TemporalDocument doc:testset){
            TemporalDocument docPred = tester.testOnDoc(doc,new String[]{TLINK.TlinkType.UNDEF.toStringfull()});
            doc.saturateTlinks();
            List<TLINK> newTlinks = doc.getETlinks();
            newTlinks.addAll(docPred.getBodyTlinks());
            docPred.setBodyTlinks(newTlinks);
            docPred.orderTimexes();
            docPred.saturateTlinks();
            List<Pair<String,String>> results = TempDocEval.evalEEBetweenDocs(doc,docPred,new String[]{TLINK.TlinkType.UNDEF.toStringfull()});
            for(Pair<String,String> res:results){
                evaluator.addPredGoldLabels(res.getSecond(),res.getFirst());
            }
        }
        evaluator.printPrecisionRecall(new String[]{TLINK.TlinkType.UNDEF.toStringfull()});*/

    }

}
