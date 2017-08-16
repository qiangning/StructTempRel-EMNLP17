package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.pipeline.common.PipelineConfigurator;
import edu.illinois.cs.cogcomp.pipeline.main.PipelineFactory;


/**
 * Created by qning2 on 11/29/16.
 */
public class testpipeline {
    public static TextAnnotation getTA(String id, String text) throws Exception{
        //ResourceManager rm = (new PipelineConfigurator()).getDefaultConfig();
        ResourceManager rm = new PipelineConfigurator().getConfig(new ResourceManager( "config/pipeline-config.properties" ));
        AnnotatorService prep = PipelineFactory.buildPipeline(rm);
        TextAnnotation rec = prep.createAnnotatedTextAnnotation(id, "", text);
        return rec;
    }
    public static void main(String[] args_) throws Exception {
        String text = "Houston, Monday, July 21 -- Men have landed and walked on the moon.";
        TextAnnotation rec1 = testpipeline.getTA("1",text);
        text = "Here's another sentence to process.";
        TextAnnotation rec2 = testpipeline.getTA("2",text);

        View lab = rec1.getView(ViewNames.POS);
    }
}
