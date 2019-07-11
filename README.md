# Human Behaviour Change Project Information Extraction (hbcpIE)
version 2.0.0


### Update: 
demo currently down due to security issue.
The system can be run locally following the instructions below.


17/04/2019

Copyright (c): Apache License Version 2.0

Created by HBCP team, IBM Research, Dublin


* Code contributions: Debasis Ganguly, Martin Gleize, Yufang Hou, Charles Jochim, Francesca Bonin

Java version: "1.8.0"

The project represents the first version of the code of the HBCP project.
It contains two main packages and information extraction pipeline ( extractor) and a prediction pipeline (predictor). Both have APIs that can be tested via a Swagger UI.


## Extractor
The package executes the following action:
1. Indexes a collection of documents
2. Store them in a Lucene index
3. Extracts pieces of information from arbitrary passages.


To cite the work please cite:

Debasis Ganguly, L�a A. Deleris, Pol Mac Aonghusa, Alison J. Wright, Ailbhe N. Finnerty, Emma Norris, Marta M. Marques, Susan Michie:
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


### Description
The project takes as input pdfs (scientific articles of behaviour change interventions) and returns several attributes encoded by the Behaviour Change Intervention Ontology developed by UCL in the context of the Human Behaviour Change Project (HBCP) (https://www.humanbehaviourchange.org/). A rest API with a Swagger UI is provided. This allows everybody to test the system from a web browser: [http://23.97.177.82:8180/swagger-ui.html](http://23.97.177.82:8180/swagger-ui.html)
 
Supported entities for the moment:

Population characteristics:  min age, max age, gender and mean age.

Behavioural Change Techniques: Goal Setting (Behaviour), Problem Solving, Action Planning, Feedback on behaviour, Self-monitoring of behaviour, Social support (unspecified), Information about health consequences, Information about social and environmental consequences, Pharmacological support and Reduce negative emotions.

Outcome : outcome value

## Predictor
The package executes the following actions:
1. Takes in input entities and the documents from which they have been extracted
2. Build a relation graph where entities are nodes and co-occurrence in the same document are edges
3. Create an embedded space via Node2Vec
4. Allow to query the space in order to find a entity given 1 or more other entities.

### Description
The project takes as input pdfs (scientific articles of behaviour change interventions) and a json of either extracted either manually annotated entities. It allow the user to write a query, and retrieves the most relevant outcome value for that particualre query. A rest API with a Swagger UI is provided. This allows everybody to test the system from a web browser: [http://23.97.177.82:8180/swagger-ui.html](http://23.97.177.82:8180/swagger-ui.html)


### Important Note:
Please consider that this is a work in progress and we are currently working on improving/expanding the project.
Feel free to download the code and use it, as well as to send us feedback and bug report.


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
### Information Extraction

- Visit this page to demo the system http://23.97.177.82:8180/swagger-ui.html
- Click on extractor-controller
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


### Prediction
Visit this page to demo the system http://23.97.177.82:8180/swagger-ui.html

Click on predictor-controller

You can either:
- get one attribute given an ID
- get the entire set of attributes ( interventions or gender, or settings etc.)
- predict an outcome given a population query and a intervention query expressed as follows:

    - For population: C:<attributeID>:value, eg. C:4507435:18 for Mean age=18
    - For Interventions: I:<attributeID>:value, eg. I:3673271:1 for Goal settings.

Click "Try it out!"
Results will be shown in this format:

## Quickstart for coders


### For information extraction
From command line type the following command to index the collection.
```
mvn exec:java@indexer
```
The next step is to execute the following command to run the IE pipeline.
```
mvn exec:java@extractor
```

### For predictions
```
mvn exec:java@predict
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
- version 2.0

## LICENSE
This program is free software; you can redistribute it and/or
 modify it under the terms of the Apache License Version 2.0.

## Contact

For help or issues using the HBCP code, please submit a GitHub issue.
For personal communication related to the project, please contact the HBCP team (humanbehaviourchange@ucl.ac.uk) 
