package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.illinois.cs.cogcomp.nlp.util.TransitivityTriplets;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 12/26/16.
 */
public class DocSanityCheck {
    /*sanityCheck_All() doesn't saturate by itself. It only checks existing links.*/
    public static int sanityCheck_All(TemporalDocument doc,boolean debug){
        int violation = 0;
        List<Object> mentions = new ArrayList<Object>();//place holder for both event and timex
        mentions.addAll(doc.getBodyEventMentions());
        mentions.addAll(doc.getBodyTimexMentions());
        /*Symmetry*/
        for(Object ec1:mentions){
            for(Object ec2:mentions){
                if(ec1==ec2)
                    continue;
                TLINK tlink1 = doc.getTlink(ec1,ec2);
                TLINK tlink2 = doc.getTlink(ec2,ec1);
                if(tlink1==null){
                    if(tlink2!=null) {
                        violation++;
                        if(debug) {
                            System.out.println(doc.getDocID() + ":" + doc.getObjectTypeId(ec1) + "," + doc.getObjectTypeId(ec2));
                            System.out.println("tlink1=null," + "tlink2=" + tlink2.toStringConcise());
                        }
                    }
                }
                else{
                    if(tlink2==null){
                        violation++;
                        if(debug) {
                            System.out.println(doc.getDocID() + ":" + doc.getObjectTypeId(ec1) + "," + doc.getObjectTypeId(ec2));
                            System.out.println("tlink1=" + tlink1.toStringConcise() + ",tlink2=null");
                        }
                    }
                    else if(tlink1.getReducedRelType().reverse()!=tlink2.getReducedRelType()){
                        violation++;
                        if(debug) {
                            System.out.println(doc.getDocID() + ":" + doc.getObjectTypeId(ec1) + "," + doc.getObjectTypeId(ec2));
                            System.out.println("tlink1=" + tlink1.toStringConcise() + ",tlink2=" + tlink2.toStringConcise());
                        }
                    }
                }
            }
        }
        /*Transitivity check*/
        for(Object ec1:mentions){
            for(Object ec2:mentions){
                if(ec1==ec2)
                    continue;
                for(Object ec3:mentions){
                    if(ec1==ec3||ec2==ec3)
                        continue;
                    List<TransitivityTriplets> transTriplets = TransitivityTriplets.transTriplets();
                    TLINK tlink12 = doc.getTlink(ec1,ec2);
                    TLINK tlink23 = doc.getTlink(ec2,ec3);
                    TLINK tlink13 = doc.getTlink(ec1,ec3);
                    TLINK.TlinkType tt12 = tlink12==null? TLINK.TlinkType.UNDEF:tlink12.getReducedRelType();
                    TLINK.TlinkType tt23 = tlink23==null? TLINK.TlinkType.UNDEF:tlink23.getReducedRelType();
                    TLINK.TlinkType tt13 = tlink13==null? TLINK.TlinkType.UNDEF:tlink13.getReducedRelType();
                    TransitivityTriplets tmp = new TransitivityTriplets(null,null,
                            new TLINK.TlinkType[]{TLINK.TlinkType.BEFORE,TLINK.TlinkType.AFTER,TLINK.TlinkType.INCLUDES,TLINK.TlinkType.IS_INCLUDED,TLINK.TlinkType.EQUAL,TLINK.TlinkType.UNDEF});
                    for(TransitivityTriplets triplet:transTriplets){
                        if(tt12==triplet.getFirst()&&tt23==triplet.getSecond()){
                            tmp=triplet;
                            break;
                        }
                    }
                    boolean found = false;
                    for(TLINK.TlinkType tt:tmp.getThird()){
                        if(tt==tt13){
                            found = true;
                            break;
                        }
                    }
                    if(!found){
                        violation++;
                        System.out.println(doc.getDocID()+":"+doc.getObjectTypeId(ec1)+","+doc.getObjectTypeId(ec2)+","+doc.getObjectTypeId(ec3));
                        System.out.println(tt12.toStringfull()+" "+tt23.toStringfull()+" "+tt13.toStringfull());
                    }
                }
            }
        }
        return violation;
    }
    public static boolean sanityCheck_EE(TemporalDocument doc){
        boolean valid = true;
        /*Symmetry check*/
        for(EventChunk ec1:doc.getBodyEventMentions()){
            for(EventChunk ec2:doc.getBodyEventMentions()){
                if(ec1==ec2)
                    continue;
                TLINK tlink1 = doc.getTlink(ec1,ec2);
                TLINK tlink2 = doc.getTlink(ec2,ec1);
                if(tlink1==null){
                    if(tlink2!=null) {
                        valid = false;
                        System.out.println(doc.getDocID()+":ec1="+ec1.getEiid()+",ec2="+ec2.getEiid());
                        System.out.println("tlink1=null,"+"tlink2="+tlink2.toStringConcise());
                    }
                }
                else{
                    if(tlink2==null){
                        valid = false;
                        System.out.println(doc.getDocID()+":ec1="+ec1.getEiid()+",ec2="+ec2.getEiid());
                        System.out.println("tlink1="+tlink1.toStringConcise()+",tlink2=null");
                    }
                    else if(tlink1.getReducedRelType().reverse()!=tlink2.getReducedRelType()){
                        valid = false;
                        System.out.println(doc.getDocID()+":ec1="+ec1.getEiid()+",ec2="+ec2.getEiid());
                        System.out.println("tlink1="+tlink1.toStringConcise()+",tlink2="+tlink2.toStringConcise());
                    }
                }
            }
        }
        /*Transitivity check*/
        for(EventChunk ec1:doc.getBodyEventMentions()){
            for(EventChunk ec2:doc.getBodyEventMentions()){
                if(ec1==ec2)
                    continue;
                for(EventChunk ec3:doc.getBodyEventMentions()){
                    if(ec1==ec3||ec2==ec3)
                        continue;
                    List<TransitivityTriplets> transTriplets = TransitivityTriplets.transTriplets();
                    TLINK tlink12 = doc.getTlink(ec1,ec2);
                    TLINK tlink23 = doc.getTlink(ec2,ec3);
                    TLINK tlink13 = doc.getTlink(ec1,ec3);
                    TLINK.TlinkType tt12 = tlink12==null? TLINK.TlinkType.UNDEF:tlink12.getReducedRelType();
                    TLINK.TlinkType tt23 = tlink23==null? TLINK.TlinkType.UNDEF:tlink23.getReducedRelType();
                    TLINK.TlinkType tt13 = tlink13==null? TLINK.TlinkType.UNDEF:tlink13.getReducedRelType();
                    TransitivityTriplets tmp = new TransitivityTriplets(null,null,
                            new TLINK.TlinkType[]{TLINK.TlinkType.BEFORE,TLINK.TlinkType.AFTER,TLINK.TlinkType.INCLUDES,TLINK.TlinkType.IS_INCLUDED,TLINK.TlinkType.EQUAL,TLINK.TlinkType.UNDEF});
                    for(TransitivityTriplets triplet:transTriplets){
                        if(tt12==triplet.getFirst()&&tt23==triplet.getSecond()){
                            tmp=triplet;
                            break;
                        }
                    }
                    boolean found = false;
                    for(TLINK.TlinkType tt:tmp.getThird()){
                        if(tt==tt13){
                            found = true;
                            break;
                        }
                    }
                    if(!found){
                        valid = false;
                        System.out.println(doc.getDocID()+":ec1="+ec1.getEiid()+",ec2="+ec2.getEiid()+",ec3="+ec3.getEiid());
                        System.out.println(tt12.toStringfull()+" "+tt23.toStringfull()+" "+tt13.toStringfull());
                    }
                }
            }
        }
        return valid;
    }
    public static void main(String[] args) throws Exception{
        TempEval3Reader myReader;
        myReader = new TempEval3Reader("TIMEML","te3-platinum","data/TempEval3/Evaluation/");
        myReader.ReadData();
        myReader.orderTimexes();
        myReader.saturateTlinks();
        for(TemporalDocument doc:myReader.getDataset().getDocuments()){
            //System.out.println(doc.getDocID()+" "+DocSanityCheck.sanityCheck_EE(doc));
            System.out.println(doc.getDocID()+" "+DocSanityCheck.sanityCheck_All(doc,true));
        }
    }
}
