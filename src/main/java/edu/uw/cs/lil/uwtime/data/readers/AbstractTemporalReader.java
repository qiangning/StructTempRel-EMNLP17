package edu.uw.cs.lil.uwtime.data.readers;

import java.io.IOException;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.SemanticHeadFinder;
import edu.stanford.nlp.util.Filters;
import edu.uw.cs.lil.uwtime.data.TemporalDataset;


public abstract class AbstractTemporalReader extends DefaultHandler {
	final protected static String SERIALIZED_DIR = "serialized_data/";
	protected StanfordCoreNLP pipeline;
	protected GrammaticalStructureFactory gsf;
	protected SAXParser sp;

	public AbstractTemporalReader(){
		// Don't initialize anything until we know we need it.
	}

	protected void initLibraries() throws ParserConfigurationException, SAXException {
		// Initialize annotation pipeline for preprocessing
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, parse");
		pipeline = new StanfordCoreNLP(props);
		gsf = new PennTreebankLanguagePack().grammaticalStructureFactory(Filters.<String>acceptFilter(), new SemanticHeadFinder(false));

		// Initialize xml parser
		SAXParserFactory spfac = SAXParserFactory.newInstance();
		sp = spfac.newSAXParser();
	}

	abstract public TemporalDataset getDataset(String datasetRoot, String datasetName) throws IOException, SAXException, ParserConfigurationException;
}
