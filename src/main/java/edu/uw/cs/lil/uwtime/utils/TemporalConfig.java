package edu.uw.cs.lil.uwtime.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ho.yaml.Yaml;

import edu.uw.cs.utils.composites.Pair;

public class TemporalConfig {
	private final static String INDENTATION = "\t";
	private final static String DEFAULT_CONFIG = "/resources/configs/default.yml";
	private static TemporalConfig instance;
	
	static {
		try {
			instance = new TemporalConfig();
		} catch (FileNotFoundException e) {
			throw new RuntimeException();
		}
	}
	
	public final String name;
	public final List<Pair<TemporalDomain, String>> train;
	public final List<Pair<TemporalDomain, String>> test;
	public final int cvFolds;

	public final String datasetDir;
	public final String outputDir;
	public final String modelDir;
	public final String correctionsDir;
	public final String correctionsFile;

	public final boolean goldMentions;
	public final boolean useContext;
	public final boolean useDCTAsData;


	public enum TemporalDomain {TIMEML, WIKIWARS};
	
	public static void set(String configFile) throws FileNotFoundException {
		instance = new TemporalConfig(configFile);
	}
	
	private TemporalConfig() throws FileNotFoundException {
		this(null);
	}

	@SuppressWarnings("unchecked")
	private TemporalConfig(String configFile) throws FileNotFoundException {
		Map<String, Object> config = (Map<String, Object>) Yaml.load(this.getClass().getResourceAsStream(DEFAULT_CONFIG));
		if (configFile != null) {
			File f = new File(configFile);
			config.putAll((Map<String, Object>) Yaml.load(f));
			name = f.getName().replace(".yml", "");
		}
		else {
			name = "default";
		}
		train = new LinkedList<Pair<TemporalDomain, String>>();
		for (Map<String, String> m : (List<Map<String, String>>) config.get("train"))
			train.add(Pair.of(TemporalDomain.valueOf(m.get("corpus_type")), m.get("dir")));

		test = new LinkedList<Pair<TemporalDomain, String>>();
		for (Map<String, String> m : (List<Map<String, String>>) config.get("test"))
			test.add(Pair.of(TemporalDomain.valueOf(m.get("corpus_type")), m.get("dir")));

		cvFolds = (Integer) config.get("cv_folds");

		datasetDir = (String) config.get("dataset_dir");
		outputDir = (String) config.get("output_dir");
		modelDir = (String) config.get("model_dir");
		correctionsDir = (String) config.get("corrections_dir");
		correctionsFile = (String) config.get("corrections_file");

		useContext = (Boolean) config.get("use_context");
		goldMentions = (Boolean) config.get("gold_mentions");
		useDCTAsData = (Boolean) config.get("use_dct_as_data");
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(FormattingUtils.formatContents(INDENTATION, "name", name));
		sb.append(FormattingUtils.formatContents(INDENTATION, "train", train));
		sb.append(FormattingUtils.formatContents(INDENTATION, "test", test));
		sb.append(FormattingUtils.formatContents(INDENTATION, "cv_folds", cvFolds));
		sb.append(FormattingUtils.formatContents(INDENTATION, "use_context", useContext));
		sb.append(FormattingUtils.formatContents(INDENTATION, "gold_mentions", goldMentions));
		sb.append(FormattingUtils.formatContents(INDENTATION, "use_dct_as_data", useDCTAsData));
		sb.append(FormattingUtils.formatContents(INDENTATION, "dataset_dir", datasetDir));
		sb.append(FormattingUtils.formatContents(INDENTATION, "output_dir", outputDir));
		sb.append(FormattingUtils.formatContents(INDENTATION, "model_dir", modelDir));
		sb.append(FormattingUtils.formatContents(INDENTATION, "corrections_file", correctionsFile));
		return sb.toString();
	}

	public boolean useCrossValidation() {
		return cvFolds > 1; 
	}

	public boolean useDCTAsData() {
		return useDCTAsData;
	}
	
	public static TemporalConfig getInstance() {
		return instance;
	}
}
