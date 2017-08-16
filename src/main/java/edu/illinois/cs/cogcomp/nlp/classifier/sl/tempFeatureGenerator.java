package edu.illinois.cs.cogcomp.nlp.classifier.sl;

import edu.illinois.cs.cogcomp.nlp.classifier.FeatureExtractor;
import edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron.ParamLBJ;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.FeatureVectorBuffer;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 12/27/16.
 */
public class tempFeatureGenerator extends AbstractFeatureGenerator implements
        Serializable {
    private static final long serialVersionUID = -2818476256571520766L;
    private Lexiconer lm;
    public int none_edge = Integer.MAX_VALUE;
    public int ignore_edge = 5;
    public int include_edge = 2;
    public boolean sentId_or_not = false;
    public boolean usePrior = false;

    public tempFeatureGenerator(Lexiconer lm){
        this.lm = lm;
        usePrior = false;
    }
    public tempFeatureGenerator(Lexiconer lm, boolean usePrior){
        this.lm = lm;
        this.usePrior = usePrior;
    }

    public Lexiconer getLm() {
        return lm;
    }

    public void setEdges(int none_edge,int ignore_edge, int include_edge){
        this.none_edge = none_edge;
        this.ignore_edge = ignore_edge;
        this.include_edge = include_edge;
    }

    @Override
    public IFeatureVector getFeatureVector(IInstance x, IStructure y){
        temporalInstance docInst = (temporalInstance) x;
        temporalStructure docStruct = (temporalStructure) y;
        return extractFeatures(docInst,docStruct);
    }

    private IFeatureVector extractFeatures(temporalInstance docInst, temporalStructure docStruct){
        /*This function is not actually used.*/
        FeatureVectorBuffer fb = new FeatureVectorBuffer();
        int nE = docStruct.nE;
        for(int i=0;i<nE;i++){
            int sentId1 = docInst.doc.getSentId(docInst.events[i]);
            for(int j=i+1;j<nE;j++){
                int sentId2 = docInst.doc.getSentId(docInst.events[j]);
                int dist = sentId_or_not?(Math.abs(sentId1-sentId2)) : (j-i);
                if(!usePrior && dist>=ignore_edge)
                    continue;
                IFeatureVector fv = getEdgeFeatures(i,j,docInst,docStruct);
                if(fv==null)
                    continue;
                fb.addFeature(fv);
            }
        }
        return fb.toFeatureVector();
    }
    public IFeatureVector getEdgeFeatures(int i, int j,
                                          temporalInstance docInst, temporalStructure docStruct){
        List<String> featureMap = new ArrayList<>();
        String[][] rels = docStruct.getRelStr();

        EventChunk ec1 = docInst.events[i];
        EventChunk ec2 = docInst.events[j];
        TLINK tlink = docInst.doc.getTlink(ec1,ec2);
        if(tlink==null||tlink.getReducedRelType()== TLINK.TlinkType.UNDEF)
            return null;
        /*if(rels[i][j].equals("undef")){
            if(Math.random()>0.05)
                return null;
        }*/

        /*rel_i_j&feat<event_i,event_j>*/
        addEventPairFeats(i,j,docInst,rels[i][j],featureMap);
        /*rel_(i-1)_i&rel_i_(i+1)&rel_(j-1)_j&rel_j_(j+1)&rel_i_j*/
        addRelChainFeats(i,j,docInst,rels,featureMap);
        /*rel_i_j*/
        featureMap.add("EventIJ:"+rels[i][j]);

        /*FeatureVectorBuffer fb = new FeatureVectorBuffer();
        for (String f : featureMap) {
            if (lm.isAllowNewFeatures())
                lm.addFeature(f);
            if (lm.containFeature(f))
                fb.addFeature(lm.getFeatureId(f), 1.0f);
            else
                fb.addFeature(lm.getFeatureId("W:unknownword"), 1.0f);
        }
        return fb.toFeatureVector();*/
        return getFeatureFromMap(featureMap);
    }
    public IFeatureVector getFeatureFromMap(List<String> featureMap){
        FeatureVectorBuffer fb = new FeatureVectorBuffer();
        for (String f : featureMap) {
            if (lm.isAllowNewFeatures())
                lm.addFeature(f);
            if (lm.containFeature(f))
                fb.addFeature(lm.getFeatureId(f), 1.0f);
            else
                fb.addFeature(lm.getFeatureId("W:unknownword"), 1.0f);
        }
        return fb.toFeatureVector();
    }
    public void addEventPairFeats(int i,int j,temporalInstance docInst, String rel,
                                  List<String> featureMap){
        if(j<=i){
            System.out.println("j must be larger than i.");
            System.exit(-1);
        }

        FeatureExtractor featExtractor = new FeatureExtractor(docInst.doc);
        String feat = featExtractor.getFeatureString(featExtractor.extractEEfeats(docInst.events[i],docInst.events[j]),"");//setting label to be "" is easier
        String[] feat_sep = feat.split(ParamLBJ.FEAT_DELIMITER);
        for(String tmp:feat_sep)
            featureMap.add("EventIJ:"+rel+"&"+tmp);
    }
    public void addRelChainFeats(int i,int j,temporalInstance docInst, String[][] rels,
                                 List<String> featureMap){
        if(j<=i){
            System.out.println("j must be larger than i.");
            System.exit(-1);
        }
        int nE = docInst.nE;
        String tmp1 = "EventI_prev:"+(i>0?rels[i-1][i]: TLINK.TlinkType.UNDEF.toStringfull());
        String tmp2 = "EventI_next:"+(i<nE-1?rels[i][i+1]: TLINK.TlinkType.UNDEF.toStringfull());
        String tmp3 = "EventJ_prev:"+(j>0?rels[j-1][j]: TLINK.TlinkType.UNDEF.toStringfull());
        String tmp4 = "EventJ_next:"+(j<nE-1?rels[j][j+1]: TLINK.TlinkType.UNDEF.toStringfull());
        String tmp5 = "EventIJ:"+rels[i][j];
        featureMap.add(tmp1+"&"+tmp2+"&"+tmp3+"&"+tmp4+"&"+tmp5);
    }
    public static void main(String[] args) throws Exception{
        TempEval3Reader myReader;
        myReader = new TempEval3Reader("TIMEML","te3-platinum","data/TempEval3/Evaluation/");
        myReader.ReadData();
        myReader.createTextAnnotation();
        myReader.saturateTlinks();
        Lexiconer lm = new Lexiconer();
        lm.setAllowNewFeatures(true);
        for(TemporalDocument doc:myReader.getDataset().getDocuments()){
            temporalInstance docInst = new temporalInstance(doc);
            temporalStructure docStruct = new temporalStructure(docInst);

            tempFeatureGenerator featGen = new tempFeatureGenerator(lm);
            featGen.getFeatureVector(docInst,docStruct);
        }
        lm.setAllowNewFeatures(false);
        TemporalDocument doc = myReader.getDataset().getDocuments().get(2);
        temporalInstance docInst = new temporalInstance(doc);
        temporalStructure docStruct = new temporalStructure(docInst);

        tempFeatureGenerator featGen = new tempFeatureGenerator(lm);
        IFeatureVector tmp = featGen.getEdgeFeatures(0,1,docInst,docStruct);
        System.out.println(tmp.toString().replace(" ","\n"));
    }
}