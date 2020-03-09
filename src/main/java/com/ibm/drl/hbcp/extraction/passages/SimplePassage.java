package com.ibm.drl.hbcp.extraction.passages;


/**
 * Simple POJO implementation of a Passage
 *
 * @author marting
 */
public class SimplePassage implements Passage {

    private final String text;
    private final String docname;
    private final double score;

    public SimplePassage(String text, String docname, double score) {
        this.text = text;
        this.docname = docname;
        this.score = score;
    }

    @Override
    public String getText() { return text; }

    @Override
    public String getDocname() { return docname; }

    @Override
    public double getScore() { return score; }
}
