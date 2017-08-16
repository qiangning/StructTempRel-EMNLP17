package edu.illinois.cs.cogcomp.nlp.classifier.lbj.perceptron;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Property;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
/**
 * Created by qning2 on 12/2/16.
 */
public class ClassifierConfigurator extends Configurator{
    public static final Property EE_MODEL_NAME = new Property("eeModelName", "ee_perceptron");
    public static final Property EE_MODEL_NAME_NONE = new Property("eeModelName_none", "ee_perceptron_none");
    public static final Property ET_MODEL_NAME = new Property("etModelName", "et_perceptron");
    public static final Property ET_MODEL_NAME_NONE = new Property("etModelName_none", "et_perceptron_none");
    public static final Property EE_MODEL_DIR_PATH = new Property("eeModelDirPath",
            "src/main/java/edu/illinois/cs/cogcomp/nlp/classifier/lbj/perceptron/ee/");
    public static final Property ET_MODEL_DIR_PATH = new Property("etModelDirPath",
            "src/main/java/edu/illinois/cs/cogcomp/nlp/classifier/lbj/perceptron/et/");
    public static final Property EE_MODEL_PATH = new Property("eeModelPath", EE_MODEL_DIR_PATH.value
            + EE_MODEL_NAME.value + ".lc");
    public static final Property ET_MODEL_PATH = new Property("etModelPath", ET_MODEL_DIR_PATH.value
            + ET_MODEL_NAME.value + ".lc");
    public static final Property EE_MODEL_LEX_PATH = new Property("eeModelLexPath", EE_MODEL_DIR_PATH.value
            + EE_MODEL_NAME.value + ".lex");
    public static final Property ET_MODEL_LEX_PATH = new Property("etModelLexPath", ET_MODEL_DIR_PATH.value
            + ET_MODEL_NAME.value + ".lex");
    @Override
    public ResourceManager getDefaultConfig() {
        Property[] props =
                {EE_MODEL_NAME, EE_MODEL_NAME_NONE, EE_MODEL_DIR_PATH, EE_MODEL_PATH, EE_MODEL_LEX_PATH,
                        ET_MODEL_NAME, ET_MODEL_NAME_NONE, ET_MODEL_DIR_PATH, ET_MODEL_PATH, ET_MODEL_LEX_PATH};
        return new ResourceManager(generateProperties(props));
    }
}
