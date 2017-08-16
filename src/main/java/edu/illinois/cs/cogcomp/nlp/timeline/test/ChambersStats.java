package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.nlp.CompareCAVEO.TBDense_split;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import net.didion.jwnl.data.Exc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 4/4/17.
 */
public class ChambersStats {
    public static void main(String[] args) throws Exception{
        List<TemporalDocument> chambers = TempEval3Reader.deserialize(TempEval3Reader.label2dir("chambers_only"));
        List<TemporalDocument> testset = new ArrayList<>(), devset = new ArrayList<>(), trainset = new ArrayList<>();
        for(TemporalDocument doc:chambers){
            switch(TBDense_split.findDoc(doc.getDocID())){
                case 1:
                    trainset.add(doc);
                    break;
                case 2:
                    devset.add(doc);
                    break;
                case 3:
                    testset.add(doc);
                    break;
                default:
            }
        }
        // row: train, dev, test, total
        // col: b a i ii s v total
        int[][] stats = new int[4][7];
        int[][] fine_stats = new int[chambers.size()][7];
        int[][] split_states = new int[chambers.size()][3];//ee, tt, et
        for(TemporalDocument doc:chambers){
            int idx = TBDense_split.findDoc(doc.getDocID())-1;
            List<TLINK> tlinks = doc.getBodyTlinks();
            for(TLINK tl:tlinks){
                if(tl.getSourceType().equals(tl.getTargetType())){
                    if(tl.getSourceType().equals(TempEval3Reader.Type_Event))
                        split_states[chambers.indexOf(doc)][0]++;
                    else
                        split_states[chambers.indexOf(doc)][1]++;
                }
                else
                    split_states[chambers.indexOf(doc)][2]++;
                if(tl.getReducedRelType()== TLINK.TlinkType.BEFORE) {
                    stats[idx][0]++;
                    fine_stats[chambers.indexOf(doc)][0]++;
                }
                else if(tl.getReducedRelType()== TLINK.TlinkType.AFTER) {
                    stats[idx][1]++;
                    fine_stats[chambers.indexOf(doc)][1]++;
                }
                else if(tl.getReducedRelType()== TLINK.TlinkType.INCLUDES) {
                    stats[idx][2]++;
                    fine_stats[chambers.indexOf(doc)][2]++;
                }
                else if(tl.getReducedRelType()== TLINK.TlinkType.IS_INCLUDED) {
                    stats[idx][3]++;
                    fine_stats[chambers.indexOf(doc)][3]++;
                }
                else if(tl.getReducedRelType()== TLINK.TlinkType.EQUAL) {
                    stats[idx][4]++;
                    fine_stats[chambers.indexOf(doc)][4]++;
                }
                else if(tl.getReducedRelType()== TLINK.TlinkType.UNDEF) {
                    stats[idx][5]++;
                    fine_stats[chambers.indexOf(doc)][5]++;
                }
            }
        }
        for(int i=0;i<6;i++) {
            stats[0][6]+=stats[0][i];
            stats[1][6]+=stats[1][i];
            stats[2][6]+=stats[2][i];
            stats[3][i] = stats[0][i]+stats[1][i]+stats[2][i];
            for(int j=0;j<chambers.size();j++){
                fine_stats[j][6]+=fine_stats[j][i];
            }
        }
        stats[3][6] = stats[0][6]+stats[1][6]+stats[2][6];
        for(int i=0;i<4;i++){
            for(int j=0;j<7;j++){
                System.out.printf("%5d\t",stats[i][j]);
            }
            System.out.println("");
        }
        for(int i=0;i<chambers.size();i++){
            System.out.printf("%20s\t",chambers.get(i).getDocID());
            for(int j=0;j<7;j++){
                System.out.printf("%5d\t",fine_stats[i][j]);
            }
            System.out.printf("%5d\t%5d\t%5d\t",split_states[i][0],split_states[i][1],split_states[i][2]);
            System.out.println("");
        }
    }

}
