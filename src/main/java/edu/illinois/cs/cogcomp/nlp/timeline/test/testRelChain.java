package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.util.ExecutionTimeUtil;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by qning2 on 12/14/16.
 */
public class testRelChain {
    private HashMap<String, HashMap<String,Integer>> successorMat = new HashMap<>();
    private HashMap<String, HashMap<String,Double>> probMat = new HashMap<>();
    public void addCurrNextLabels(String Curr, String Next) {
        if(successorMat.containsKey(Curr)){
            if(successorMat.get(Curr).containsKey(Next)){
                int newval = successorMat.get(Curr).get(Next);
                successorMat.get(Curr).put(Next,newval+1);
            }
            else{
                successorMat.get(Curr).put(Next,1);
            }
        }
        else{
            HashMap<String,Integer> tmpHash = new HashMap<>();
            tmpHash.put(Next,1);
            successorMat.put(Curr,tmpHash);
        }
    }
    public void printSuccessorMat(){
        Set<String> keys = successorMat.keySet();
        for(String key:keys){
            System.out.println(key+"="+successorMat.get(key).toString());
        }
    }
    public void printProbMat(){
        Set<String> keys = successorMat.keySet();
        for(String key:keys){
            probMat.put(key,new HashMap<>());
            Set<String> keys2 = successorMat.get(key).keySet();
            int total = 0;
            for(String key2:keys2)
                total+=successorMat.get(key).get(key2);
            for(String key2:keys2)
                probMat.get(key).put(key2,1.0*successorMat.get(key).get(key2)/total);
            System.out.printf(key+"\t={");
            for(String key2:keys2)
                System.out.printf(key2+"=%.2f\t",probMat.get(key).get(key2));
            System.out.printf("}\n");
        }
    }
    public void reset(){
        successorMat = new HashMap<>();
        probMat = new HashMap<>();
    }
    public static void main(String[] args) throws Exception{
        TempEval3Reader myReader;
        myReader = new TempEval3Reader("TIMEML","te3-platinum","data/TempEval3/Evaluation/");
        //myReader = new TempEval3Reader("TIMEML", "TimeBank", "data/TempEval3/Training/TBAQ-cleaned/");
        //myReader = new TempEval3Reader("TIMEML", "AQUAINT", "data/TempEval3/Training/TBAQ-cleaned/");
        myReader.ReadData();
        myReader.readBethard();
        myReader.readChambers();
        testRelChain tester = new testRelChain();
        for(int k=0;k<20;k++) {
            TemporalDocument doc = myReader.getDataset().getDocuments().get(k);
            TemporalLog.println(doc.getDocID(), "----" + doc.getDocID() + "----");
            doc.saturateTlinks(false);
            int n = doc.getBodyEventMentions().size();
            String prev = "NONE";
            String curr = "";
            for (int i = 0; i < n - 1; i++) {
                EventChunk ec1 = doc.getBodyEventMentions().get(i);
                EventChunk ec2 = doc.getBodyEventMentions().get(i + 1);
                TLINK tlink = doc.getTlink(ec1, ec2);
                if (tlink != null) {
                    curr = tlink.getReducedRelType().toStringfull();
                }
                else {
                    curr = "NONE";
                }
                tester.addCurrNextLabels(prev, curr);
                prev = curr;
                //TemporalLog.println(doc.getDocID(), "(e" + ec1.getEid() + ",e" + ec2.getEid() + ")" + curr);
            }
        }
        tester.printSuccessorMat();
        tester.printProbMat();
    }
}
