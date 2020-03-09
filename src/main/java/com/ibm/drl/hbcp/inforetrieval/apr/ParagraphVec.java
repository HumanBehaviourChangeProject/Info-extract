package com.ibm.drl.hbcp.inforetrieval.apr;

public class ParagraphVec extends AttributeVec implements Comparable<ParagraphVec> {
    String para_id;
    protected float simWithQuery;

    public ParagraphVec(int id, String id_str) {
        super(id);
        this.para_id = id_str;
    }

    public ParagraphVec(AttributeVec vec) {
        super(vec);
    }

    public void setQuerySim(float sim) { this.simWithQuery = sim; }

    @Override
    public int compareTo(ParagraphVec that) {
        return Float.compare(that.simWithQuery, simWithQuery);  // descending
    }
    
    @Override
    public String toString() {
        return para_id;
    }
}
