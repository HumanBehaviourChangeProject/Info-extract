# Human Behaviour Change Project Information Extraction (hbcpIE)  version 0.1

06/11/2018

Copyright (c): Apache License Version 2.0

Created by HBCP team, IBM Research, Dublin

* Main author: Debasis Ganguly
* Code contributions: Francesca Bonin, Yufang Hou, Lea Deleris

Java version: "1.8.0"

The project executes the following action:
1. Indexes a collection of documents
2. Store them in a Lucene index
3. Extracts pieces of information from arbitrary passages.


To cite the work please cite:

Debasis Ganguly, Léa A. Deleris, Pol Mac Aonghusa, Alison J. Wright, Ailbhe N. Finnerty, Emma Norris, Marta M. Marques, Susan Michie:
Unsupervised Information Extraction from Behaviour Change Literature. MIE 2018: 680-684
```@inproceedings{DBLP:conf/mie/GangulyAAWFNMM18,
  author    = {Debasis Ganguly and
               L{\'{e}}a A. Deleris and
               Pol Mac Aonghusa and
               Alison J. Wright and
               Ailbhe N. Finnerty and
               Emma Norris and
               Marta M. Marques and
               Susan Michie},
  title     = {Unsupervised Information Extraction from Behaviour Change Literature},
  booktitle = {Building Continents of Knowledge in Oceans of Data: The Future of
               Co-Created eHealth - Proceedings of {MIE} 2018, Medical Informatics
               Europe, Gothenburg, Sweden, April 24-26, 2018},
  pages     = {680--684},
  year      = {2018}
}
```
[bibtex](https://dblp.uni-trier.de/rec/bibtex/conf/mie/GangulyAAWFNMM18)

### Note:
Please consider that this is a work in progress and we are currently working on improving/expanding the project.
Feel free to download the code and use it, as well as to send us feedback and bug report.


## Description


The project takes as input pdfs (scientific articles of behaviour change interventions) and returns several attributes encoded by the Behaviour Change Intervention Ontology developed by UCL in the context of the Human Behaviour Change Project (HBCP) (https://www.humanbehaviourchange.org/). 
A rest API with a Swagger UI is provided. This allows everybody to test the system from a web browser: http://23.97.177.82:8180/swagger-ui.html


Supported entities for the moment:


Population characteristics: min age, max age, gender and mean age.


Behaviour Change Techniques: Goal Setting (Behaviour), Problem Solving, Action Planning, Feedback on behaviour, Self-monitoring of behaviour, Social support (unspecified), Information about health consequences, Information about social and environmental consequences, Pharmacological support and Reduce negative emotions.


## Requirements
Most recent version of Maven: https://maven.apache.org/download.cgi

## What has been released in this repository

We are releasing:
- code for supervised, semisupervised and unsupersived retrieval of a selection of BCTs
- Swagger api facilities
- Java documentation for each class
- 17 fully annotated open access papers

## Dataset

In the context of the HBCP (https://www.humanbehaviourchange.org/), 244 papers of behaviour science intervention papers have been annotated for 10 behaviour change techniques and 4 population characteristics (min age, max age, mean age and gender) according to the Behaviour Change Intervention Ontology. Our pre-trained model is trained on a subset of 111 papers. We release 17 papers (that the model has not been trained on) as a sample dataset. Those 17 papers are open access and publicly available.


## Quickstart for behavioural science users
- Visit this page to demo the system http://23.97.177.82:8180/swagger-ui.html
- Click on doc-extract-info
- Choose the entity you want to extract from your pdf:
     - all BCT present in the paper (allbcts)
     - all BCT present in the paper using a supervised method (allbcts/supervised)
     - detect the presence of the bct specified in the parameter "code" (bct)
     - detect the presence of the bct specified in the parameter "code" using a supervised method (bct/supervised)
     - the gender of participants (gender)
     - the minimum age of participants (minage)
     - the max of participants (maxage)
     - the mean of participants (meanage)
- Upload the pdf with the : "choose file" button
- Click "Try it out!" 

Results will be shown in this format:

```
code": "Minage",
  "docName": "Volpp 2009 primary paper.pdf",
  "extractedValue": "18"
}
```

I.E. Looking for gender in pdf Volpp 2009 primary paper.pdf, the extracted value is 18.



## Quickstart for coders


### Running the project
From command line type the following command to index the collection.
```
mvn exec:java@indexer
```
The next step is to execute the following command to run the IE pipeline.
```
mvn exec:java@extractor
```

### Test REST API with Swagger UI
The project uses spring boot and spring fox with swagger ui. 

From the command line, locate yourself in the project HOME (hbcpIE/) and execute
```
mvn spring-boot:run
```
Open a browser and hit this [URL] (http://localhost:8180/swagger-ui.html)


## Java Documentation
[Javadoc](apidocs/index.html)


## THANKS
Thanks to the UCL annotators that developed the Behaviour Change Intervention Ontology.

## CHANGES
- version 0.1 

## LICENSE
This program is free software; you can redistribute it and/or
 modify it under the terms of the Apache License Version 2.0.

## Contact

For help or issues using the HBCP code, please submit a GitHub issue.
For personal communication related to the project, please contact the HBCP team (humanbehaviourchange@ucl.ac.uk) 
