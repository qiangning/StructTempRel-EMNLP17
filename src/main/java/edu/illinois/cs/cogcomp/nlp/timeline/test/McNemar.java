package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.timeline.CoDL_Exp;
import edu.illinois.cs.cogcomp.nlp.util.TempDocEval;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.util.HashMap;
import java.util.List;

/**
 * Created by qning2 on 2/5/17.
 */
public class McNemar {
    public List<TemporalDocument> docsGold;
    public List<TemporalDocument> docs1;
    public List<TemporalDocument> docs2;
    public McNemar(List<TemporalDocument> docsGold, List<TemporalDocument> docs1, List<TemporalDocument> docs2){
        this.docsGold = docsGold;
        this.docs1 = docs1;
        this.docs2 = docs2;
    }
    public void test() throws Exception{
        int n01 = 0;
        int n10 = 0;
        for(TemporalDocument docGold:docsGold) {
            TemporalDocument doc1 = new TemporalDocument();
            TemporalDocument doc2 = new TemporalDocument();
            for(TemporalDocument doc:docs1){
                if(doc.getDocID().equals(docGold.getDocID())) {
                    doc1 = doc;
                    break;
                }
            }
            for(TemporalDocument doc:docs2){
                if(doc.getDocID().equals(docGold.getDocID())) {
                    doc2 = doc;
                    break;
                }
            }
            doc1.saturateTlinks(Integer.MAX_VALUE,false,true,false);
            doc2.saturateTlinks(Integer.MAX_VALUE,false,true,false);
            Pair<Integer, Integer> mismatch = TempDocEval.McNemarTest(docGold,doc1,doc2);
            n01+=mismatch.getFirst();
            n10+=mismatch.getSecond();
        }
        System.out.println("McNemar statistic="+1.0*(Math.abs(n01-n10)-1)*(Math.abs(n01-n10)-1)/(n01+n10));
    }
    public static void CoDL_CAVEO_TDTest() throws Exception{
        List<TemporalDocument> gold = TempEval3Reader.deserialize("serialized_data/Chambers/raw-TD-Test");
        List<TemporalDocument> codl = TempEval3Reader.deserialize("serialized_data/Chambers/CoDL/TD-Test/lambda0.3_kl1.4");
        List<TemporalDocument> caveo = TempEval3Reader.deserialize("serialized_data/Chambers/raw-TD-Test");// load gold first
        HashMap<String,List<TLINK>> CAVEO_output = sieve_output.get_CAVEO_output();
        for(TemporalDocument doc:caveo){
            doc.setBodyTlinks(CAVEO_output.get(doc.getDocID()+".tml"));
        }
        McNemar exp = new McNemar(gold,codl,caveo);
        exp.test();
    }
    public static void CoDL_ClearTK_TBVC() throws Exception{
        List<TemporalDocument> gold = TempEval3Reader.deserialize("serialized_data/te3-platinum");
        List<TemporalDocument> codl = TempEval3Reader.deserialize("serialized_data/aug_Bethard/CoDL/Platinum/lambda0.6_kl1.4");
        List<TemporalDocument> cleartk = TempEval3Reader.deserialize("serialized_data/ClearTK_Output");// load gold first
        McNemar exp = new McNemar(gold,codl,cleartk);
        exp.test();
    }
    public static void SL_ClearTK_TBVC() throws Exception{
        List<TemporalDocument> gold = TempEval3Reader.deserialize("serialized_data/te3-platinum");
        List<TemporalDocument> codl = TempEval3Reader.deserialize("serialized_data/aug_Bethard/SL/TBVC_SL");
        List<TemporalDocument> cleartk = TempEval3Reader.deserialize("serialized_data/ClearTK_Output");// load gold first
        McNemar exp = new McNemar(gold,codl,cleartk);
        exp.test();
    }

    public static void main(String[] args) throws Exception{
        /*List<TemporalDocument> platinum = TempEval3Reader.deserialize(TempEval3Reader.label2dir("platinum"));
        List<TemporalDocument> local_known = TempEval3Reader.deserialize("serialized_data/McNemar-known/local");
        List<TemporalDocument> global_known = TempEval3Reader.deserialize("serialized_data/McNemar-known/global");
        List<TemporalDocument> sl_known = TempEval3Reader.deserialize("serialized_data/McNemar-known/sl");//et from cleartk
        McNemar exp1 = new McNemar(platinum,local_known,global_known);
        exp1.test();
        McNemar exp2 = new McNemar(platinum,global_known, sl_known);
        exp2.test();*/

        //CoDL_CAVEO_TDTest();
        //CoDL_ClearTK_TBVC();
        SL_ClearTK_TBVC();
    }
}
