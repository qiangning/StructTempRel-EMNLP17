package edu.uw.cs.lil.uwtime.main;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import edu.uw.cs.lil.tiny.mr.lambda.FlexibleTypeComparator;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.language.type.TypeRepository;
import edu.uw.cs.lil.uwtime.data.readers.AbstractTemporalReader;
import edu.uw.cs.lil.uwtime.data.readers.TimeMLReader;
import edu.uw.cs.lil.uwtime.data.readers.WikiWarsReader;
import edu.uw.cs.lil.uwtime.corrections.AnnotationCorrections;
import edu.uw.cs.lil.uwtime.data.TemporalDataset;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import edu.uw.cs.lil.uwtime.pipeline.ExperimentPipeline;
import edu.uw.cs.lil.uwtime.stats.TemporalStatistics;
import edu.uw.cs.lil.uwtime.utils.FileUtils;
import edu.uw.cs.lil.uwtime.utils.TemporalConfig;
import edu.uw.cs.lil.uwtime.utils.TemporalConfig.TemporalDomain;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;
import edu.uw.cs.utils.composites.Pair;

public class TemporalExperiment {
	public TemporalExperiment(String configFile) throws IOException {
		TemporalConfig.set(configFile);
		initLogs();
		initServices();
		TemporalLog.printf("progress", "Using config file (%s):\n%s\n", configFile, TemporalConfig.getInstance().toString());
	}

	private void initLogs() {
		TemporalLog.setLogs(TemporalConfig.getInstance().name);
		TemporalLog.addAlias("progress", System.out);
		TemporalLog.addAlias("error", System.out);
		TemporalLog.addAlias("stats", System.out);
		TemporalLog.suppressLabel("debug");
	}

	public void initServices() throws IOException {
		LogicLanguageServices.setInstance(
				new LogicLanguageServices.Builder(
						new TypeRepository(FileUtils.streamToFile(TemporalExperiment.class.getResourceAsStream("/resources/lexicon/temporal.types"), "types")), 
						new FlexibleTypeComparator())
				.setNumeralTypeName("n")
				.build());
	}

	private ExperimentPipeline evaluate(
			TemporalDataset trainingDataset, 
			TemporalDataset testingDataset, 
			TemporalStatistics stats, 
			int fold) {
		ExperimentPipeline thread = new ExperimentPipeline(trainingDataset, testingDataset, stats, fold);
		if (TemporalConfig.getInstance().useCrossValidation())
			thread.start();
		else {
			long startTime = System.currentTimeMillis();
			thread.run();
			long endTime   = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			System.out.println(totalTime);
		}
		return thread;
	}

	private void crossValidate(TemporalDataset dataset, TemporalStatistics stats) throws InterruptedException {
		List<List<TemporalDocument>> partitions = dataset.partition(TemporalConfig.getInstance().cvFolds);
		List<ExperimentPipeline> threads = new LinkedList<ExperimentPipeline>();
		for (int i = 0; i < partitions.size(); i++) {
			TemporalDataset trainingDataset = new TemporalDataset(dataset.getName());
			TemporalDataset testingDataset = new TemporalDataset(dataset.getName(), partitions.get(i));
			for (int j = 0; j < partitions.size(); j++)
				if (j != i)
					for (TemporalDocument doc : partitions.get(j))
						trainingDataset.addDocument(doc);
			threads.add(evaluate(trainingDataset, testingDataset, stats, i));
		}
		for (ExperimentPipeline t : threads)
			t.join();
	}

	public void run() throws IOException, SAXException, ParserConfigurationException, InterruptedException {
		AnnotationCorrections corrections = new AnnotationCorrections(TemporalConfig.getInstance().correctionsFile);

		TemporalDataset testingDataset = new TemporalDataset(TemporalConfig.getInstance().name);
		for (Pair<TemporalDomain, String> p: TemporalConfig.getInstance().test) {
			AbstractTemporalReader reader;
			switch(p.first()) {
			case TIMEML:
				reader = new TimeMLReader();
				break;
			case WIKIWARS:
				reader = new WikiWarsReader();
				break;
			default:
				reader = new TimeMLReader();
				break;
			}
			for (TemporalDocument document : reader.getDataset(TemporalConfig.getInstance().datasetDir, p.second()).getDocuments()) {
				testingDataset.addDocument(document);
			}
		}
		testingDataset.sort();
		if (!TemporalConfig.getInstance().useDCTAsData())
			testingDataset = testingDataset.withoutDocumentCreationTimes();
		corrections.applyCorrections(testingDataset);
		
		testingDataset.dump(TemporalConfig.getInstance().correctionsDir);

		TemporalLog.printf ("progress","Evaluating %d sentences...\n\n", testingDataset.size());

		long startTime = System.nanoTime();

		TemporalStatistics stats = new TemporalStatistics();

		if (TemporalConfig.getInstance().useCrossValidation()) {
			crossValidate(testingDataset, stats);
		}
		else {
			TemporalDataset trainingDataset = new TemporalDataset(TemporalConfig.getInstance().name);
			for (Pair<TemporalDomain, String> p: TemporalConfig.getInstance().train) {
				AbstractTemporalReader reader;
				switch(p.first()) {
				case TIMEML:
					reader = new TimeMLReader();
					break;
				case WIKIWARS:
					reader = new WikiWarsReader();
					break;
				default:
					reader = new TimeMLReader();
					break;
				}
				for (TemporalDocument document : reader.getDataset(TemporalConfig.getInstance().datasetDir, p.second()).getDocuments()) {
					trainingDataset.addDocument(document);
				}
			}
			trainingDataset.sort();
			if (!TemporalConfig.getInstance().useDCTAsData())
				trainingDataset = trainingDataset.withoutDocumentCreationTimes();
			corrections.applyCorrections(trainingDataset);
			evaluate(trainingDataset, testingDataset, stats, 0);
		}

		TemporalLog.println("stats", stats.formatStats());

		TemporalLog.printf("progress", "Done with analysis. (%.2f seconds)\n", (System.nanoTime() - startTime)*1.0e-9);
	}

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, InterruptedException {
		args = new String[1];
		args[0] = "configs/wikiwars/wikiwars_test.yml";
		if (args.length < 1) {
			System.out.println("Config file not found");
			System.exit(0);
		}
		TemporalExperiment exp = new TemporalExperiment(args[0]);
		exp.run();
	}
}
