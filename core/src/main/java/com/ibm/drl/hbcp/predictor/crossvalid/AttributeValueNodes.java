/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.crossvalid;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.normalization.Normalizers;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.queries.AndQuery;
import com.ibm.drl.hbcp.predictor.queries.NodeQuery;
import com.ibm.drl.hbcp.predictor.queries.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents a container for query nodes and is responsible for creating
 * queries for prediction from the test set of a cross validation split.
 *
 * @author Debasis and Martin
 */
public class AttributeValueNodes {
    HashSet<AttributeValueNode> populationNodes;
    HashSet<AttributeValueNode> interventionNodes;
    HashSet<AttributeValueNode> outcomeNodes;
    HashSet<AttributeValueNode> outcomeValueNodes;
    List<String> docNames;

    Map<String, Set<AttributeValueNode>> cnodeMap, inodeMap, onodeMap, ovnodeMap;  // keyed by doc name
    AttribNodeRelations source;
    private final Set<Attribute> knownAttributeSource;
    HashSet<String> queryAttribs;
    boolean includeONodesInQuery;
    List<Query> testQueries;

    static Logger logger = LoggerFactory.getLogger(AttributeValueNodes.class);

    /**
     * Constructs the container object from a properties file (usually the init.properties) and
     * a constructed graph from the `RelationGraphBuilder' class
     * @param prop Properties
     * @param knownAttributeSource Graph constructed from the `RelationGraphBuilder' class
     */
    public AttributeValueNodes(Properties prop, AttribNodeRelations source, AttribNodeRelations knownAttributeSource) {
        this.source = source;
        this.knownAttributeSource = knownAttributeSource.getEdges().stream()
                .flatMap(edge -> Lists.newArrayList(edge.source, edge.target).stream())
                .map(AttributeValuePair::getAttribute)
                .collect(Collectors.toSet());
        testQueries = new ArrayList<>();

        populationNodes = new HashSet<>();
        interventionNodes = new HashSet<>();
        outcomeNodes = new HashSet<>();
        outcomeValueNodes = new HashSet<>();

        cnodeMap = new HashMap<>();
        inodeMap = new HashMap<>();
        onodeMap = new HashMap<>();
        ovnodeMap = new HashMap<>();

        queryAttribs = new HashSet<>();
        queryAttribs.addAll(Normalizers.getAttributeIdsFromProperty(prop, "prediction.testquery.population"));
        queryAttribs.addAll(Normalizers.getAttributeIdsFromProperty(prop, "prediction.testquery.outcomes"));
        queryAttribs.addAll(Normalizers.getAttributeIdsFromProperty(prop, "prediction.testquery.outcomevalues"));

        includeONodesInQuery = Boolean.parseBoolean(prop.getProperty("prediction.testquery.include_outcomes", "false"));

        this.build();
    }

    public AttributeValueNodes(Properties prop, AttribNodeRelations source) {
        this(prop, source, source);
    }

    /**
     * Construct a single query instance for one type, e.g. "C".
     * The map 'nodeMap' is keyed by docname
     * @param docName
     * @param nodeMap
     * @return An 'AndQuery' object
     */
    public AndQuery constructTypedQueryForOneDoc(String docName, Map<String, Set<AttributeValueNode>> nodeMap) {

        Set<AttributeValueNode> alist = nodeMap.get(docName);
        if (alist == null)
            return null;

        List<Query> qterms = new ArrayList<>(); // new list for new doc
        // list of attributes for one doc
        for (AttributeValueNode a: alist) {
            NodeQuery ndq = new NodeQuery(a);
            // test if this attribute has been encountered in the train set
            if (knownAttributeSource.contains(ndq.node.getAttribute())) {
                if (ndq.node.getValue() != null) {
                    qterms.add(ndq);
                }
            }
        }
        return new AndQuery(qterms);
    }

    public void setDocNames(List<String> docNames) { this.docNames = docNames; }
    public String getDocName(int instanceNum) { return docNames.get(instanceNum); }

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
                                         Map<String, Set<AttributeValueNode>> cnodeMap,
                                         Map<String, Set<AttributeValueNode>> inodeMap,
                                         Map<String, Set<AttributeValueNode>> onodeMap
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

    // In the regression flow use every feature
    boolean toInclude(AttributeValueNode a) {
        return true;
    }

    public boolean includeONodesInQuery() { return includeONodesInQuery; }

    /**
     * Builds a query object by collection P--I, I--O and O--OV edges for
     * each document.
     */
    public final void build() {

        // loop over relations, populate set of nodes and doc-node map
        for (AttribNodeRelations.AttribNodeRelation ar: source.getNodeRelations().values()) {
            AttributeType aType = ar.source.getAttribute().getType();
            AttributeType bType = ar.target.getAttribute().getType();

            if (aType == AttributeType.POPULATION) {
                populationNodes.add(ar.source);
                Set<AttributeValueNode> cNodes = cnodeMap.get(ar.docName);
                if (cNodes == null) {
                    cNodes = new HashSet<>();
                    cnodeMap.put(ar.docName, cNodes);
                }
                cNodes.add(ar.source);
            }
            else if (aType == AttributeType.INTERVENTION) {
                interventionNodes.add(ar.source);
                Set<AttributeValueNode> iNodes = inodeMap.get(ar.docName);
                if (iNodes == null) {
                    iNodes = new HashSet<>();
                    inodeMap.put(ar.docName, iNodes);
                }
                iNodes.add(ar.source);
            }
            else if (aType == AttributeType.OUTCOME) {
                outcomeNodes.add(ar.source);
                if (includeONodesInQuery) { // if no outcome
                    Set<AttributeValueNode> oNodes = onodeMap.get(ar.docName);
                    if (oNodes == null) {
                        oNodes = new HashSet<>();
                        onodeMap.put(ar.docName, oNodes);
                    }
                    oNodes.add(ar.source);
                }
            }

            if (bType == AttributeType.OUTCOME_VALUE) {
                outcomeValueNodes.add(ar.target);
                Set<AttributeValueNode> ovNodes = ovnodeMap.get(ar.docName);
                if (ovNodes == null) {
                    ovNodes = new HashSet<>();
                    ovnodeMap.put(ar.docName, ovNodes);
                }
                ovNodes.add(ar.target);
            }
        }

        logger.debug("Size of the query lists (P/I/O/OV): " + populationNodes.size() + ", " + interventionNodes.size()
                + ", " + outcomeNodes.size() + ", " + outcomeValueNodes.size());

    }

    public Map<String, Set<AttributeValueNode>> cnodeMap() { return cnodeMap; }
    public Map<String, Set<AttributeValueNode>> inodeMap() { return inodeMap; }
    public Map<String, Set<AttributeValueNode>> onodeMap() { return onodeMap; }
    public Map<String, Set<AttributeValueNode>> ovnodeMap() { return ovnodeMap; }

    /**
     * Builds a list of queries to be used in the testing phase. Iterates over each
     * document and calls the 'constructQueryForOneDoc' method.
     * @return
     */
    public List<Query> buildQueries() {
        docNames = new ArrayList<>();

        for (String docName: ovnodeMap.keySet()) {  // iterate over documents
            Query thisDocQuery = constructQueryForOneDoc(docName, cnodeMap(), inodeMap(), onodeMap());
            testQueries.add(thisDocQuery);
            docNames.add(docName);
        }
        return testQueries;
    }
}
