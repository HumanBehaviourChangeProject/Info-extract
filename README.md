# Human Behaviour Change Project (HBCP)

The Human Behaviour-Change Project (HBCP) is a collaboration between behavioral scientists, computer scientists and 
system architects that aims to revolutionize methods for synthesizing evidence in real time and generate new insights on 
behavior change.

This repository includes code for two important tasks in the project: behavior change entity extraction and prediction 
for behavior change (e.g., outcome value given a set of population and intervention entities).

## Getting Started

These instructions will get you a copy of the project up and running on your local machine, with a REST API ready to use. 

### Prerequisite

[Docker](https://docs.docker.com/get-docker/) is the only requirement.

⚠️ Make sure that your Docker runtime memory option is set to at least 8GB.
* Mac: https://docs.docker.com/docker-for-mac/#memory
* Windows: https://docs.docker.com/docker-for-windows/#advanced

Make sure your version of Docker (e.g. Docker Desktop on Mac) is running.

### Running the API

After cloning the project, open a terminal in the root `hbcpIE` directory and simply type this command:

```
docker-compose up
```

Docker will set up our API and install all of its requirements for you. This may take a while as
this also trains some of the machine learning models we use. When it's over the last line you
should see in your terminal should be something like:
```
hbcp-core | 17-Feb-2021 13:21:37.079 INFO [main] org.apache.catalina.startup.Catalina.start Server startup in [60,591] milliseconds
```

### Example Usage

The easiest way to try the API is to extract all the entities in a behavior change article in PDF format. 

Here is a study of Lou et al. (2013) to get you started: https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3704267/pdf/1471-2296-14-91.pdf.
You can download this file and feed it to the API through the interface. Of course, you can use any other PDF of behavior change literature.

1. Open a web browser and go to http://127.0.0.1:8080/swagger-ui.html.
1. Look at the bottom of the page and click on `extractor-controller` to view its calls.
1. Click on the first call: `/api/extract/all`.
1. Find the `file` parameter and click on `Choose file` to select your PDF.
1. Click on `Try it out!` and wait for 1-2 minutes.
1. You can then view all the extracted entities in JSON format in the `Response Body`.

## Publications

If you use the system please cite:

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
* Francesca Bonin
* Martin Gleize
* Yufang Hou
* Pierpaolo Tommasi

## Acknowledgments
Thanks to the UCL annotators that developed the Behaviour Change Intervention Ontology.

## License
This program is free software; you can redistribute it and/or modify it under the terms of the [Apache License 
Version 2.0](./LICENSE).
