# Human Behaviour Change Project Information Extraction (hbcpIE)  version 0.1

02/10/2018

Copyright (c): Apache License Version 2.0

Created by HBCP team, IBM Research, Dublin

* Main author: Debasis Ganguli
* Code contributions: Francesca Bonin, Yufang Hou, Lea Deleris

Java version: "1.8.0"

The project executes the following action:
1. Indexes a collection of documents
2. Store them in a Lucene index
3. Extracts pieces of information from arbitrary passages.


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
Thanks to the UCL annotators that developed the Behavioral Change Ontology

## CHANGES
- version 0.1 

## LICENSE
This program is free software; you can redistribute it and/or
 modify it under the terms of the Apache License Version 2.0.

## Contact
For all information please contact the HBCP team (https://www.humanbehaviourchange.org/contact-us) 
