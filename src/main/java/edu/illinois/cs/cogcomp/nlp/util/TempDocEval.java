package edu.illinois.cs.cogcomp.nlp.util;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK.TlinkType;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 12/20/16.
 */
public class TempDocEval {
    public static Pair<Integer,Integer> McNemarTest(TemporalDocument docGold, TemporalDocument doc1, TemporalDocument doc2){
        int n01=0;//doc1 wrong, doc2 correct
        int n10=0;//doc1 correct, doc2 wrong
        for(EventChunk ec1:docGold.getBodyEventMentions()){
            for(EventChunk ec2:docGold.getBodyEventMentions()){
                if(ec1==ec2)
                    continue;
                TLINK tlink_gold = docGold.getTlink(ec1,ec2);
                if(tlink_gold==null || tlink_gold.getReducedRelType()==TlinkType.UNDEF)
                    continue;
                TLINK tlink1 = doc1.getTlink(ec1,ec2);
                TLINK tlink2 = doc2.getTlink(ec1,ec2);

                String rel_gold = tlink_gold.getReducedRelType().toStringfull();
                String rel1 = tlink1==null?TLINK.TlinkType.UNDEF.toStringfull():tlink1.getReducedRelType().toStringfull();
                String rel2 = tlink2==null?TLINK.TlinkType.UNDEF.toStringfull():tlink2.getReducedRelType().toStringfull();
                if(rel1.equals(rel_gold)&&!rel2.equals(rel_gold))
                    n10++;
                else if(!rel1.equals(rel_gold)&&rel2.equals(rel_gold))
                    n01++;
            }
        }

        return new Pair<>(n01,n10);
    }
    /*Assume event extraction is perfect*/
    public static List<Pair<String,String>> evalEEBetweenDocs(TemporalDocument docGold, TemporalDocument docPred,String[] ignore_label) throws Exception{
        List<Pair<String,String>> results = new ArrayList<>();
        for(EventChunk ec1:docGold.getBodyEventMentions()){
            for(EventChunk ec2:docGold.getBodyEventMentions()){
                if(ec1==ec2)
                    continue;
                TLINK tlink1 = docGold.getTlink(ec1,ec2);
                TLINK tlink2 = docPred.getTlink(ec1,ec2);
                String gold = tlink1==null?TLINK.TlinkType.UNDEF.toStringfull():tlink1.getReducedRelType().toStringfull();
                String pred = tlink2==null?TLINK.TlinkType.UNDEF.toStringfull():tlink2.getReducedRelType().toStringfull();
                boolean gold_is_ignore = false;
                boolean pred_is_ignore = false;
                for(String ignore:ignore_label){
                    if(gold.equals(ignore)){
                        gold_is_ignore = true;
                    }
                    if(pred.equals(ignore)){
                        pred_is_ignore = true;
                    }
                    if(gold_is_ignore&&pred_is_ignore)
                        break;
                }
                if(gold_is_ignore&&pred_is_ignore){
                    continue;
                }
                results.add(new Pair<String,String>(gold,pred));
            }
        }
        return results;
    }

    /*Assume event extraction is perfect*/
    /*Evaluator for comparison with Chambers: following his metric*/
    public static List<Pair<String,String>> evalBetweenDocs_Chambers(TemporalDocument docGold, TemporalDocument docPred) throws Exception{
        List<Pair<String,String>> results = new ArrayList<>();
        List<TLINK> goldlinks = docGold.getBodyTlinks();
        for(TLINK tlink:goldlinks){
            String gold = tlink.getReducedRelType().toStringfull();
            Object o1 = tlink.getSourceType().equals(TempEval3Reader.Type_Event)?
                    docPred.getEventMentionFromEIID(tlink.getSourceId()):
                    docPred.getTimexMentionFromTID(tlink.getSourceId());
            Object o2 = tlink.getTargetType().equals(TempEval3Reader.Type_Event)?
                    docPred.getEventMentionFromEIID(tlink.getTargetId()):
                    docPred.getTimexMentionFromTID(tlink.getTargetId());
            if(o1 instanceof TemporalJointChunk && o2 instanceof TemporalJointChunk)
                System.out.print("");
            TLINK predTlink = docPred.getTlink(o1,o2);
            if(predTlink==null) {
                predTlink = docPred.getTlink(o2, o1);
                String pred = predTlink==null?
                        TLINK.TlinkType.UNDEF.toStringfull():
                        predTlink.getReducedRelType().reverse().toStringfull();
                results.add(new Pair<String,String>(gold,pred));
            }
            else{
                String pred = predTlink.getReducedRelType().toStringfull();
                results.add(new Pair<String,String>(gold,pred));
            }

        }
        return results;
    }

    /*Assume two docs only differ in norm values*/
    public static List<Pair<String,String>> evalDateBetweenDocs(TemporalDocument docGold, TemporalDocument docPred){
        List<Pair<String,String>> results = new ArrayList<>();
        List<TemporalJointChunk> GOLD = docGold.getBodyTimexMentions();
        List<TemporalJointChunk> PRED = docPred.getBodyTimexMentions();
        String dct_str = docGold.getDocumentCreationTime().getResult().getValue();
        int idx = dct_str.indexOf("T");
        if(idx!=-1)
            dct_str = dct_str.substring(0,idx);
        mySimpleDate dct = mySimpleDate.String2Date(dct_str);
        for(TemporalJointChunk gold:GOLD){
            String gold_str = gold.getResult().getValue();
            mySimpleDate gold_date = gold_str.equals("PRESENT_REF")?
                    dct:mySimpleDate.String2Date(gold_str);
            if(gold_date==null)
                continue;

            String pred_str = PRED.get(GOLD.indexOf(gold)).getResult().getValue();

            if (gold_str.equals(pred_str)
                    || TlinkType.EQUAL == gold_date.compareDate(mySimpleDate.String2Date(pred_str))) {
                results.add(new Pair<String, String>("true", "true"));
            }
            else {
                results.add(new Pair<String, String>("true", "false"));
            }

        }
        return results;
    }
}
