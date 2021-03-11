package com.ibm.drl.hbcp.parser.pdf.textract.structure;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter
public class Document {

    private DocumentMetadata documentMetadata;
    private String jobStatus;
    private List<Block> blocks;

    @Override
    public String toString() {
        return StringUtils.join(blocks.stream().map(Block::toString).collect(Collectors.toList()), "\n");
    }
}
