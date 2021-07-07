# Grobid-based PDF parser

A PDF parser implemented as a wrapper around Grobid, adding better table handling. Heavily based on code from https://github.com/IBM/science-result-extractor.

## Requirements

* Maven
* Gradle

## Installation

1. Open a terminal at the root of the pdf-parser folder
1. Download Grobid:
```
> wget https://github.com/kermitt2/grobid/archive/refs/tags/0.6.2.zip
> unzip 0.6.2.zip
```
3. Build Grobid:
```
> cd grobid-0.6.2/
> ./gradlew clean install
```

## Usage

Run the class `com.ibm.drl.hbcp.parser.pdf.grobid.GrobidParser` (modify it beforehand with the paths to your PDFs and your output JSON files).