package edu.illinois.cs.cogcomp.nlp.CompareCAVEO;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.MultiClassifiers;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ee.ee_perceptron;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.ReaderConfigurator;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.timeline.LocalEEClassifierExp;
import edu.illinois.cs.cogcomp.nlp.timeline.LocalETClassifierExp;
import edu.illinois.cs.cogcomp.nlp.timeline.test.sieve_output;
import edu.illinois.cs.cogcomp.nlp.util.PrecisionRecallManager;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import jdk.nashorn.internal.objects.Global;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by qning2 on 4/5/17.
 */
public class CoDL {
    public int MAX_EVENTS = 60;
    public boolean debug = false;
    public boolean tdtest_or_platinum = true;//true: TD-Test. false: platinum
    public double lambda;
    public double kl_threshold;
    public ResourceManager rm = new ClassifierConfigurator().getDefaultConfig();
    public String ee_on_T_modelName = "ee_on_T";
    public MultiClassifiers ee_comb;
    public List<TemporalDocument> Unlabeled = new ArrayList<>();
    public List<TemporalDocument> Unlabeled_T = new ArrayList<>();
    public List<TemporalDocument> Platinum = new ArrayList<>();
    public CoDL(double lambda, double kl_threshold) {
        this.lambda = lambda;
        this.kl_threshold = kl_threshold;
        ee_comb = new MultiClassifiers(lambda);
    }
    public void loadModelOnL() throws Exception{
        String dir = rm.getString("eeModelDirPath");
        String name = rm.getString("eeModelName")+"_dense";
        String modelPath = dir+name+".lc";
        String lexPath = dir+name+".lex";
        ee_comb.addClassifier(new ee_perceptron(modelPath,lexPath));
        ee_comb.addClassifier(new ee_perceptron(modelPath,lexPath));
    }
    public void loadModelOnT(int iter) throws Exception{
        // the model path might be wrong
        String dir = rm.getString("eeModelDirPath");
        String modelPath = dir+ee_on_T_modelName+iter+".lc";
        String lexPath = dir+ee_on_T_modelName+iter+".lex";
        ee_comb.dropClassifier();
        ee_comb.addClassifier(new ee_perceptron(modelPath,lexPath));
    }
    public void loadUnlabeled(String dir) throws Exception{
        if(Unlabeled == null)
            Unlabeled = new ArrayList<>();
        File file = new File(dir);
        File[] filelist = file.listFiles();
        if(filelist==null) {
            System.out.println(dir+" is empty.");
            return;
        }
        System.out.println("Loading from "+dir+". In total "+filelist.length+" files.");
        int cnt = 0;
        for(File f:filelist){
            if(f.isFile()) {
                //System.out.println(f.getName());
                //deserialize doc
                TemporalDocument doc = TemporalDocument.deserialize(dir,f.getName().substring(0,f.getName().indexOf(".ser")),false);
                //<max events
                if(doc.getBodyEventMentions().size()>MAX_EVENTS){
                    System.out.println("Too many events. "+doc.getDocID()+" is skipped.");
                    continue;
                }
                //annotation exists
                if(doc.getTextAnnotation()==null){
                    System.out.println("TextAnnotation doesn't exist. "+doc.getDocID()+" is skipped.");
                    continue;
                }
                if(debug) {
                    cnt++;
                    if(cnt>6)
                        break;
                }

                Unlabeled.add(doc);
            }
        }
    }
    public void loadPlatinum() throws Exception{
        Platinum = new ArrayList<>();
        List<TemporalDocument> chambers;
        if(tdtest_or_platinum)
            chambers = TempEval3Reader.deserialize(TempEval3Reader.label2dir("chambers_only"));
        else
            chambers = TempEval3Reader.deserialize(TempEval3Reader.label2dir("platinum"));
        for(TemporalDocument doc:chambers){
            if(tdtest_or_platinum) {
                switch (TBDense_split.findDoc(doc.getDocID())) {
                    case 3:
                        Platinum.add(doc);
                        break;
                    default:
                }
            }
            else
                Platinum.add(doc);
        }
    }
    public void processUnlabeled(int iter) throws Exception{
        Unlabeled_T = new ArrayList<>();
        GlobalEE exp;
        exp = new GlobalEE(Unlabeled,ee_comb);
        GlobalEE.CAVEO_or_LpI = false;
        GlobalEE.use_CAVEO_ettt = false;
        GlobalEE.remove_vague = true;
        GlobalEE.knownNONEs = false;
        GlobalEE.useBaselineProb = false;
        exp.serialize = true;
        if(iter==0)
            exp.cacheDir = "serialized_data/Chambers/CoDL/"+"iter"+iter+"/"+"kl"+kl_threshold+"/";
        else
            exp.cacheDir = "serialized_data/Chambers/CoDL/"+"iter"+iter+"/"+"kl"+kl_threshold+"/"+"lambda"+lambda+"/";
        exp.force_update = false;
        exp.kl_threshold = kl_threshold;
        exp.sentDistFilter = true;
        List<TemporalDocument> solvedDocs = exp.solve();
        /*Remove null docs*/
        for(TemporalDocument doc:solvedDocs){
            if(doc==null)
                continue;
            Unlabeled_T.add(doc);
        }
    }
    public ee_perceptron trainModelOnT(int iter, int round){
        LocalEEClassifierExp.vague_portion = 0.05;
        LocalEEClassifierExp.addVagueTlinks = false;
        LocalEEClassifierExp.feat_filter_on = true;
        LocalEEClassifierExp.ignore_edge = 2;
        String dir = rm.getString("eeModelDirPath")
                +File.separator+"codl_chambers"
                +File.separator+String.valueOf(kl_threshold)+"_"+String.valueOf(lambda);
        IOUtils.mkdir(dir);
        LocalEEClassifierExp tester = new LocalEEClassifierExp(Unlabeled_T, null,
                dir, ee_on_T_modelName+iter);
        tester.trainClassifier(round);
        tester.writeModelsToDisk();
        System.out.println("-----Performance of local EE-----");
        System.out.println("Iter="+iter);
        tester.TestOnTrain();
        return tester.getClassifier();
    }
    public void testModelOnP(String dir) throws Exception{
        GlobalEE exp;
        exp = new GlobalEE(Platinum,ee_comb);
        GlobalEE.CAVEO_or_LpI = false;
        GlobalEE.use_CAVEO_ettt = true;
        GlobalEE.remove_vague = true;
        GlobalEE.knownNONEs = false;
        GlobalEE.useBaselineProb = false;
        exp.serialize = true;
        if(tdtest_or_platinum)
            exp.cacheDir = "serialized_data/Chambers/CoDL/TD-Test/"+"lambda"+String.valueOf(lambda)+"_"+
                    "kl"+String.valueOf(kl_threshold)+"/";
        else
            exp.cacheDir = "serialized_data/Chambers/CoDL/Platinum/"+"lambda"+String.valueOf(lambda)+"_"+
                    "kl"+String.valueOf(kl_threshold)+"/";
        exp.force_update = false;
        exp.kl_threshold = kl_threshold;
        exp.sentDistFilter = true;

        HashMap<String,List<TLINK>> CAVEO_output;
        if(tdtest_or_platinum)
            CAVEO_output = sieve_output.get_CAVEO_output();
        else
            CAVEO_output = sieve_output.get_CAVEO_output("serialized_data/Chambers/sieve_output_te3platinum.ser","platinum");
        exp.CAVEO_output = CAVEO_output;

        List<TemporalDocument> predPlatinum = exp.solve();
        List<TemporalDocument> kbcom = TempEval3Reader.deserialize("/home/qning2/Research/KBconstruction/serialized_data/TBDense-SRLEvent/best/noClustering_allLabels_vagueCorr2_regGloVe42BK1000");
        //List<TemporalDocument> kbcom = TempEval3Reader.deserialize("/home/qning2/Research/KBconstruction/serialized_data/TBDense-SRLEvent/best/noClustering_allLabels_vagueCorr2_regGloVe42BK1000_newfeat");
        PrecisionRecallManager evaluator = new PrecisionRecallManager();
        PrecisionRecallManager evaluator_kbcom = new PrecisionRecallManager();
        for(TemporalDocument doc:predPlatinum) {
            doc.temporalDocumentToText(dir + File.separator + doc.getDocID() + ".tml");
            TemporalDocument doc2 = new TemporalDocument(doc);
            TemporalDocument tmp=null;
            for(TemporalDocument kbcom_doc:kbcom){
                if(kbcom_doc.getDocID().equals(doc2.getDocID())){
                    tmp = kbcom_doc;
                    break;
                }
            }
            if(tmp==null)
                continue;
            List<TLINK> newtlinks = new ArrayList<>();
            for(TLINK tt:tmp.getBodyTlinks()){
                newtlinks.add(tt);
                newtlinks.add(tt.converse());
            }
            for(TLINK tt:doc2.getBodyTlinks()){
                if(tmp.checkTlinkExistence(tt))
                    continue;
                newtlinks.add(tt);
            }
            doc2.setBodyTlinks(newtlinks);
            doc2.temporalDocumentToText(dir+"_kbcom" + File.separator + doc2.getDocID() + ".tml");
            //doc2.temporalDocumentToText(dir+"_kbcom_newfeat" + File.separator + doc2.getDocID() + ".tml");
            for(TemporalDocument golddoc:Platinum){
                for(TLINK tt:golddoc.getBodyTlinks()){
                    if(!tt.getSourceType().equals(TempEval3Reader.Type_Event)
                            ||!tt.getTargetType().equals(TempEval3Reader.Type_Event))
                        continue;
                    TextAnnotation ta = golddoc.getTextAnnotation();
                    EventChunk ec1 = golddoc.getEventMentionFromEIID(tt.getSourceId());
                    EventChunk ec2 = golddoc.getEventMentionFromEIID(tt.getTargetId());
                    int sentid1 = ta.getSentenceId(ta.getTokenIdFromCharacterOffset(ec1.getCharStart()));
                    int sentid2 = ta.getSentenceId(ta.getTokenIdFromCharacterOffset(ec2.getCharStart()));
                    if(Math.abs(sentid1-sentid2)>1)
                        continue;

                    String gold = tt.getReducedRelType().toStringfull();
                    TLINK.TlinkType predtt = doc.getTlinkType_general(doc.getEventMentionFromEIID(tt.getSourceId()),doc.getEventMentionFromEIID(tt.getTargetId()));
                    String pred = predtt==null? TLINK.TlinkType.UNDEF.toStringfull():predtt.toStringfull();
                    TLINK.TlinkType predtt_kbcom = doc2.getTlinkType_general(doc2.getEventMentionFromEIID(tt.getSourceId()),doc2.getEventMentionFromEIID(tt.getTargetId()));
                    String pred_kbcom = predtt_kbcom==null? TLINK.TlinkType.UNDEF.toStringfull():predtt_kbcom.toStringfull();
                    evaluator.addPredGoldLabels(pred,gold);
                    evaluator_kbcom.addPredGoldLabels(pred_kbcom,gold);
                }
            }
        }
        evaluator.printPrecisionRecall(new String[]{TLINK.TlinkType.UNDEF.toStringfull()});
        evaluator_kbcom.printPrecisionRecall(new String[]{TLINK.TlinkType.UNDEF.toStringfull()});
    }

    public static void main(String[] args) throws Exception{
        double lambda = Double.valueOf(args[0]);
        double kl_th = Double.valueOf(args[1]);
        int maxIter = Integer.valueOf(args[2]);
        int silver = Integer.valueOf(args[3]);// silver has to be 1~6
        boolean retrain = Boolean.valueOf(args[4]);

        CoDL exp = new CoDL(lambda,kl_th);
        System.out.printf("--------Configuration--------\nlambda=%.2f\nKL threshold=%.2f\nmaxIter=%d\n",
                lambda,kl_th,maxIter);

        int train_rounds = 10;
        exp.loadModelOnL();
        int startIter = 0;
        if(retrain) {
            ResourceManager rm = new ReaderConfigurator().getDefaultConfig();
            //load U
            exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver0_label"));
            if(silver>=2)
                exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver1_label"));
            if(silver>=3)
                exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver2_label"));
            if(silver>=4)
                exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver3_label"));
            if(silver>=5)
                exp.loadUnlabeled(rm.getString("ser_dir")+File.separator+rm.getString("silver4_label"));
            if(silver>=6)
                exp.loadUnlabeled(rm.getString("ser_dir") + File.separator + rm.getString("silver5_label"));
            //load existing model on T
            if(startIter>0)
                exp.loadModelOnT(startIter-1);
            for (int iter = startIter; iter < maxIter; iter++) {
                //proc U
                exp.processUnlabeled(iter);
                //exp.Unlabeled_T = exp.Unlabeled;

                //train on T
                ee_perceptron newclassifier = exp.trainModelOnT(iter, train_rounds);

                //update multiclassifier
                exp.ee_comb.dropClassifier();
                exp.ee_comb.addClassifier(newclassifier);
            }
        }
        else{
            String savedir = exp.rm.getString("eeModelDirPath")
                    +File.separator+"codl_chambers"
                    +File.separator+String.valueOf(kl_th)+"_"+String.valueOf(lambda);
            String modelpath = savedir+File.separator+exp.ee_on_T_modelName+(maxIter-1)+".lc";
            String lexpath = savedir+File.separator+exp.ee_on_T_modelName+(maxIter-1)+".lex";
            exp.ee_comb.dropClassifier();
            exp.ee_comb.addClassifier(new ee_perceptron(modelpath,lexpath));
        }
        String dir;
        if(exp.tdtest_or_platinum)
            dir = "./output/Chambers/codl/TD-Test"+File.separator+
                    String.valueOf(lambda) + "_" +
                    String.valueOf(kl_th) + "_"  +
                    String.valueOf(maxIter);
        else
            dir = "./output/Chambers/codl/Platinum"+File.separator+
                    String.valueOf(lambda) + "_" +
                    String.valueOf(kl_th) + "_"  +
                    String.valueOf(maxIter);
        IOUtils.mkdir(dir);
        IOUtils.mkdir(dir+"_kbcom");
        //IOUtils.mkdir(dir+"_kbcom_newfeat");
        exp.loadPlatinum();
        exp.testModelOnP(dir);

        /*Evaluation*/
        Runtime rt = Runtime.getRuntime();
        String cmd;
        if(exp.tdtest_or_platinum)
            cmd = "sh scripts/evaluate_general.sh ./output/Chambers/gold " + dir + " "+
                    "chambers_codl_tdtest_"+
                    String.valueOf(lambda) + "_" +
                    String.valueOf(kl_th) + "_"  +
                    String.valueOf(maxIter);
        else
            cmd = "sh scripts/evaluate_general.sh ./data/TempEval3/Evaluation/te3-platinum " + dir + " "+
                    "chambers_codl_platinum_"+
                    String.valueOf(lambda) + "_" +
                    String.valueOf(kl_th) + "_"  +
                    String.valueOf(maxIter);
        Process pr = rt.exec(cmd);

        cmd = "sh scripts/evaluate_general.sh ./output/Chambers/gold " + dir+"_kbcom" + " "+
                "chambers_codl_tdtest_"+
                String.valueOf(lambda) + "_" +
                String.valueOf(kl_th) + "_"  +
                String.valueOf(maxIter)+"_kbcom";
        /*cmd = "sh scripts/evaluate_general.sh ./output/Chambers/gold " + dir+"_kbcom_newfeat" + " "+
                "chambers_codl_tdtest_"+
                String.valueOf(lambda) + "_" +
                String.valueOf(kl_th) + "_"  +
                String.valueOf(maxIter)+"_kbcom_newfeat";*/
        Process pr2 = rt.exec(cmd);

    }
}
