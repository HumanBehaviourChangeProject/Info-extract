package com.ibm.drl.hbcp.parser;

import com.google.common.collect.ConcurrentHashMultiset;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.jsonstructure.JsonAnnotationFile;
import com.ibm.drl.hbcp.parser.jsonstructure.JsonAttribute;
import com.ibm.drl.hbcp.parser.jsonstructure.JsonAttributes;
import com.ibm.drl.hbcp.parser.jsonstructure.JsonCodeSet;

import java.util.*;
import java.util.stream.Collectors;

public class CodeSetTreeBuilder {

    private final Attributes attributes;

    private final CodeSetTree[] trees;

    public CodeSetTreeBuilder(JsonAnnotationFile json,
                              Attributes attributes,
                              AttributeValueCollection<AnnotatedAttributeValuePair> avps) {
        this.attributes = attributes;
        // build the (root of the) trees
        CodeSetTreeNode[] roots = initializeCodeSetTreeArray(attributes);
        for (JsonCodeSet codeSet : json.getCodeSets()) {
            AttributeType type = AttributeType.fromName(codeSet.getSetName());
            if (type != AttributeType.NONE)
                roots[type.code()] = getRootCodeSetTreeNode(type, codeSet);
        }
        // fill the PerDocRefs
        for (CodeSetTreeNode root : roots) {
            if (root != null) {
                fillPerDocRefs(root, avps);
            }
        }
        // complete the trees
        trees = new CodeSetTree[roots.length];
        for (int i = 0; i < trees.length; i++) {
            if (roots[i] != null)
                trees[i] = new CodeSetTree(roots[i]);
        }
    }

    public CodeSetTree[] getTrees() { return trees; }

    private void fillPerDocRefs(CodeSetTreeNode node, AttributeValueCollection<AnnotatedAttributeValuePair> avps) {
        if (!node.isRoot()) {
            // fill the refs for this attribute
            Attribute attribute = node.getAttribute();
            for (AnnotatedAttributeValuePair avp : avps.byId().getOrDefault(attribute.getId(), ConcurrentHashMultiset.create())) {
                node.refs.add(avp.getDocName(), avp);
                node.refs.addArm(avp.getDocName(), avp.getArm().getStandardName(), avp);
            }
        }
        // fill the refs of the node's children
        for (CodeSetTreeNode child : node.getChildren()) {
            fillPerDocRefs(child, avps);
        }
    }

    private CodeSetTree getCodeSetTree(AttributeType type, JsonCodeSet codeSet) {
        CodeSetTreeNode root = getRootCodeSetTreeNode(type, codeSet);
        return new CodeSetTree(root);
    }

    private CodeSetTreeNode getRootCodeSetTreeNode(AttributeType type, JsonCodeSet codeSet) {
        return CodeSetTreeNode.buildRoot(codeSet.getSetName(), String.valueOf(codeSet.getSetId()),
                parent -> getChildren(parent, codeSet.getAttributes()));
    }

    private List<CodeSetTreeNode> getChildren(CodeSetTreeNode parent, JsonAttributes jsonChildren) {
        if (jsonChildren != null) {
            return Arrays.stream(jsonChildren.getAttributesList())
                    .map(jsonAttribute -> getCodeSetTreeNode(parent, jsonAttribute))
                    .collect(Collectors.toList());
        } else return new ArrayList<>();
    }

    private CodeSetTreeNode getCodeSetTreeNode(CodeSetTreeNode parent, JsonAttribute jsonAttribute) {
        return CodeSetTreeNode.buildAttributeNode(attributes.getFromId(String.valueOf(jsonAttribute.getAttributeId())),
                parent,
                newParent -> getChildren(newParent, jsonAttribute.getAttributes()));
    }

    private CodeSetTreeNode[] initializeCodeSetTreeArray(Attributes attributes) {
        Set<AttributeType> types = attributes.groupedByType().keySet();
        Optional<AttributeType> greatestType = types.stream().max(new Comparator<AttributeType>() {
            @Override
            public int compare(AttributeType o1, AttributeType o2) {
                return Integer.compare(o1.code(), o2.code());
            }
        });
        int length = greatestType.isPresent() ? greatestType.get().code() + 1 : 0;
        return new CodeSetTreeNode[length];
    }
}
