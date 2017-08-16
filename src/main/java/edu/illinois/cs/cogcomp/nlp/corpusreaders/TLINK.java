package edu.illinois.cs.cogcomp.nlp.corpusreaders;
import edu.illinois.cs.cogcomp.nlp.graph.vertex;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 11/15/16.
 */
public class TLINK implements java.io.Serializable{
    //[BEFORE, AFTER, INCLUDES, IS_INCLUDED, DURING, DURING_INV, SIMULTANEOUS, IAFTER, IBEFORE, IDENTITY, BEGINS, ENDS, BEGUN_BY, ENDED_BY, OVERLAP, BEFORE-OR-OVERLAP, OVERLAP-OR-AFTER, VAGUE, NONE]
    public enum TlinkType {
        BEFORE ("","before"),
        AFTER ("after","after"),
        EQUAL ("=","equal"),
        INCLUDES ("icd","includes"),
        IS_INCLUDED ("icd'd","included"),
        UNDEF ("undef");
        private final String name;
        private final String fullname;
        TlinkType(String s) {
            name = s;
            fullname = s;
        }
        TlinkType(String name,String fullname){
            this.name = name;
            this.fullname = fullname;
        }
        public String toString() {
            return this.name;
        }
        public String toStringfull(){return this.fullname;}
        public TlinkType reverse(){
            switch(this){
                case BEFORE:
                    return AFTER;
                case AFTER:
                    return BEFORE;
                case EQUAL:
                    return EQUAL;
                case INCLUDES:
                    return IS_INCLUDED;
                case IS_INCLUDED:
                    return INCLUDES;
                case UNDEF:
                    return UNDEF;
                default:
                    System.out.println("Undefined TlinkType.");
                    System.exit(-1);
            }
            return UNDEF;
        }
        public static TlinkType reverse(String fullname){
            return str2TlinkType(fullname).reverse();
        }
        public static TlinkType str2TlinkType(String fullname){
            switch(fullname){
                case "before":
                    return BEFORE;
                case "after":
                    return AFTER;
                case "equal":
                    return EQUAL;
                case "includes":
                    return INCLUDES;
                case "included":
                    return IS_INCLUDED;
                case "undef":
                case "":
                    return UNDEF;
                default:
                    System.out.println("Undefined TlinkType.");
                    System.exit(-1);
            }
            return UNDEF;
        }
    }
    private static final long serialVersionUID = 5225315703669795655L;
    private int lid;
    private String relType;
    private String sourceType;
    private String targetType;
    private int sourceId;
    private int targetId;
    private TlinkType reducedRelType;
    public static String[] ignore_tlink = new String[]{TlinkType.UNDEF.toStringfull()};

    public TLINK(int lid, String relType, String sourceType, String targetType, int sourceId, int targetId) {
        this.lid = lid;
        this.relType = relType;
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.reducedRelType = getReducedRelType(this.relType);
    }
    public TLINK(int lid, String relType, String sourceType, String targetType, int sourceId, int targetId, TlinkType reducedRel) {
        this.lid = lid;
        this.relType = relType;
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.reducedRelType = reducedRel;
    }
    public TLINK deepCopy(){
        return new TLINK(lid,relType,sourceType,targetType,sourceId,targetId,reducedRelType);
    }
    public TLINK converse(){
        return new TLINK(lid, invRelType(relType), targetType, sourceType, targetId, sourceId,reducedRelType.reverse());
    }

    public vertex getSourceVertex(){
        vertex v = null;
        if(sourceType.equals(TempEval3Reader.Type_Event))
            v =  new vertex(sourceId,vertex.EntityType.EVENT);
        else if(sourceType.equals(TempEval3Reader.Type_Timex))
            v = new vertex(sourceId,vertex.EntityType.TIMEX);
        return v;
    }

    public vertex getTargetVertex(){
        vertex v = null;
        if(targetType.equals(TempEval3Reader.Type_Event))
            v =  new vertex(targetId,vertex.EntityType.EVENT);
        else if(targetType.equals(TempEval3Reader.Type_Timex))
            v = new vertex(targetId,vertex.EntityType.TIMEX);
        return v;
    }
    public int getLid() {
        return lid;
    }

    public String getRelType() {
        return relType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getTargetType() {
        return targetType;
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setLid(int lid) {
        this.lid = lid;
    }

    public void setRelType(String relType) {
        this.relType = relType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    @Override
    public String toString() {
        return "TLINK{" +
                "lid=" + lid +
                ", sourceType='" + sourceType + '\'' +
                ", targetType='" + targetType + '\'' +
                ", sourceId=" + sourceId +
                ", targetId=" + targetId +
                ", relType='" + reducedRelType + '\'' +
                '}';
    }

    public String toStringConcise(){
        return "("+sourceType.substring(0,1)+sourceId+","+targetType.substring(0,1)+targetId+")="
                +reducedRelType.toStringfull();
    }


    public boolean equals(TLINK tlink){
        boolean eq = this.sourceType.equals(tlink.getSourceType())
                && this.targetType.equals(tlink.getTargetType())
                && this.sourceId==tlink.getSourceId()
                && this.targetId==tlink.getTargetId();
        if(eq&&!this.getReducedRelType().equals(tlink.getReducedRelType())) {
            TemporalLog.println("SaturationProgress","Unexpected mismatch: "+this.toStringConcise()+" vs "+tlink.toStringConcise());
            //System.out.println("Unexpected mismatch: "+this.toStringConcise()+" vs "+tlink.toStringConcise());
        }

        return eq;
    }
    public TlinkType getReducedRelType(){
        return reducedRelType;
    }
    public TlinkType getReducedRelType(String relType){
        switch(relType.toLowerCase()){
            case "before":
                return TlinkType.BEFORE;
            case "after":
                return TlinkType.AFTER;
            case "identity":
                return TlinkType.EQUAL;
            case "simultaneous":
                return TlinkType.EQUAL;
            case "includes":
                return TlinkType.INCLUDES;
            case "is_included":
                return TlinkType.IS_INCLUDED;
            case "ended_by":
                return TlinkType.INCLUDES;
            case "ends":
                return TlinkType.IS_INCLUDED;
            case "during":
                return TlinkType.IS_INCLUDED;
            case "begun_by":
                return TlinkType.INCLUDES;
            case "begins":
                return TlinkType.IS_INCLUDED;
            case "continues":
                return TlinkType.AFTER;
            case "initiates":
                return TlinkType.IS_INCLUDED;
            case "terminates":
                return TlinkType.IS_INCLUDED;
            case "ibefore":
                return TlinkType.BEFORE;
            case "iafter":
                return TlinkType.AFTER;
            default:
                return TlinkType.UNDEF;
        }
    }
    public String invRelType(String relType){
        switch(relType.toLowerCase()) {
            case "before":
                return "AFTER";
            case "after":
                return "BEFORE";
            case "identity":
                return "IDENTITY";
            case "simultaneous":
                return "SIMULTANEOUS";
            case "includes":
                return "IS_INCLUDED";
            case "is_included":
                return "INCLUDES";
            case "ended_by":
                return "ENDS";
            case "ends":
                return "ENDED_BY";
            case "during":
                return "INCLUDES";
            case "begun_by":
                return "BEGINS";
            case "begins":
                return "BEGUN_BY";
            case "continues":
                return "BEFORE";
            case "initiates":
                return "BEGUN_BY";
            case "terminates":
                return "ENDED_BY";
            case "ibefore":
                return "IAFTER";
            case "iafter":
                return "IBEFORE";
            default:
                return "INV_" + relType;
        }
    }

    public static List<TLINK> removeDuplicates(List<TLINK> tlinks){
        List<TLINK> reducedTlinks = new ArrayList<>();
        for(int i=0;i<tlinks.size();i++){
            /*Self-identity (redundant)*/
            if(tlinks.get(i).getSourceType().equals(tlinks.get(i).getTargetType())&&
                    tlinks.get(i).getSourceId()==tlinks.get(i).getTargetId())
                continue;
            /*Duplicates exist*/
            boolean found = false;
            for(int j=i+1;j<tlinks.size();j++){
                if(tlinks.get(i).equals(tlinks.get(j))) {
                    found = true;
                    break;
                }
            }
            if(!found)
                reducedTlinks.add(tlinks.get(i));
        }
        return reducedTlinks;
    }

    public String toXmlAnnotation(){return toXmlAnnotation(lid);}
    public String toXmlAnnotation(int lid) {
        String source = sourceType.equals(TempEval3Reader.Type_Event)? "eventInstanceID" : "timeID";
        String target = targetType.equals(TempEval3Reader.Type_Event)? "relatedToEventInstance" : "relatedToTime";
        String relType2str = "";
        switch(reducedRelType.toStringfull()){
            case "before":
                relType2str = "BEFORE";
                break;
            case "after":
                relType2str = "AFTER";
                break;
            case "includes":
                relType2str = "INCLUDES";
                break;
            case "included":
                relType2str = "IS_INCLUDED";
                break;
            case "equal":
                relType2str = "SIMULTANEOUS";
                break;
            case "undef":
                relType2str = "NONE";
                break;
            default:
                System.exit(-1);
        }
        return String.format("<TLINK %s %s %s %s />\n",
                source + "=\"" + (sourceType.equals(TempEval3Reader.Type_Event)?"ei":"t") + sourceId + "\"",
                "lid=\"l" + lid + "\"",
                "relType=\"" + relType2str + "\"",
                target + "=\"" + (targetType.equals(TempEval3Reader.Type_Event)?"ei":"t") + targetId + "\"");
    }
}
