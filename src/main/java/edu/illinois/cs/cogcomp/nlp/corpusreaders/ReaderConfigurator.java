package edu.illinois.cs.cogcomp.nlp.corpusreaders;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Property;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
/**
 * Created by qning2 on 12/2/16.
 */
public class ReaderConfigurator extends Configurator{
    /*directories*/
    public static final Property SERIAL_DIR = new Property("ser_dir", "./serialized_data");
    public static final Property SERIAL_DIR2 = new Property("ser_dir2", "./serialized_data/NoBethardChambers");
    public static final Property SERIAL_DIR3 = new Property("ser_dir3", "./serialized_data/Chambers/raw");

    public static final Property TIMEBANK_DIR = new Property("timebank_dir", "./data/TempEval3/Training/TBAQ-cleaned/");
    public static final Property AQUAINT_DIR = new Property("aquaint_dir", "./data/TempEval3/Training/TBAQ-cleaned/");
    public static final Property PLATINUM_DIR = new Property("platinum_dir", "./data/TempEval3/Evaluation/");
    public static final Property SILVER0_DIR = new Property("silver0_dir", "./data/TempEval3/Training/");
    public static final Property SILVER1_DIR = new Property("silver1_dir", "./data/TempEval3/Training/");
    public static final Property SILVER2_DIR = new Property("silver2_dir", "./data/TempEval3/Training/");
    public static final Property SILVER3_DIR = new Property("silver3_dir", "./data/TempEval3/Training/");
    public static final Property SILVER4_DIR = new Property("silver4_dir", "./data/TempEval3/Training/");
    public static final Property SILVER5_DIR = new Property("silver5_dir", "./data/TempEval3/Training/");
    /*constants*/
    public static final Property TIMEBANK_LABEL = new Property("timebank_label", "TimeBank");
    public static final Property AQUAINT_LABEL = new Property("aquaint_label", "AQUAINT");
    public static final Property PLATINUM_LABEL = new Property("platinum_label", "te3-platinum");
    public static final Property SILVER0_LABEL = new Property("silver0_label", "TE3-Silver-data-0");
    public static final Property SILVER1_LABEL = new Property("silver1_label", "TE3-Silver-data-1");
    public static final Property SILVER2_LABEL = new Property("silver2_label", "TE3-Silver-data-2");
    public static final Property SILVER3_LABEL = new Property("silver3_label", "TE3-Silver-data-3");
    public static final Property SILVER4_LABEL = new Property("silver4_label", "TE3-Silver-data-4");
    public static final Property SILVER5_LABEL = new Property("silver5_label", "TE3-Silver-data-5");

    @Override
    public ResourceManager getDefaultConfig() {
        Property[] props =
                {SERIAL_DIR, SERIAL_DIR2, SERIAL_DIR3, TIMEBANK_DIR, AQUAINT_DIR, PLATINUM_DIR, SILVER0_DIR,SILVER1_DIR,SILVER2_DIR,SILVER3_DIR,SILVER4_DIR,SILVER5_DIR,
                        TIMEBANK_LABEL, AQUAINT_LABEL, PLATINUM_LABEL, SILVER0_LABEL, SILVER1_LABEL,SILVER2_LABEL,SILVER3_LABEL,SILVER4_LABEL,SILVER5_LABEL};
        return new ResourceManager(generateProperties(props));
    }
}
