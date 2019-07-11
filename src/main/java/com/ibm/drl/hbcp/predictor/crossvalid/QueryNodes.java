/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.crossvalid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.normalization.Normalizers;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.queries.AndQuery;
import com.ibm.drl.hbcp.predictor.queries.NodeQuery;
import com.ibm.drl.hbcp.predictor.queries.Query;

/**
 * This class represents a container for query nodes and is responsible for creating
 * queries for prediction from the test set of a cross validation split.
 * 
 * @author Debasis and Martin
 */
public class QueryNodes {
    HashSet<AttributeValueNode> populationNodes;
    HashSet<AttributeValueNode> interventionNodes;
    HashSet<AttributeValueNode> outcomeNodes;
    HashSet<AttributeValueNode> outcomeValueNodes;
    
    Map<String, List<AttributeValueNode>> cnodeMap, inodeMap, onodeMap, ovnodeMap;
    AttribNodeRelations test;
    HashSet<String> queryAttribs;
    boolean includeONodesInQuery;
    List<Query> testQueries;

    static Logger logger = LoggerFactory.getLogger(QueryNodes.class);

    /**
     * Constructs the container object from a properties file (usually the init.properties) and
     * a constructed graph from the `RelationGraphBuilder' class
     * @param prop Properties
     * @param test Graph constructed from the `RelationGraphBuilder' class
     */
    public QueryNodes(Properties prop, AttribNodeRelations test) {
        this.test = test;
        testQueries = new ArrayList<>();
        
        populationNodes = new HashSet<>();
        interventionNodes = new HashSet<>();
        outcomeNodes = new HashSet<>();
        outcomeValueNodes = new HashSet<>();
        
        onodeMap = new HashMap<>();
        
        queryAttribs = new HashSet<>();
        queryAttribs.addAll(Normalizers.getAttributeIdsFromProperty(prop, "prediction.testquery.population"));
        queryAttribs.addAll(Normalizers.getAttributeIdsFromProperty(prop, "prediction.testquery.outcomes"));
        queryAttribs.addAll(Normalizers.getAttributeIdsFromProperty(prop, "prediction.testquery.outcomevalues"));
        
        includeONodesInQuery = Boolean.parseBoolean(prop.getProperty("prediction.testquery.include_outcomes", "false"));
    }
    
    /**
     * Construct a single query instance for one type, e.g. "C".
     * The map 'nodeMap' is keyed by docname
     * @param docName
     * @param nodeMap
     * @return An 'AndQuery' object
     */
    public AndQuery constructTypedQueryForOneDoc(String docName, Map<String, List<AttributeValueNode>> nodeMap) {

        List<AttributeValueNode> alist = nodeMap.get(docName);
        if (alist == null)
            return null;
        
        List<Query> qterms = new ArrayList<>(); // new list for new doc
        // list of attributes for one doc
        for (AttributeValueNode a: alist) {
            NodeQuery ndq = new NodeQuery(a);
            if (ndq.node.getValue() != null) {
                qterms.add(ndq);
            }
        }
        return new AndQuery(qterms);
    }
    
    /**
     * Constructs a query object from one document. The query is constructed
     * by adding attributes that are not to be predicted, specifically the ones configured with
     * the properties 'prediction.testquery.outcomes'
     * and 'prediction.testquery.outcomevalues'. Hence, a query must comprise C, and I nodes.
     * Additionally, it may also contain O nodes if
     * 'prediction.testquery.include_outcomes' is true (i.e. we are not going to
     * predict outcome qualifiers, instead we add them as our input).
     * 
     * @param docName   The name of a document
     * @param cnodeMap  A hashmap of C type nodes keyed by the attribute id, e.g. MinAge and MaxAge would have separate keys in the map
     * @param inodeMap  A hashmap of I type nodes keyed by the attribute id
     * @param onodeMap  A hashmap of O type nodes keyed by the attribute id
     * @return A 'Query' object
     */
    public Query constructQueryForOneDoc(String docName,
        Map<String, List<AttributeValueNode>> cnodeMap,
        Map<String, List<AttributeValueNode>> inodeMap,
        Map<String, List<AttributeValueNode>> onodeMap
        ) {
        
        List<Query> allQ = new ArrayList<>();
        
        AndQuery cquery = constructTypedQueryForOneDoc(docName, cnodeMap);
        AndQuery iquery = constructTypedQueryForOneDoc(docName, inodeMap);
        AndQuery oquery = constructTypedQueryForOneDoc(docName, onodeMap);
        
        if (cquery!=null)
            allQ.add(cquery);
        
        if (iquery!=null)
            allQ.add(iquery);
        
        if (oquery!=null)
            allQ.add(oquery);
        
        return new AndQuery(allQ);
    }
    
    boolean toInclude(AttributeValueNode a) {
        return queryAttribs.contains(a.getAttribute().getId());
    }
    
    public boolean includeONodesInQuery() { return includeONodesInQuery; }
    
    /**
     * Builds a query object by collection P--I, I--O and O--OV edges for
     * each document.
     */
    public void build() {

        // loop over relations, populate set of nodes and doc-node map
        for (AttribNodeRelations.AttribNodeRelation ar: test.getNodeRelations().values()) {
            AttributeType aType = ar.source.getAttribute().getType();
            AttributeType bType = ar.target.getAttribute().getType();
            // No filter for interventions...
            if (toInclude(ar.source) || aType == AttributeType.INTERVENTION) {
                if (aType == AttributeType.POPULATION) {
                    populationNodes.add(ar.source);
                    List<AttributeValueNode> cNodes = cnodeMap.get(ar.docName);
                    if (cNodes == null) {
                        cNodes = new ArrayList<>();
                        cnodeMap.put(ar.docName, cNodes);
                    }
                    cNodes.add(ar.source);
                } else if (aType == AttributeType.INTERVENTION) {
                    interventionNodes.add(ar.source);
                    List<AttributeValueNode> iNodes = inodeMap.get(ar.docName);
                    if (iNodes == null) {
                        iNodes = new ArrayList<>();
                        inodeMap.put(ar.docName, iNodes);
                    }
                    iNodes.add(ar.source);
                } else if (aType == AttributeType.OUTCOME) {
                    outcomeNodes.add(ar.source);
                    if (includeONodesInQuery) { // if no outcome 
                        List<AttributeValueNode> oNodes = onodeMap.get(ar.docName);
                        if (oNodes == null) {
                            oNodes = new ArrayList<>();
                            onodeMap.put(ar.docName, oNodes);
                        }
                        oNodes.add(ar.source);
                    }
                }
            }
            if (bType == AttributeType.OUTCOME_VALUE) {
                outcomeValueNodes.add(ar.target);
                List<AttributeValueNode> ovNodes = ovnodeMap.get(ar.docName);
                if (ovNodes == null) {
                    ovNodes = new ArrayList<>();
                    ovnodeMap.put(ar.docName, ovNodes);
                }
                ovNodes.add(ar.source);
            }
        }        
        
        logger.debug("Size of the query lists (P/I/O/OV): " + populationNodes.size() + ", " + interventionNodes.size()
        + ", " + outcomeNodes.size() + ", " + outcomeValueNodes.size());

    }
    
    public Map<String, List<AttributeValueNode>> cnodeMap() { return cnodeMap; }
    public Map<String, List<AttributeValueNode>> inodeMap() { return inodeMap; }
    public Map<String, List<AttributeValueNode>> onodeMap() { return onodeMap; }
    public Map<String, List<AttributeValueNode>> ovnodeMap() { return ovnodeMap; }

    /**
     * Builds a list of queries to be used in the testing phase. Iterates over each
     * document and calls the 'constructQueryForOneDoc' method.
     * @return 
     */
    public List<Query> buildQueries() {
        for (String docName: cnodeMap.keySet()) {  // iterate over documents
            Query thisDocQuery = constructQueryForOneDoc(docName, cnodeMap(), inodeMap(), onodeMap());
            testQueries.add(thisDocQuery);
        }
        return testQueries;
    }    
}
