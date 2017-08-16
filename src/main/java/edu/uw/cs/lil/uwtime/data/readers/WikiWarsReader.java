package edu.uw.cs.lil.uwtime.data.readers;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.data.TemporalDataset;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;


public class WikiWarsReader extends AbstractTemporalReader {
	private String currentText;
	private String currentDCTText;
	private String currentDocID;
	private Stack<TemporalJointChunk> chunkStack;
	private Stack<TemporalJointChunk> dctStack;
	private Stack<StringBuilder> mentionTextStack;

	private boolean isReadingText;
	private boolean isReadingDCT;
	private boolean isReadingDocID;
	private TemporalDocument currentDocument;
	private int mentionCount;

	public WikiWarsReader(){
		this.chunkStack = new Stack<TemporalJointChunk>();
		this.dctStack = new Stack<TemporalJointChunk>();
		this.mentionTextStack = new Stack<StringBuilder>();
	}

	@Override
	public TemporalDataset getDataset(String datasetRoot, String datasetName) throws ParserConfigurationException, SAXException, IOException {
		new File(SERIALIZED_DIR).mkdirs();
		File serializedFile = new File(SERIALIZED_DIR + datasetName + ".ser");
		TemporalLog.println("progress", "Looking for serialized data at " + serializedFile);
		if(serializedFile.exists()) {
			TemporalLog.println("progress","Serialized data found. Deserializing data...");
			try {
				return TemporalDataset.deserialize(serializedFile.getPath());
			}
			catch (ClassNotFoundException e) {
			}
		}

		if (!serializedFile.exists())
			TemporalLog.println("progress", "Serialized data unavailable.");
		else
			TemporalLog.println("progress", "Serialized data invalid.");


		initLibraries();

		TemporalLog.println("progress","Reading and dependency parsing data...");

		long startTime = System.nanoTime();

		TemporalDataset dataset = new TemporalDataset(datasetName);
		File xmlDir = new File(datasetRoot + datasetName);
		File[] xmlFiles = xmlDir.listFiles();
		TemporalLog.printf("progress","Reading %d files from %s\n", xmlFiles.length, datasetName);
		int count = 0;
		for(File f: xmlFiles) {
			if (!f.getName().endsWith(".xml")) {
				TemporalLog.println("error", "Found " + f.getName() + ", which does not belong here!");
				continue;
			}
			count ++;
			TemporalLog.printf("progress","[%d/%d] Parsing %s\n", count, xmlFiles.length, f.getName());
			currentDocument = new TemporalDocument();
			mentionCount = 1;
			sp.parse(xmlDir.getPath() + "/" + f.getName(), this);
			currentDocument.doPreprocessing(pipeline, gsf);
			dataset.addDocument(currentDocument);
		}

		long endTime = System.nanoTime();
		dataset.serialize(serializedFile.getPath());
		TemporalLog.printf("progress","%d sentences parsed and serialized (%.2f seconds)\n", dataset.size(), (endTime - startTime)*1.0e-9);
		return dataset;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals("TIMEX2")) {
			String value = attributes.getValue("val");
			String mod = attributes.getValue("mod");
			if (isReadingText){
				chunkStack.add(new TemporalJointChunk(null, value, mod, currentText.length(), -1, mentionCount));
				mentionCount++;
			}
			if (isReadingDCT) {
				dctStack.add(new TemporalJointChunk(null, value, mod, currentDCTText.length(), -1, 0));
			}
			mentionTextStack.add(new StringBuilder());
		}
		else if (qName.equals("TEXT")) {
			isReadingText = true;
			currentText = "";
		}
		else if (qName.equals("DATETIME")) {
			isReadingDCT = true;
			currentDCTText = "";
		}
		else if (qName.equals("DOCID")){
			isReadingDocID = true;
			currentDocID = "";
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equals("TIMEX2")) {
			if (isReadingText) {
				TemporalJointChunk topChunk = chunkStack.pop();
				if (topChunk.getResult().getValue() != null) {
					topChunk.setCharEnd(mentionTextStack.pop().toString());
					currentDocument.insertMention(topChunk);
				}
			}
			if (isReadingDCT){
				TemporalJointChunk topDCT = dctStack.pop();
				if (topDCT.getResult().getValue() != null) {
					topDCT.setCharEnd(mentionTextStack.pop().toString());
					currentDocument.insertDCTMention(topDCT);
				}
			}
		}
		else if (qName.equals("TEXT") ) {
			currentDocument.setText(currentText);
			isReadingText = false;
		}
		else if (qName.equals("DATETIME") ) {
			currentDocument.setDCTText(currentDCTText);
			isReadingDCT = false;
		}
		else if (qName.equals("DOCID")){
			currentDocument.setDocID(currentDocID);
			isReadingDocID = false;
		}
	}

	@Override
	public void characters(char[] buffer, int start, int length) {
		String text = new String(buffer, start, length);
		if (isReadingText)
			currentText += text;
		if (isReadingDCT)
			currentDCTText += text;
		for (StringBuilder sb : mentionTextStack) {
			sb.append(text);
		}
		if (isReadingDocID)
			currentDocID += text;
	}
}
