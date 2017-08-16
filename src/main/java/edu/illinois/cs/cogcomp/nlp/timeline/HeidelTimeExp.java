package edu.illinois.cs.cogcomp.nlp.timeline;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK.TlinkType;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.timeline.test.DocSanityCheck;
import edu.illinois.cs.cogcomp.nlp.util.ExecutionTimeUtil;
import edu.illinois.cs.cogcomp.nlp.util.PrecisionRecallManager;
import edu.illinois.cs.cogcomp.nlp.util.TempDocEval;
import edu.illinois.cs.cogcomp.nlp.util.mySimpleDate;
import edu.stanford.nlp.time.SUTime;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import edu.uw.cs.lil.uwtime.learn.temporal.MentionResult;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by qning2 on 1/7/17.
 */
public class HeidelTimeExp {
    public static boolean DCT_as_REF = true;
    /*processDoc() deep copies timex
    * It only chooses previous timex or DCT as ref*/
    public static TemporalDocument processDoc(TemporalDocument doc) throws Exception{
        TemporalDocument newdoc = new TemporalDocument(doc);
        List<TemporalJointChunk> bodyTimexMentions = doc.deepCopyTimex();
        /*Get dct*/
        String dct_str = newdoc.getDocumentCreationTime().getResult().getValue();
        int idx = dct_str.indexOf("T");//some dct also includes time info
        if(idx!=-1)
            dct_str = dct_str.substring(0,idx);
        mySimpleDate dct = mySimpleDate.String2Date(dct_str);

        mySimpleDate ref = dct;//init reference time is dct
        for(TemporalJointChunk tjc:bodyTimexMentions){
            String gold_str = tjc.getResult().getValue();
            mySimpleDate gold_date = gold_str.equals("PRESENT_REF")?
                    dct:mySimpleDate.String2Date(gold_str);
            if(gold_date==null)
                continue;

            String tmp = tjc.getPhrase().toString();
            mySimpleDate pred = mySimpleDate.callHeidelTime(tmp,ref);
            if(pred!=null) {
                if(DCT_as_REF)
                    ref = dct;
                else
                    ref = pred;
                MentionResult oldresult = tjc.getResult();
                MentionResult newresult = new MentionResult(oldresult.getChunk(),
                        oldresult.getType(),
                        pred.toString(),
                        oldresult.getMod());
                tjc.setResult(newresult);
                System.out.println("gold="+oldresult.getValue()+",pred="+newresult.getValue());
            }
        }
        newdoc.setBodyTimexMentions(bodyTimexMentions);
        return newdoc;
    }

    /*processDoc_BestRef() deep copies timex and tlink
    * It chooses between previous timex and dct as ref*/
    public static TemporalDocument processDoc_BestRef(TemporalDocument doc, int max_iter)throws Exception{
        TemporalDocument newdoc = HeidelTimeExp.processDoc(doc);//preprocess
        int iter = 0;
        /*Get dct*/
        String dct_str = newdoc.getDocumentCreationTime().getResult().getValue();
        int idx = dct_str.indexOf("T");//some dct also includes time info
        if(idx!=-1)
            dct_str = dct_str.substring(0,idx);
        mySimpleDate dct = mySimpleDate.String2Date(dct_str);
        /*Get the indices of dates*/
        List<Integer> dates_idx = new ArrayList<>();
        List<TemporalJointChunk> bodyTimexMentions = newdoc.getBodyTimexMentions();
        for(TemporalJointChunk tjc:bodyTimexMentions){
            String gold_str = tjc.getResult().getValue();
            mySimpleDate gold_date = gold_str.equals("PRESENT_REF")?
                    dct:mySimpleDate.String2Date(gold_str);
            if(gold_date==null)
                continue;
            dates_idx.add(bodyTimexMentions.indexOf(tjc));
        }
        /*Iteratively determine the best val assignment*/
        while(iter<max_iter){
            iter++;
            for(int id:dates_idx) {
                /*Construct ref list: prev or dct*/
                if(dates_idx.indexOf(id)==0)
                    continue;
                List<mySimpleDate> refs = new ArrayList<>();
                refs.add(dct);
                String prev_str = bodyTimexMentions.get(dates_idx.get(dates_idx.indexOf(id)-1)).getResult().getValue();
                mySimpleDate prev = prev_str.equals("PRESENT_REF")?
                        dct:mySimpleDate.String2Date(prev_str);
                refs.add(prev);
                /*FindBestRef*/
                newdoc = FindBestRef(newdoc,id,refs);
            }
        }
        return newdoc;
    }

    /*FindBestRef() deep copies timex and tlink*/
    public static TemporalDocument FindBestRef(TemporalDocument doc, int target_idx, List<mySimpleDate> refs) throws Exception{
        String LABEL_LOG = "FINDBESTREF_LOG";
        TemporalLog.println(LABEL_LOG,"----------"+doc.getDocID()+"----------");
        TemporalDocument newdoc = new TemporalDocument(doc);
        newdoc.setBodyTimexMentions(doc.deepCopyTimex());
        newdoc.setBodyTlinks(doc.deepCopyTlink());
        /*check if target_idx is valid*/
        int n = doc.getBodyTimexMentions().size();
        if(target_idx<0||target_idx>=n) {
            System.out.println("Invalid target_idx="+target_idx);
            return newdoc;
        }
        TemporalJointChunk target = doc.getBodyTimexMentions().get(target_idx);
        String target_str = target.getPhrase().toString();
        TemporalLog.println(LABEL_LOG,"TARGET TIMEX="+target_str);
        /*Try every ref*/
        List<TemporalJointChunk> resolved_tjc = new ArrayList<>();
        for(mySimpleDate refdate:refs){
            mySimpleDate pred = mySimpleDate.callHeidelTime(target_str,refdate);
            if(pred==null) {
                TemporalLog.println(LABEL_LOG,"\tREF="+refdate.toString()+"-->PRED=NULL");
                continue;
            }
            TemporalLog.println(LABEL_LOG,"\tREF="+refdate.toString()+"-->PRED="+pred.toString());
            TemporalJointChunk new_tjc = target.deepCopy();
            MentionResult oldresult = new_tjc.getResult();
            MentionResult newresult = new MentionResult(oldresult.getChunk(),
                    oldresult.getType(),
                    pred.toString(),
                    oldresult.getMod());
            new_tjc.setResult(newresult);
            resolved_tjc.add(new_tjc);
        }
        /*Remove duplicated resolved_tjc*/
        List<TemporalJointChunk> tmp = new ArrayList<>();
        for(int i=0;i<resolved_tjc.size();i++){
            boolean dup = false;
            TemporalJointChunk tjc1 = resolved_tjc.get(i);
            mySimpleDate date1 = mySimpleDate.String2Date(tjc1.getResult().getValue());
            assert date1!=null;
            /*Keep tjc1 only if it cannot be found later on*/
            for(int j=i+1;j<resolved_tjc.size();j++){
                TemporalJointChunk tjc2 = resolved_tjc.get(j);
                mySimpleDate date2 = mySimpleDate.String2Date(tjc2.getResult().getValue());
                if(date1.compareDate(date2)==TlinkType.EQUAL){
                    dup = true;
                    break;
                }
            }
            if(!dup)
                tmp.add(tjc1);
        }
        resolved_tjc = tmp;
        if(resolved_tjc.size()==0){
            return newdoc;
        }
        /*Count #violation*/
        int min = Integer.MAX_VALUE;
        int best_idx = 0;
        TemporalLog.println(LABEL_LOG,"Count #Violations");
        if(resolved_tjc.size()>1) {
            best_idx = -1;
            for (TemporalJointChunk tjc : resolved_tjc) {
                newdoc.getBodyTimexMentions().set(target_idx, tjc);
                newdoc.setBodyTlinks(doc.deepCopyTlink());//reload original tlinks
                newdoc.orderTimexes();
                newdoc.saturateTlinks();
                int vio = DocSanityCheck.sanityCheck_All(newdoc, false);
                TemporalLog.println(LABEL_LOG, "\tPRED=" + tjc.getResult().getValue() + ", #Violation=" + vio);
                if (vio < min) {
                    min = vio;
                    best_idx = resolved_tjc.indexOf(tjc);
                }
            }
            TemporalLog.println(LABEL_LOG, "RESULT: PRED=" + resolved_tjc.get(best_idx).getResult().getValue()
                    + ", #Violation=" + min);
        }
        else{
            TemporalLog.println(LABEL_LOG, "RESULT: PRED=" + resolved_tjc.get(best_idx).getResult().getValue()
                    + ", #Violation=N/A");
        }
        newdoc.setBodyTlinks(doc.deepCopyTlink());
        newdoc.getBodyTimexMentions().set(target_idx,resolved_tjc.get(best_idx));
        return newdoc;
    }

    public static void evalDocs(List<TemporalDocument> docs)throws Exception{
        String LABEL = "EVALDOCS_LOG";
        PrecisionRecallManager evaluator = new PrecisionRecallManager();
        ExecutionTimeUtil timer = new ExecutionTimeUtil();
        timer.start();
        for(TemporalDocument golddoc:docs){
            TemporalLog.println(LABEL,golddoc.getDocID());
            TemporalDocument preddoc = HeidelTimeExp.processDoc(golddoc);
            preddoc.orderTimexes();
            preddoc.saturateTlinks();

            List<Pair<String,String>> results = TempDocEval.evalDateBetweenDocs(golddoc,preddoc);
            evaluator.addListPairs(results);
            TemporalLog.println(LABEL,golddoc.getDocID()+" #Violation="+DocSanityCheck.sanityCheck_All(preddoc,false));
        }
        timer.end();
        TemporalLog.println(LABEL,"Total time = "+timer.getTimeSeconds());
        TemporalLog.println(LABEL, evaluator.getConfusionMat().toString());
        evaluator.printPrecisionRecall();
        evaluator.printConfusionMatrix();
    }

    public static void evalDocs_BestRef(List<TemporalDocument> docs, int max_iter)throws Exception{
        String LABEL = "EVALDOCS_BESTREF_LOG";
        PrecisionRecallManager evaluator = new PrecisionRecallManager();
        ExecutionTimeUtil timer = new ExecutionTimeUtil();
        timer.start();
        for(TemporalDocument golddoc:docs){
            TemporalLog.println(LABEL,golddoc.getDocID());
            TemporalDocument preddoc = HeidelTimeExp.processDoc_BestRef(golddoc,max_iter);
            List<Pair<String,String>> results = TempDocEval.evalDateBetweenDocs(golddoc,preddoc);
            evaluator.addListPairs(results);
            TemporalLog.println(LABEL,golddoc.getDocID()+" #Violation="+DocSanityCheck.sanityCheck_All(preddoc,false));
        }
        timer.end();
        TemporalLog.println(LABEL,"Total time = "+timer.getTimeSeconds());
        TemporalLog.println(LABEL, evaluator.getConfusionMat().toString());
        evaluator.printPrecisionRecall();
        evaluator.printConfusionMatrix();
    }
    public static void main(String[] args) throws Exception{
        TempEval3Reader myReader;
        //myReader = new TempEval3Reader("TIMEML","te3-platinum","data/TempEval3/Evaluation/");
        myReader = new TempEval3Reader("TIMEML", "TimeBank", "data/TempEval3/Training/TBAQ-cleaned/");
        //myReader = new TempEval3Reader("TIMEML", "AQUAINT", "data/TempEval3/Training/TBAQ-cleaned/");
        myReader.ReadData();
        myReader.saturateTlinks();
        //myReader.getDataset().getDocuments().get(0).saturateTlinks();
        /*Remove all the gold E-E links*/
        /*for(TemporalDocument doc:myReader.getDataset().getDocuments()){
            List<TLINK> et_tlinks = doc.getETlinks();
            doc.setBodyTlinks(et_tlinks);
        }*/
        HeidelTimeExp.evalDocs(myReader.getDataset().getDocuments());
        HeidelTimeExp.evalDocs_BestRef(myReader.getDataset().getDocuments(),2);
    }
}
