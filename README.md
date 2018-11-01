# Human Behaviour Change Project Information Extraction (hbcpIE)  version 0.1

02/10/2018

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
 
[bibtex](https://dblp.uni-trier.de/rec/bibtex/conf/mie/GangulyAAWFNMM18)

### Note:
Please consider that this is a work in progress and we are currently working on improving/expanding the project.
Feel free to download the code and use it, as well as to send us feedback and bug report.


## Description
The project takes as input pdfs (scientific articles of behavioural change) and return several attributes encoded in the behavioural change ontology developed by UCL in the context of the HBCP project (https://www.humanbehaviourchange.org/).
A rest API with a Swagger UI is provided.

Examples of extracted attributes:

Features of the population of the study:
- minimun age 
- max age
- gender

Behavioral Change Intervention described in the study:
- goal settings
- problem solving
- ...

## Requirements
Most recent version of Maven: https://maven.apache.org/download.cgi

## Quickstart


### Running the project
To run the project, simply type from your command-line.
```
sh scripts/hbcpie.sh
```

In case you're working on windows, type the following command to index the collection.
```
mvn exec:java@indexer
```

The next step is to execute the following command to run the IE pipeline.
```
mvn exec:java@extractor
```


## Test REST API with Swagger UI
The project uses spring boot and spring fox with swagger ui.

From the command line, execute
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
For all information please contact the HBCP team (humanbehaviourchange@ucl.ac.uk) 
