package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.classifier.FeatureExtractor;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ClassifierConfigurator;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.timeline.LocalEEClassifierExp;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.util.List;

import static edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader.label2dir;

/**
 * Created by qning2 on 1/12/17.
 */
public class testNONEvsDIST {
    public int granularity;
    public int N;
    public int[] count_before;
    public int[] count_after;
    public int[] count_includes;
    public int[] count_included;
    public int[] count_equal;
    public int[] count_none;
    public int[] count_before_all;
    public int[] count_after_all;
    public int[] count_includes_all;
    public int[] count_included_all;
    public int[] count_equal_all;
    public int[] count_none_all;
    public int[] count_all;


    public testNONEvsDIST(int granularity, int N) {
        this.granularity = granularity;
        this.N = N;
        count_none = new int[N];
        count_before = new int[N];
        count_after = new int[N];
        count_includes = new int[N];
        count_included = new int[N];
        count_equal = new int[N];
        count_none_all = new int[N];
        count_before_all = new int[N];
        count_after_all = new int[N];
        count_includes_all = new int[N];
        count_included_all = new int[N];
        count_equal_all = new int[N];
        count_all = new int[N];
    }
    public void add(int[] histogram, int k){
        if(k<0)
            System.exit(-1);
        int ind = (int)(1.0*k/granularity);
        if(ind>=N)
            histogram[N-1]++;
        else{
            histogram[ind]++;
        }
    }
    public void printLabel(int[] histogram, int[] histogram_all){
        System.out.println("granularity="+granularity);
        System.out.println("capacity="+N);
        for(int i=0;i<N-1;i++){
            System.out.printf("Distance [%d,%d]: %d/%d=%.4f\n",
                    i*granularity, ((i+1)*granularity-1),
                    histogram[i], histogram_all[i],
                    1.0*histogram[i]/histogram_all[i]);
        }
        System.out.printf("Distance [%d,inf): %d/%d=%.4f\n",
                (N-1)*granularity,
                histogram[N-1], histogram_all[N-1],
                1.0*histogram[N-1]/histogram_all[N-1]);
    }
    public void printPrecisionAll(){
        System.out.println("----Before----");
        printLabel(count_before,count_before_all);
        System.out.println("----After----");
        printLabel(count_after,count_after_all);
        System.out.println("----Includes----");
        printLabel(count_includes,count_includes_all);
        System.out.println("----Included----");
        printLabel(count_included,count_included_all);
        System.out.println("----Equal----");
        printLabel(count_equal,count_equal_all);
        System.out.println("----None----");
        printLabel(count_none,count_none_all);
    }
    public void printPortionAll(){
        System.out.println("----Before----");
        printLabel(count_before,count_all);
        System.out.println("----After----");
        printLabel(count_after,count_all);
        System.out.println("----Includes----");
        printLabel(count_includes,count_all);
        System.out.println("----Included----");
        printLabel(count_included,count_all);
        System.out.println("----Equal----");
        printLabel(count_equal,count_all);
        System.out.println("----None----");
        printLabel(count_none,count_all);
    }
    public void countLABELvsDIST(TemporalDocument doc, boolean original_or_not, boolean sentId_or_not){
        List<EventChunk> eventChunks = doc.getBodyEventMentions();
        for(EventChunk ec1:eventChunks){
            for(EventChunk ec2:eventChunks){
                if(ec1==ec2){
                    continue;
                }
                int k = sentId_or_not? doc.getSentId(ec1)-doc.getSentId(ec2)
                                      :eventChunks.indexOf(ec1) - eventChunks.indexOf(ec2);
                TLINK tlink = doc.getTlink(ec1,ec2,original_or_not);
                if(tlink==null
                        ||tlink.getReducedRelType()== TLINK.TlinkType.UNDEF){
                    add(count_none,k>=0?k:-k);
                }
                else if(tlink.getReducedRelType()== TLINK.TlinkType.BEFORE){
                    add(count_before,k>=0?k:-k);
                }
                else if(tlink.getReducedRelType()== TLINK.TlinkType.AFTER){
                    add(count_after,k>=0?k:-k);
                }
                else if(tlink.getReducedRelType()== TLINK.TlinkType.INCLUDES){
                    add(count_includes,k>=0?k:-k);
                }
                else if(tlink.getReducedRelType()== TLINK.TlinkType.IS_INCLUDED){
                    add(count_included,k>=0?k:-k);
                }
                else if(tlink.getReducedRelType()== TLINK.TlinkType.EQUAL){
                    add(count_equal,k>=0?k:-k);
                }
                add(count_all,k>=0?k:-k);
            }
        }
    }

    /*Precision of local classifier*/
    public void countPREDvsGOLD(TemporalDocument doc, LocalEEClassifierExp classifier, FeatureExtractor featureExtractor){
        List<EventChunk> eventChunks = doc.getBodyEventMentions();
        for(EventChunk ec1:eventChunks){
            for(EventChunk ec2:eventChunks){
                if(ec1==ec2){
                    continue;
                }
                int k = eventChunks.indexOf(ec1) - eventChunks.indexOf(ec2);
                TLINK tlink = doc.getTlink(ec1,ec2);//gold
                String feat = featureExtractor.getFeatureString(featureExtractor.extractEEfeats(ec1, ec2),
                        tlink==null? TLINK.TlinkType.UNDEF.toString():tlink.getReducedRelType().toStringfull());
                Pair<String,String> result = classifier.testClassifier(feat);
                if(result.getFirst().equals(result.getSecond())){
                    switch(result.getFirst()){
                        case "before":
                            add(count_before,k>=0?k:-k);
                            break;
                        case "after":
                            add(count_after,k>=0?k:-k);
                            break;
                        case "includes":
                            add(count_includes,k>=0?k:-k);
                            break;
                        case "included":
                            add(count_included,k>=0?k:-k);
                            break;
                        case "equal":
                            add(count_equal,k>=0?k:-k);
                            break;
                        default:
                            add(count_none,k>=0?k:-k);
                    }
                }
                switch(result.getSecond()){
                    case "before":
                        add(count_before_all,k>=0?k:-k);
                        break;
                    case "after":
                        add(count_after_all,k>=0?k:-k);
                        break;
                    case "includes":
                        add(count_includes_all,k>=0?k:-k);
                        break;
                    case "included":
                        add(count_included_all,k>=0?k:-k);
                        break;
                    case "equal":
                        add(count_equal_all,k>=0?k:-k);
                        break;
                    default:
                        add(count_none_all,k>=0?k:-k);
                }
                add(count_all,k>=0?k:-k);
            }
        }
    }

    public static void main(String[] args) throws Exception{
        List<TemporalDocument> docs = TempEval3Reader.deserialize(label2dir("platinum"),Integer.MAX_VALUE,false);

        ResourceManager rm = new ClassifierConfigurator().getDefaultConfig();
        LocalEEClassifierExp.addVagueTlinks = false;
        LocalEEClassifierExp localEE = new LocalEEClassifierExp(null, null,
                rm.getString("eeModelDirPath"),rm.getString("eeModelName_none"));

        testNONEvsDIST tester = new testNONEvsDIST(1,10);
        for(TemporalDocument doc:docs){
            /*FeatureExtractor featureExtractor = new FeatureExtractor(doc);
            tester.countPREDvsGOLD(doc,localEE,featureExtractor);*/
            tester.countLABELvsDIST(doc,true,true);
        }
        //tester.printPrecisionAll();
        tester.printPortionAll();
    }
}
