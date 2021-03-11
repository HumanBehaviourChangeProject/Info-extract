package com.ibm.drl.hbcp.extraction.extractors.flair;

import com.ibm.drl.hbcp.parser.pdf.Document;
import lombok.Data;

@Data
public class NamedParsedDocument {
    private final String docName;
    private final Document document;
}
