/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor.matcher;

import java.util.ArrayList;
import java.util.List;

/**
 * The class which stores the positions of the query term matches within a passage.
 * Useful for reranking based on the positional differences.
 * 
 * @author dganguly
 */
public class QueryTermMatch {

    String term;
    List<Integer> pos;

    public QueryTermMatch(String term) {
        this.term = term;
        pos = new ArrayList<>();
    }

    void add(int pos) {
        this.pos.add(pos);
    }

    @Override
    public String toString() {
        return term + ": " + pos;
    }
}


