package edu.illinois.cs.cogcomp.nlp.graph;

import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by qning2 on 11/19/16.
 */
public class GraphJavaScript {
    private String fname;
    private final int width = 4000;
    private final int height = 2000;
    private final int linkDistance = 200;
    private List<vertex> V;
    private List<edge> E;

    public GraphJavaScript(String fname){
        this.fname = fname;
        V = new LinkedList<vertex>();
        E = new LinkedList<edge>();
    }

    public void addVertex(int id, vertex.EntityType type,String text){
        V.add(new vertex(id,type,text));
    }
    public void addVertex(EventChunk ec){
        addVertex(ec.getEiid(),vertex.EntityType.EVENT,ec.getText());
    }
    public void addVertex(TemporalJointChunk timex){
        addVertex(timex.getTID(),vertex.EntityType.TIMEX,timex.getPhrase().getString());
    }
    public void addEdge(vertex source, vertex target, String label){
        E.add(new edge(source,target,label));
    }
    public void addEdge(TLINK tlink){
        addEdge(tlink.getSourceVertex(),tlink.getTargetVertex(), tlink.getReducedRelType().toString());
    }
    public void addEdge(TemporalJointChunk timex1, TemporalJointChunk timex2, TemporalJointChunk dct){
        if(timex1.equals(timex2))
            return;
        TLINK.TlinkType comp = timex1.compareResult(timex2.getResult(),dct.getResult());
        if(comp.equals(TLINK.TlinkType.UNDEF))
            return;
        String relType = "";
        switch(comp){
            case BEFORE:
                relType = "BEFORE";
                break;
            case AFTER:
                relType = "AFTER";
                break;
            case EQUAL:
                relType = "SIMULTANEOUS";
                break;
            case INCLUDES:
                relType = "INCLUDES";
                break;
            case IS_INCLUDED:
                relType = "IS_INCLUDED";
                break;
            default:
                System.out.println("TlinkType undefined.");
        }
        TLINK tlink = new TLINK(-1,relType, TempEval3Reader.Type_Timex,TempEval3Reader.Type_Timex,timex1.getTID(),timex2.getTID());
        if(tlink.getReducedRelType().equals(TLINK.TlinkType.AFTER)
                || tlink.getReducedRelType().equals(TLINK.TlinkType.INCLUDES))
            addEdge(tlink.converse());
        else if(tlink.getReducedRelType().equals(TLINK.TlinkType.BEFORE)
                || tlink.getReducedRelType().equals(TLINK.TlinkType.IS_INCLUDED)
                || tlink.getReducedRelType().equals(TLINK.TlinkType.EQUAL))
            addEdge(tlink);
        else
            System.out.println("TLINK type undefined: "+tlink.getRelType());
    }
    public void createJS(){
        if(V.size()==0){
            System.out.println(fname+": Graph has not been initialized.");
        }
        if(E.size()==0)
            System.out.println(fname+": Graph has no edges.");
        String js_vertex = "";
        String js_edge = "";
        for(vertex v:V){
            String tmp = "    {name: \"" +v.toString() + "\"" + ", type: \"" + v.getType() + "\"}, \n";
            js_vertex += tmp;
        }
        for(edge e:E){
            int source = getVertexIdx(e.getSource());
            int target = getVertexIdx(e.getTarget());
            String type = e.getLabel();
            String tmp = "    {source: "+source+", target: "+target+", label: \""+type+"\"},\n";
            js_edge += tmp;
        }
        String jscript = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "<meta charset=\"utf-8\">\n" +
                "<title>Force Layout with labels on edges</title>\n" +
                "<script src=\"http://d3js.org/d3.v3.min.js\" charset=\"utf-8\"></script>\n" +
                "<style type=\"text/css\">\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "<script type=\"text/javascript\">\n" +
                "\n" +
                "    var w = "+width+"\n" +
                "    var h = "+height+"\n" +
                "    var linkDistance="+linkDistance+";\n" +
                "\n" +
                "    var colors = d3.scale.category10();\n" +
                "\t\n" +
                "    var dataset = {\n" +
                "\n" +
                "    nodes: [\n" +
                js_vertex+
                "    ],\n" +
                "    edges: [\n" +
                js_edge+
                "    ]\n" +
                "    };\n" +
                "\n" +
                " \n" +
                "    var svg = d3.select(\"body\").append(\"svg\").attr({\"width\":w,\"height\":h});\n" +
                "\n" +
                "    var force = d3.layout.force()\n" +
                "        .nodes(dataset.nodes)\n" +
                "        .links(dataset.edges)\n" +
                "        .size([w,h])\n" +
                "        .linkDistance([linkDistance])\n" +
                "        .charge([-500])\n" +
                "        .theta(0.1)\n" +
                "        .gravity(0.05)\n" +
                "        .start();\n" +
                "\n" +
                " \n" +
                "\n" +
                "    var edges = svg.selectAll(\"line\")\n" +
                "      .data(dataset.edges)\n" +
                "      .enter()\n" +
                "      .append(\"line\")\n" +
                "      .attr(\"id\",function(d,i) {return 'edge'+i})\n" +
                "      .attr('marker-end','url(#arrowhead)')\n" +
                "      .style(\"stroke\",\"#ccc\")\n" +
                "      .style(\"pointer-events\", \"none\");\n" +
                "    \n" +
                "    var nodes = svg.selectAll(\"circle\")\n" +
                "      .data(dataset.nodes)\n" +
                "      .enter()\n" +
                "      .append(\"circle\")\n" +
                "      .attr({\"r\":20})\n" +
                "      .style(\"fill\",function(d,i){return d.type==\"EVENT\"?colors(0): colors(1);})\n" +
                "      .call(force.drag)\n" +
                "\n" +
                "\n" +
                "    var nodelabels = svg.selectAll(\".nodelabel\") \n" +
                "       .data(dataset.nodes)\n" +
                "       .enter()\n" +
                "       .append(\"text\")\n" +
                "       .attr({\"x\":function(d){return d.x;},\n" +
                "              \"y\":function(d){return d.y;},\n" +
                "              \"class\":\"nodelabel\",\n" +
                "              \"stroke\":\"black\"})\n" +
                "       .text(function(d){return d.name;});\n" +
                "\n" +
                "    var edgepaths = svg.selectAll(\".edgepath\")\n" +
                "        .data(dataset.edges)\n" +
                "        .enter()\n" +
                "        .append('path')\n" +
                "        .attr({'d': function(d) {return 'M '+d.source.x+' '+d.source.y+' L '+ d.target.x +' '+d.target.y},\n" +
                "               'class':'edgepath',\n" +
                "               'fill-opacity':0,\n" +
                "               'stroke-opacity':0,\n" +
                "               'fill':'blue',\n" +
                "               'stroke':'red',\n" +
                "               'id':function(d,i) {return 'edgepath'+i}})\n" +
                "        .style(\"pointer-events\", \"none\");\n" +
                "\n" +
                "    var edgelabels = svg.selectAll(\".edgelabel\")\n" +
                "        .data(dataset.edges)\n" +
                "        .enter()\n" +
                "        .append('text')\n" +
                "        .style(\"pointer-events\", \"none\")\n" +
                "        .attr({'class':'edgelabel',\n" +
                "               'id':function(d,i){return 'edgelabel'+i},\n" +
                "               'dx':linkDistance/2,\n" +
                "               'dy':0,\n" +
                "               'font-size':20,\n" +
                "               'fill':'#aaa'});\n" +
                "\n" +
                "    edgelabels.append('textPath')\n" +
                "        .attr('xlink:href',function(d,i) {return '#edgepath'+i})\n" +
                "        .style(\"pointer-events\", \"none\")\n" +
                "        .text(function(d){return d.label});\n" +
                "\n" +
                "\n" +
                "    svg.append('defs').append('marker')\n" +
                "        .attr({'id':'arrowhead',\n" +
                "               'viewBox':'-0 -5 10 10',\n" +
                "               'refX':25,\n" +
                "               'refY':0,\n" +
                "               //'markerUnits':'strokeWidth',\n" +
                "               'orient':'auto',\n" +
                "               'markerWidth':10,\n" +
                "               'markerHeight':10,\n" +
                "               'xoverflow':'visible'})\n" +
                "        .append('svg:path')\n" +
                "            .attr('d', 'M 0,-5 L 10 ,0 L 0,5')\n" +
                "            .attr('fill', '#ccc')\n" +
                "            .attr('stroke','#ccc');\n" +
                "     \n" +
                "\n" +
                "    force.on(\"tick\", function(){\n" +
                "\n" +
                "        edges.attr({\"x1\": function(d){return d.source.x;},\n" +
                "                    \"y1\": function(d){return d.source.y;},\n" +
                "                    \"x2\": function(d){return d.target.x;},\n" +
                "                    \"y2\": function(d){return d.target.y;}\n" +
                "        });\n" +
                "\n" +
                "        nodes.attr({\"cx\":function(d){return d.x;},\n" +
                "                    \"cy\":function(d){return d.y;}\n" +
                "        });\n" +
                "\n" +
                "        nodelabels.attr(\"x\", function(d) { return d.x; }) \n" +
                "                  .attr(\"y\", function(d) { return d.y; });\n" +
                "\n" +
                "        edgepaths.attr('d', function(d) { var path='M '+d.source.x+' '+d.source.y+' L '+ d.target.x +' '+d.target.y;\n" +
                "                                           //console.log(d)\n" +
                "                                           return path});       \n" +
                "\n" +
                "        edgelabels.attr('transform',function(d,i){\n" +
                "            if (d.target.x<d.source.x){\n" +
                "                bbox = this.getBBox();\n" +
                "                rx = bbox.x+bbox.width/2;\n" +
                "                ry = bbox.y+bbox.height/2;\n" +
                "                return 'rotate(180 '+rx+' '+ry+')';\n" +
                "                }\n" +
                "            else {\n" +
                "                return 'rotate(0)';\n" +
                "                }\n" +
                "        });\n" +
                "    });\n" +
                "\n" +
                "</script>\n" +
                "\n" +
                "</body>\n" +
                "</html>";
        try{
            File file = new File(fname);
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(jscript);
            fileWriter.flush();
            fileWriter.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public int getVertexIdx(vertex v){
        for(int i=0;i<V.size();i++){
            if(v.equals(V.get(i)))
                return i;
        }
        return -1;
    }
    public static void main(String[] args){
        GraphJavaScript gjs = new GraphJavaScript("test.html");
        gjs.createJS();
    }
}
