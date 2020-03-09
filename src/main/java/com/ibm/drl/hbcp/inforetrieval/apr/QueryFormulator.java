/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.apr;

import org.apache.lucene.search.Query;

/**
 *
 * @author dganguly
 */
public interface QueryFormulator {
    Query constructQuery() throws Exception;
}
