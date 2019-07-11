package com.ibm.drl.hbcp.util;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.ibm.drl.hbcp.predictor.api.AttributeInfo;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.parser.CodeSetTree;
import com.ibm.drl.hbcp.parser.CodeSetTreeNode;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.graph.RelationGraphBuilder;


/**
 * Allows the retrieval of full attribute information from their ID, in the context of the API.
 * Such functionality already exists in the parser package, so this class should be avoided and is a candidate
 * for removal.
 * @author charlesj
 */
@Deprecated
public class AttributeIdLookup {

    private JSONRefParser parser;
    private RelationGraphBuilder relationGraphBuilder;
    private boolean isInitialized;

    private static class LazyHolder {
        static final AttributeIdLookup instance = new AttributeIdLookup();
    }

    private AttributeIdLookup() {
        try {
            // ensure that the graph is built from the JSON annotations only
            Properties props = Props.loadProperties("init.properties");
            props.setProperty("prediction.source", "gt");
            parser = new JSONRefParser(props);
            relationGraphBuilder = new RelationGraphBuilder(props);
            parser.buildAll();
            isInitialized = true;
        } catch (IOException e) {
            e.printStackTrace();
            isInitialized = false;
        }
    }

    public static AttributeIdLookup getInstance() {
        return LazyHolder.instance;
    }

    public AttributeInfo getAttributeInfo(String attId) {
        // POPULATION to EFFECT  0 to 4
        for (int i = 0; i < 5; i++) {
            CodeSetTree groundTruths = parser.getGroundTruths(i);
            CodeSetTreeNode node = groundTruths.getNode(attId);
            if (node != null) {
                return new AttributeInfo(AttributeInfo.getType(i), attId, node.getName());
            }
        }
        return null;
    }

    public String getAttributeName(String attId) {
        return getAttributeInfo(attId).getName();
    }

    private static void printUsage() {
        System.out.println("USAGE: SearchAttribId <ATTRIB_ID>");
    }

    public boolean isInitialized() { return isInitialized; }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Expects an attribute id.");
            printUsage();
            System.exit(1);
        }
        
        // check valid attrib id
        try {
            Long.parseLong(args[0]);
        } catch (NumberFormatException e1) {
            System.err.println("Attribute id is expected to be a number (not " + args[0]);
            printUsage();
            System.exit(1);
        }
        
        AttributeIdLookup inst = AttributeIdLookup.getInstance();
        String attributeName = inst.getAttributeName(args[0]);
        System.out.println("Attribute " + args[0] + " has name: \"" + attributeName);
    }

}
