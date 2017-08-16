package edu.uw.cs.lil.uwtime.data.readers;
/*Also load event mentions and TLINKS. Nov 2016@@Qiang Ning.*/
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

import edu.illinois.cs.cogcomp.nlp.corpusreaders.TempEval3Reader;
import edu.uw.cs.lil.uwtime.parsing.typeshifting.IntegerAdditionTypeShifting;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import edu.uw.cs.lil.uwtime.data.TemporalDataset;
import edu.uw.cs.lil.uwtime.data.TemporalDocument;
import edu.uw.cs.lil.uwtime.utils.TemporalLog;


public class TimeMLReader extends AbstractTemporalReader {
	private String currentText;
	private String currentTimex;
	private String currentEvent;
	private String currentTlink;
	private String currentMakeInstance;
	private String currentDocID;
	private String currentDCT;
	private boolean isReadingText;
	private boolean isReadingTimex;
	private boolean isReadingEvent;
	private boolean isReadingTlink;
	private boolean isReadingMakeinstance;
	private boolean isReadingDocID;
	private boolean isReadingDCT;
	private TemporalDocument currentDocument;

	@Override
	public TemporalDataset getDataset(String datasetRoot, String datasetName) throws IOException, SAXException, ParserConfigurationException  {
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
		
		File xmlDir = new File(datasetRoot + datasetName);
		File[] xmlFiles = xmlDir.listFiles();
		TemporalLog.printf("progress","Reading %d files from %s\n", xmlFiles.length, datasetName);
		int count = 0;
		TemporalDataset dataset = new TemporalDataset(datasetName);
		for(File f: xmlFiles) {
			if (!f.getName().endsWith(".tml")) {
				TemporalLog.println("error", "Found " + f.getName() + ", which does not belong here!");
				continue;
			}
			count ++;
			TemporalLog.printf("progress","[%d/%d] Parsing %s\n", count, xmlFiles.length, f.getName());
			System.out.printf("[%d/%d] Parsing %s\n",count, xmlFiles.length, f.getName());
			currentDocument = new TemporalDocument();
			sp.parse(xmlDir.getPath() + "/" + f.getName(), this);
			currentDocument.doPreprocessing(pipeline, gsf);
			currentDocument.deleteSpacesInEvents();
			dataset.addDocument(currentDocument);
		}

		long endTime = System.nanoTime();
		dataset.serialize(serializedFile.getPath());
		TemporalLog.printf("progress","%d sentences parsed and serialized (%.2f seconds)\n", dataset.size(), (endTime - startTime)*1.0e-9);
		return dataset;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals("TIMEX3")) {
			String value = attributes.getValue("value");
			String type = attributes.getValue("type");
			String mod = attributes.getValue("mod");
			int tid = Integer.parseInt(attributes.getValue("tid").replaceFirst("t", ""));

			if (isReadingText)
				currentDocument.insertMention(type, value, mod, currentText.length(), tid);
			if (isReadingDCT)
				currentDocument.insertDCTMention(type, value, mod, currentDCT.length(), tid);
			isReadingTimex = true;
			currentTimex = "";
		}
		else if(qName.equals("EVENT")){
			String eventclass = attributes.getValue("class");
			String tense = attributes.getValue("tense");
			String aspect = attributes.getValue("aspect");
			int eid = Integer.parseInt(attributes.getValue("eid").replaceFirst("e",""));
			if(isReadingText){
				currentDocument.insertEventMention(eventclass,tense,aspect,currentText.length(),eid);
			}
			isReadingEvent = true;
			currentEvent = "";
		}
		else if(qName.equals("TLINK")){
			int lid = Integer.parseInt(attributes.getValue("lid").replaceFirst("l",""));
			String relType = attributes.getValue("relType");
			//int timeID = attributes.getValue("timeID")==null?-1:Integer.parseInt(attributes.getValue("timeID").replaceFirst("t",""));
			//int eventInstanceID = attributes.getValue("eventInstanceID")==null?-1:Integer.parseInt(attributes.getValue("eventInstanceID").replaceFirst("ei",""));
			String timeID = attributes.getValue("timeID");
			String eventInstanceID = attributes.getValue("eventInstanceID");
			String relatedToTime = attributes.getValue("relatedToTime");
			String relatedToEventInstance = attributes.getValue("relatedToEventInstance");
			String sourceType="", targetType="";
			int sourceId=-1, targetId=-1;
			if(timeID!=null) {
				sourceType = TempEval3Reader.Type_Timex;
				sourceId = Integer.parseInt(timeID.replaceFirst("t",""));
			}
			if(eventInstanceID!=null){
				sourceType = TempEval3Reader.Type_Event;
				sourceId = Integer.parseInt(eventInstanceID.replaceFirst("ei",""));
			}
			if(relatedToTime!=null){
				targetType = TempEval3Reader.Type_Timex;
				targetId = Integer.parseInt(relatedToTime.replaceFirst("t",""));
			}
			if(relatedToEventInstance!=null){
				targetType = TempEval3Reader.Type_Event;
				targetId = Integer.parseInt(relatedToEventInstance.replaceFirst("ei",""));
			}
			currentDocument.insertTlink(lid,relType,sourceType,targetType,sourceId,targetId);
			isReadingTlink = true;
			currentTlink = "";
		}
		else if(qName.equals("MAKEINSTANCE")){
			String tense = attributes.getValue("tense");
			String aspect = attributes.getValue("aspect");
			String polarity = attributes.getValue("polarity");
			String pos = attributes.getValue("pos");
			int eid = Integer.parseInt(attributes.getValue("eventID").replaceFirst("e",""));
			int eiid = Integer.parseInt(attributes.getValue("eiid").replaceFirst("ei",""));
			String cardinality = attributes.getValue("cardinality");
			if(!currentDocument.makeInstance(eid, eiid, tense, aspect, polarity, pos,cardinality)){
				System.out.println("Invalid MAKEINSTANCE in "+currentDocument.getDocID()+": eid="+eid+" cannot be found.");
			}
			isReadingMakeinstance = true;
			currentMakeInstance = "";
		}
		else if (qName.equals("TEXT")) {
			isReadingText = true;
			currentText = "";
		}
		else if (qName.equals("DCT")) {
			isReadingDCT = true;
			currentDCT = "";
		}
		else if (qName.equals("DOCID")){
			isReadingDocID = true;
			currentDocID = "";
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if (qName.equals("TIMEX3")) {
			if (isReadingTimex) {
				if (isReadingText)
					currentDocument.setLastMentionText(currentTimex);
				if (isReadingDCT)
					currentDocument.setDCTMentionText(currentTimex);
			}
			isReadingTimex = false;
		}
		else if(qName.equals("EVENT")){
			if(isReadingEvent){
				if(isReadingText){
					//set last mention text
					currentDocument.setLastMentionText(currentEvent, TempEval3Reader.Type_Event);
				}
			}
			isReadingEvent = false;
		}
		else if(qName.equals("TLINK")){
			isReadingTlink = false;
		}
		else if(qName.equals("MAKEINSTANCE")){
			isReadingMakeinstance = false;
		}
		else if (qName.equals("TEXT") ) {
			currentDocument.setText(currentText);
			isReadingText = false;
		}
		else if (qName.equals("DCT") ) {
			currentDocument.setDCTText(currentDCT);
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
		if (isReadingTimex)
			currentTimex += text;
		if (isReadingEvent)
			currentEvent += text;
		if (isReadingTlink)
			currentTlink += text;
		if (isReadingDocID)
			currentDocID += text;
		if (isReadingDCT) 
			currentDCT += text;
	}
}
