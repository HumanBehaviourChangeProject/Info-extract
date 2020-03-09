# Human Behaviour Change Project (HBCP)

The Human Behaviour-Change Project (HBCP) is a collaboration between behavioral scientists, computer scientists and 
system architects that aims to revolutionize methods for synthesizing evidence in real time and generate new insights on 
behavior change.

This repository includes code for two important tasks in the project: behavior change entity extraction and prediction 
for behavior change (e.g., outcome value given a set of population and intervention entities).

## Getting Started

These instructions will get you a copy of the project up and running on your local machine.

### Prerequisites

hbcpIE uses Java 1.8 and needs to have [Maven](https://maven.apache.org/) installed to compile and run the project. 

### Installing

After cloning the project go the the root `hbcpIE` directory.

Compile the code:
```
mvn clean compile -U
```

## Example Usage

Once you've confirmed that Maven can build the code, there are two ways to quickly test our APIs.
1. [Use with Maven commands](#with-maven-commands)
1. [Use with Docker](#with-docker)

### With Maven commands

The easiest way to test our entity extraction and prediction APIs is via a [Swagger](https://swagger.io/) interface 
using your own PDFs of behavior change literature. Before doing that, we need to build the indexes used by extraction
and prediction. This is done with the following commands:
```
mvn exec:java@indexer
```
and 
```
mvn exec:java@extractor
```

(you should see after each of these something like `[INFO] BUILD SUCCESS`)

Next we will start the server that will allow us to access the Swagger interface:
```
mvn spring-boot:run
```

This will take several seconds to start.  After it has started, open a web browser and go to 
http://127.0.0.1:8080/swagger-ui.html. You can then follow the instructions on that page to see how to use the extractor and
predictor APIs. 

### With Docker

First build the project with:
```
mvn clean install
```
Build the docker image with:
```
docker build .
```
The only thing you might need is to increase your Docker runtime memory option to at least 8GB.

Run a docker container exposing the API on port 8080:
```
docker run -t -p 8080:8080 [your_image_id]
```
It will take a while, as it is indexing and running the extraction. After it has started, open a web browser and go to http://127.0.0.1:8080/swagger-ui.html. You can then follow the instructions on that page to see how to use the extractor and predictor APIs.

## Publications

If you use the extractor please cite:

* Debasis Ganguly, Yufang Hou, Léa A. Deleris, Francesca Bonin: 
  [Information Extraction of Behavior Change Intervention Descriptions](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6568066/). AMIA Joint Summits on Translational Science 
  proceedings 2019:182–191.

#### Other Related Publications
* Yufang Hou, Debasis Ganguly, Léa A. Deleris, Francesca Bonin:
  [Extracting Factual Min/Max Age Information from Clinical Trial Studies](https://www.aclweb.org/anthology/W19-1914/). 
  Proceedings of the 2nd Clinical Natural Language Processing Workshop 2019: 107-116.
* Debasis Ganguly, Léa A. Deleris, Pol Mac Aonghusa, Alison J. Wright, Ailbhe N. Finnerty, Emma Norris, Marta M. Marques, Susan Michie:
  [Unsupervised Information Extraction from Behaviour Change Literature](http://ebooks.iospress.nl/publication/48878). MIE 2018: 680-684
* Susan Michie, James Thomas, Marie Johnston, Pol Mac Aonghusa, John Shawe-Taylor, Michael P. Kelly, Léa A. Deleris, 
  Ailbhe N. Finnerty, Marta M. Marques, Emma Norris, Alison O’Mara-Eves, Robert West: [The Human Behaviour-Change Project: 
  harnessing the power of artificial intelligence and machine learning for evidence synthesis and interpretation](https://doi.org/10.1186/s13012-017-0641-5). 
  Implementation Science 12, 121 (2017). 


## Team Members
* Debasis Ganguly
* Martin Gleize
* Yufang Hou
* Charles Jochim
* Francesca Bonin
* Pierpaolo Tommasi

## Acknowledgments
Thanks to the UCL annotators that developed the Behaviour Change Intervention Ontology.

## License
This program is free software; you can redistribute it and/or modify it under the terms of the [Apache License 
Version 2.0](./LICENSE).


