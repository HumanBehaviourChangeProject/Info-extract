package com.ibm.drl.hbcp.parser.pdf.manager;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface PdfParsingConfig {

    enum ContentToUse {
        ALL,
        ONLY_TEXT,
        ONLY_TABLES
    }

    List<Pair<RegisteredPdfParsers.PdfParser, ContentToUse>> getParsingOrder();

    static PdfParsingConfig only(RegisteredPdfParsers.PdfParser parser) {
        return () -> Lists.newArrayList(Pair.of(parser, ContentToUse.ALL));
    }
}
