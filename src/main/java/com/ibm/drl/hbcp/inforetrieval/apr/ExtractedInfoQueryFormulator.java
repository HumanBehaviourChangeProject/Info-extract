/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.apr;

import com.ibm.drl.hbcp.extractor.InformationExtractor;
import com.ibm.drl.hbcp.extractor.InformationUnit;
import org.apache.lucene.search.Query;

import java.util.List;

/**
 *
 * @author dganguly
 */
public class ExtractedInfoQueryFormulator implements QueryFormulator {

    InformationExtractor ie;
    Retriever retriever;
    int docId;
    
    public ExtractedInfoQueryFormulator(Retriever retriever, int docId) throws Exception {
        ie = retriever.getExtractor();
        this.docId = docId;
        this.retriever = retriever;
    }

    
    @Override
    public Query constructQuery() throws Exception {
        List<InformationUnit> iuList = retriever.getIUList();
        
        StringBuffer buff = new StringBuffer();
        for (InformationUnit iu: iuList) {
            InformationUnit eu = ie.extractInformationFromDoc(docId, iu);
            if (!(eu==null || eu.getBestAnswer().getKey()==null))
                buff.append(eu.getBestAnswer().getKey());
        }
        
        BoWQueryFormulator bowFormulator = new BoWQueryFormulator(buff.toString(), retriever);
        return bowFormulator.constructQuery();
    }
}
