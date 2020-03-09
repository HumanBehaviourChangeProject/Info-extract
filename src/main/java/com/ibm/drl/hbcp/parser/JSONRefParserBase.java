package com.ibm.drl.hbcp.parser;

import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.inforetrieval.apr.AttributeVec;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

/**
 * Automatically extracted interface to easily make the new JSONRefParser comply with the old one.
 *
 * @author marting
 */
public interface JSONRefParserBase {

    /**
     * Saves the parsed reference tree in secondary memory
     * @throws IOException
     */
    void save() throws IOException;

    /**
     * Parse the JSON file specified in properties file.
     *
     * @throws IOException
     */
    void parse() throws IOException;

    /**
     * Runs {@link #loadCodeSet(int)} for all code set (e.g., population, intervent, outcome, etc.)
     *
     * @throws IOException
     */
    void loadCodeSets() throws IOException;

    /**
     * Uses JSON parser to populate {@link CodeSetTree} structure for given attribute type
     * and returns this data structure.
     *
     * @param code the indicates the attribute type
     * @return populated {@link CodeSetTree} from JSON parse
     * @throws IOException
     */
    CodeSetTree loadCodeSet(int code) throws IOException;

    /**
     * Call {@link #groupByDocs(CodeSetTree)} for all attribute types
     *
     * @throws IOException
     */
    void groupByDocs() throws IOException;

    CodeSetTree getGroundTruths(int code);

    /**
     * Calls methods to parse JSON, populate {@link CodeSetTree}, and add annotated attribute references
     * per document per attribute (see {@link CodeSetTreeNode}).
     *
     * @throws IOException
     */
    void buildAll() throws IOException;

    /**
     * Calls methods to parse JSON from URL and populate {@link CodeSetTree}.
     *
     * To be called from the web server.
     * @param url
     * @throws IOException
     */
    void buildCodeSetsFromURL(URL url) throws IOException;

    /**
     * Create mapping of sprint number to set of document names
     *
     * @return map of sprint numbers to set of document names
     * @throws IOException
     */
    Map<String, Set<String>> docsBySprint() throws IOException;

    /**
     * Add annotated attribute references (per document) to {@link CodeSetTreeNode} (which represent an attribute in the
     * {@link CodeSetTree} structure).
     *
     * @param tree
     * @throws IOException
     */
    void groupByDocs(CodeSetTree tree) throws IOException;

    /**
     * Returns a vector of all the attributes present in a document.
     * @param docName The filename of the document
     * @return
     */
    AttributeVec getAttributesInDoc(String docName);

    /**
     * @return mapping of attribute ids to attribute objects
     */
    Attributes getAttributes();

    CodeSetTree[] getTrees();

    /** Returns all the attribute-value pairs parsed. */
    AttributeValueCollection<AnnotatedAttributeValuePair> getAttributeValuePairs();

    String getJSON(int code);
}
