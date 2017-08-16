************** NewsReader MEANTIME corpus **************

By downloading or using these data, you accept the terms and conditions of the Creative Commons Attribution 4.0 International (CC BY 4.0) license agreement (http://creativecommons.org/licenses/by/4.0/).

© 2015, Fondazione Bruno Kessler, VU University Amsterdam, Universidad Del Pais Vasco

Contacts:
Whole corpus/English section: manspera@fbk.eu, minard@fbk.eu
Italian section: manspera@fbk.eu
Spanish section: ruben.urizar@ehu.eus
Dutch section: marieke.van.erp@vu.nl

Website: www.newsreader-project.eu/results/data/wikinews/

===============================

In this folder you will find:
- 2 folders containing each 120 articles annotated either only at the document level (intra-doc_annotation/) or both at the document and corpus level (intra_cross-doc_annotation/). The 120 articles are divided in 4 subfolders. The articles are in XML format.
- an XML schema
- a list of event and entity instances (see below for more details)
- the license
- this readme file

The corpus is distributed with a CC-BY license (http://creativecommons.org/licenses/by/4.0/).


=== Description of the corpus ===
The NewsReader  MEANTIME (Multilingual Event ANd TIME) corpus is a semantically annotated corpus of 480 English, Italian, Spanish, and Dutch news articles. It was created within the NewsReader project (http://www.newsreader-project.eu/), whose goal is to build a multilingual system for reconstructing storylines across news articles to provide policy and decision makers with an overview of what happened, to whom, when, and where. 
Semantic annotations in the MEANTIME corpus span multiple levels, including entities, events, temporal information, semantic roles, and intra-document and cross-document event and entity coreference.

The English section of the corpus comes from Wikinews (Wikinews is a collection of multilingual online news articles written collaboratively in a wiki-like manner - http://en.wikinews.org). The Spanish, Italian and Dutch sections are translations of the English articles aligned at the sentence level, enabling a comparison of natural language processing tools across the languages.


=== Guidelines ===
- Sara Tonelli, Rachele Sprugnoli, Manuela Speranza and Anne-Lyse Minard (2014) NewsReader Guidelines for Annotation at Document Level. NWR-2014-2-2. Version FINAL (Aug 2014). Fondazione Bruno Kessler. http://www.newsreader-project.eu/files/2014/12/NWR-2014-2-2.pdf
- Manuela Speranza and Anne-Lyse Minard (2014). Cross-Document Annotation Guidelines. NWR-2014-9. Fondazione Bruno Kessler. http://www.newsreader-project.eu/files/2015/01/NWR-2014-9.pdf
- Anneleen Schoen, Chantal van Son, Marieke van Erp and Hennie van der Vliet (2014). NewsReader Document-Level Annotation Guidelines - Dutch. NWR-2014-08. VU University Amsterdam. http://www.newsreader-project.eu/files/2013/01/8-AnnotationGuidelinesDutch.pdf
- Manuela Speranza, Rubén Urizar and Anne-Lyse Minard (2014). NewsReader Italian and Spanish specific Guidelines for Annotation at Document Level. NWR-2014-6. DRAFT version. Fondazione Bruno Kessler. http://www.newsreader-project.eu/files/2015/02/NWR-2014-61.pdf


=== Annotation tools ===
The files have been annotated at the document level using CAT (http://dh.fbk.eu/resources/cat-content-annotation-tool) and at the corpus level using CROMER (http://hlt-nlp.fbk.eu/technologies/cromer).


=== List of event and entity instances ===
The instance list file contains all event and entity instances annotated at the corpus level using the CROMER tool. 
The list is the same for the 4 languages. 
The file contains one line by instance and each instance is described by the following attributes: 
ID: identifier code used in the annotated files (attribute "instance_id")
TYPE: EVENT or ENTITY
CLASS: entity type (product, organization, person, location, financial) or event class (grammatical, speech-cognitive, other)
INSTANCE_NAME: a free text name given by annotators
DESCR: a description of the instance, free text
EXT_LINK: DBpedia uri for entity when available and the PropBank sense for a selection of events
COMMENT: free text comments provided by annotators
TIME: time anchor for events
BEGINTERVAL and ENDINTERVAL: begin and end for durative events
N.ANN_DOC.USER and N.ANN_DOC.ALL: not relevant information
HAS_PARTS: id of entities that are participant of an event and their roles


=== Multilingual corpus ===
MEANTIME corpus is composed by articles in 4 languages. The annotation at the corpus level has been done using the same set of instances in the 4 languages, i.e. event and entity instances are shared.
In the XML files, EVENT and ENTITY elements are discribed by an attribute "instance_id" which has as value the identifier code given in the list of event and entity instances previously described. 


=== Italian, Spanish and Dutch ===
The information in the XML element Document (doc_id and url) refer to the original English article. The doc_id is the same in the 4 languages, it enables to find the equivalent articles in different languages. 
