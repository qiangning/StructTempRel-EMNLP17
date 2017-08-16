package edu.illinois.cs.cogcomp.nlp.timeline;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.classifier.FeatureExtractor;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ParamLBJ;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.et.et_perceptron;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 12/12/16.
 */
public class LocalETClassifierExp extends LocalClassifier {
    private String modelPath;
    private String modelLexPath;
    public static boolean addVagueTlinks = false;
    public static double vague_portion = 1;
    public static boolean filter_on = true;
    public static boolean knownNONEs = false;

    public LocalETClassifierExp(List<TemporalDocument> traindocs, List<TemporalDocument> testdocs, String modelDirPath, String modelName){
        super(modelDirPath,modelName);
        modelPath = modelDirPath+modelName+".lc";
        modelLexPath = modelDirPath+modelName+".lex";
        classifier = new et_perceptron(modelPath,modelLexPath);
        if(traindocs!=null) {
            for (TemporalDocument doc : traindocs) {
                addTrainFeats(doc);
            }
        }
        if(testdocs!=null) {
            for (TemporalDocument doc : testdocs) {
                addTestFeats(doc);
            }
        }
    }
    @Override
    public List<String> ExtractFeatures(TemporalDocument doc){
        FeatureExtractor featureExtractor = new FeatureExtractor(doc);
        featureExtractor.extractETfeats_doc();
        if(addVagueTlinks)
            featureExtractor.extractETfeats_Vague_doc(vague_portion);
        return featureExtractor.getEtFeatures();
    }
    public String ExtractFeatures(TemporalDocument doc, EventChunk ec, TemporalJointChunk tjc){
        FeatureExtractor extractor = getFeatureExtractor(doc);
        TLINK tlink = doc.getTlink(ec,tjc);
        String label = tlink==null?TLINK.TlinkType.UNDEF.toStringfull():tlink.getReducedRelType().toStringfull();
        return extractor.getFeatureString(extractor.extractETfeats(ec, tjc),label);
    }

    public void clearDCTTlinks(){
        List<String> new_testfeats = new ArrayList<>();
        for(String s:testfeats){
            if(s.contains("IS_DCT:false"))
                new_testfeats.add(s);
        }
        testfeats = new_testfeats;
    }

    public TemporalDocument testOnDoc(TemporalDocument docGold, String[] label_ignores){
        TemporalDocument docPred = new TemporalDocument(docGold);
        docPred.setBodyTlinks(null);
        List<TLINK> predTlinks = new ArrayList<>();
        int lid = 0;
        int count_ignore=0;
        int count_none=0;
        int count=0;
        for(EventChunk ec:docGold.getBodyEventMentions()){
            List<TemporalJointChunk> timex2check = docGold.deepCopyTimex();
            timex2check.add(docGold.getDocumentCreationTime());
            for(TemporalJointChunk tjc:timex2check){
                if(knownNONEs){
                    TLINK tlink = docGold.getTlink(ec,tjc,true);
                    if(tlink==null||tlink.getReducedRelType()== TLINK.TlinkType.UNDEF)
                        continue;
                }
                count++;
                String feat = ExtractFeatures(docGold,ec,tjc);
                Pair<String,String> result = testClassifier(feat);
                String goldLabel = result.getFirst();
                String predLabel = result.getSecond();
                /*Filter out E-T links that: 1. T is not DCT and 2. E-T is far away and  3. E-T is not closest*/
                if(filter_on
                        &&feat.contains("IS_DCT:false")
                        && (feat.contains("NUM_SENT_DIFF:SOME")||feat.contains("NUM_SENT_DIFF:ALOT")||feat.contains("NUM_SENT_DIFF:TWO")||feat.contains("NUM_SENT_DIFF:ONE"))
                        /*&& feat.contains("CLOSEST:false")*/) {
                    count_ignore++;
                    if(goldLabel.equals(TLINK.TlinkType.UNDEF.toStringfull()))
                        count_none++;
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
                TLINK tmp = new TLINK(lid++,"", TempEval3Reader.Type_Event,TempEval3Reader.Type_Timex,ec.getEiid(),tjc.getTID(),
                        TLINK.TlinkType.str2TlinkType(predLabel));
                predTlinks.add(tmp);
            }
        }
        docPred.setBodyTlinks(predTlinks);
        //System.out.printf("%s: ignore=%d/%d, none=%d/%d\n",docGold.getDocID(),count_ignore,count,count_none,count);
        return docPred;
    }
    public static void addPredETlinks(TemporalDocument docGold, TemporalDocument docPred, boolean knownNONEs){
        ResourceManager rm = new ClassifierConfigurator().getDefaultConfig();
        LocalETClassifierExp tester = new LocalETClassifierExp(null, null, rm.getString("etModelDirPath"), rm.getString("etModelName"));
        LocalETClassifierExp.filter_on = !knownNONEs;
        LocalETClassifierExp.addVagueTlinks = false;
        LocalETClassifierExp.knownNONEs = knownNONEs;
        TemporalDocument etpred;
        if(knownNONEs)
            etpred = tester.testOnDoc(docGold,TLINK.ignore_tlink);
        else
            etpred = tester.testOnDoc(docPred,TLINK.ignore_tlink);
        List<TLINK> newtlinks = docPred.getBodyTlinks();
        newtlinks.addAll(etpred.getETlinks());
        docPred.setBodyTlinks(newtlinks);
    }
    public static void main(String[] args) throws Exception{
        boolean retrain = false;

        /*test set loading*/
        TempEval3Reader testReader;
        testReader = new TempEval3Reader("TIMEML","te3-platinum","data/TempEval3/Evaluation/");
        testReader.ReadData();
        testReader.removeDuplicatedTlinks();
        /*testReader.removeEElinks();
        testReader.removeTTlinks();
        testReader.saturateTlinks();*/
        testReader.addVagueTlinks(1);
        testReader.createTextAnnotation();
        List<TemporalDocument> testset = testReader.getDataset().getDocuments();

        List<TemporalDocument> trainset;
        if(retrain) {
            TempEval3Reader trainReader1;
            trainReader1 = new TempEval3Reader("TIMEML", "TimeBank", "data/TempEval3/Training/TBAQ-cleaned/");
            trainReader1.ReadData();
            trainReader1.readBethard();
            trainReader1.readChambers();
            trainReader1.orderTimexes();
            trainReader1.saturateTlinks(1,false);
            //trainReader1.addVagueTlinks(0.005);
            trainReader1.createTextAnnotation();
            trainset = trainReader1.getDataset().getDocuments();

            TempEval3Reader trainReader2;
            trainReader2 = new TempEval3Reader("TIMEML", "AQUAINT", "data/TempEval3/Training/TBAQ-cleaned/");
            trainReader2.ReadData();
            trainReader2.readBethard();
            trainReader2.readChambers();
            trainReader2.orderTimexes();
            trainReader2.saturateTlinks(1,false);
            //trainReader2.addVagueTlinks(0.005);
            trainReader2.createTextAnnotation();
            trainset.addAll(trainReader2.getDataset().getDocuments());
        }
        else
            trainset = testset;


        ResourceManager rm = new ClassifierConfigurator().getDefaultConfig();
        String modelname = LocalETClassifierExp.addVagueTlinks?
                (rm.getString("etModelName_none")+"_"+LocalETClassifierExp.vague_portion)
                :rm.getString("etModelName");
        String modelname_ee = LocalEEClassifierExp.addVagueTlinks?
                (rm.getString("eeModelName_none")+"_"+LocalEEClassifierExp.vague_portion)
                :rm.getString("eeModelName");
        LocalETClassifierExp tester = new LocalETClassifierExp(trainset, testset, rm.getString("etModelDirPath"), modelname);
        LocalEEClassifierExp tester_ee = new LocalEEClassifierExp(trainset, testset,rm.getString("eeModelDirPath"),modelname_ee);
        if(retrain) {
            tester.trainClassifier(ParamLBJ.etLearningRound);
            tester.writeModelsToDisk();
            tester.TestOnTrain();
        }
        tester.testClassifier();
        /*For official evaluator*/
        for(TemporalDocument doc:testset){
            TemporalDocument docPred = tester.testOnDoc(doc,TLINK.ignore_tlink);

            //----Set prediced E-E links
            List<TLINK> etlinks = docPred.getETlinks();
            TemporalDocument docPred_ee = tester_ee.testOnDoc(doc,TLINK.ignore_tlink);
            List<TLINK> eelinks = docPred_ee.getEElinks();
            List<TLINK> goldeelinks = doc.getEElinks();
            List<TLINK> newtlinks = etlinks;
            newtlinks.addAll(eelinks);
            docPred.setBodyTlinks(newtlinks);

            //----Set gold E-E links
            //newtlinks.addAll(goldeelinks);

            //----Don't add E-E links
            /*docPred.removeEElinks();
            docPred.removeTTlinks();
            doc.removeEElinks();
            doc.removeTTlinks();*/

            docPred.temporalDocumentToText("./output/pred/"+docPred.getDocID()+".tml");
            doc.temporalDocumentToText("./output/gold/"+doc.getDocID()+".tml");
        }
    }
}
