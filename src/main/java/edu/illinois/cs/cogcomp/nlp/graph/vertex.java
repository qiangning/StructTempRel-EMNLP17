package edu.illinois.cs.cogcomp.nlp.graph;

/**
 * Created by qning2 on 11/19/16.
 */
public class vertex {
    private int id;
    private EntityType type;
    private String text;

    public boolean equals(vertex v2){
        return id==v2.getId()&&type==v2.getType();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    public vertex(int id, EntityType type) {
        this.id = id;
        this.type = type;
    }

    public vertex(int id, EntityType type, String text) {
        this.id = id;
        this.type = type;
        this.text = text;
    }

    public enum EntityType {EVENT, TIMEX}
    
    public String toString(){
        String s = "";
        if(type.equals(EntityType.EVENT)){
            s = "e"+id;
        }
        else if(type.equals(EntityType.TIMEX)){
            s = "t"+id;
        }
        if(text!=null)
            s = s+":"+text;
        return s;
    }
}
