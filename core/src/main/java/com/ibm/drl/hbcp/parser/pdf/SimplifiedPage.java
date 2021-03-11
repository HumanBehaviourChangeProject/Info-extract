package com.ibm.drl.hbcp.parser.pdf;

import java.util.ArrayList;
import java.util.List;

public class SimplifiedPage implements Page {

    private final Page originalPage;

    public SimplifiedPage(Page originalPage) {
        this.originalPage = originalPage;
    }

    /** Same as the old getBlocks, but consecutive LINES blocks are concatenated for better human readability */
    @Override
    public List<? extends Block> getBlocks() {
        List<? extends Block> blocks = originalPage.getBlocks();
        List<Block> res = new ArrayList<>();
        String currentText = "";
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            if (b.getType() == Block.Type.LINES) {
                // add its text to the current text
                currentText += "\n\n" + b.getValue();
            } else {
                if (!currentText.isEmpty()) {
                    res.add(new SimpleTextBlock(postProcessText(currentText)));
                }
                currentText = "";
                // this is a table block, add it as is
                res.add(b);
            }
        }
        // empty the text into a last block if needed
        if (!currentText.isEmpty()) {
            res.add(new SimpleTextBlock(postProcessText(currentText)));
        }
        return res;
    }

    private String postProcessText(String text) {
        String res = text;
        res = res.trim();
        return res;
    }
}
