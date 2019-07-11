/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import java.util.*;

import com.ibm.drl.hbcp.parser.JSONRefParser;

/**
 * The factory class for creating the information extractor objects.
 *
 * @author dganguly
 */
public class InformationExtractorFactory {
    InformationExtractor extractor;
    
    public InformationExtractorFactory(InformationExtractor extractor) {
        this.extractor = extractor;
    }
    
    /**
     * Create the information units - both supervised and unsupervised. The properties
     * file controls what gets created and what doesn't.
     * @return A list of 'InformationUnit' objects.
     */    
    public List<InformationUnit> createIUnits() {
        List<InformationUnit> listToExtract = new ArrayList<>();
        Properties prop = extractor.prop;
        
        boolean extractContext = Boolean.parseBoolean(prop.getProperty("extract.context", "false"));
        
        if (extractContext) {
            listToExtract.add(new PopulationMinAge(extractor, ResearchDoc.FIELD_CONTENT, AttributeType.POPULATION));
            listToExtract.add(new PopulationMaxAge(extractor, ResearchDoc.FIELD_CONTENT, AttributeType.POPULATION));
            listToExtract.add(new PopulationGender(extractor, ResearchDoc.FIELD_CONTENT, AttributeType.POPULATION));
            listToExtract.add(new PopulationMeanAge(extractor, ResearchDoc.FIELD_CONTENT, AttributeType.POPULATION));
        }

        boolean extractOutcomes = Boolean.parseBoolean(prop.getProperty("extract.outcome", "false"));
        if (extractOutcomes) {
            List<InformationUnit> outcomeAndEffectValueIUs = AbstractPercentageDoubleValueAttributesFactory.createInformationUnits(extractor);
            for (InformationUnit iu: outcomeAndEffectValueIUs)
                listToExtract.add(iu);
        }

        boolean extractArms = Boolean.parseBoolean(prop.getProperty("extract.arms", "false"));
        if(extractArms){
            listToExtract.add(new ArmsIdentifier(extractor, ResearchDoc.FIELD_CONTENT, AttributeType.OUTCOME_VALUE));
        }
        
        AbstractDetectPresenceAttributesFactory fact = new AbstractDetectPresenceAttributesFactory(extractor);
        listToExtract.addAll(fact.createInformationUnits());
        
        return listToExtract;
    }
    
    /**
     * Creates the supervised units only.
     * @return A list of supervised information units.
     */    
    public List<InformationUnit> createSupervisedIUnits() {
        AbstractDetectPresenceAttributesFactory fact = new AbstractDetectPresenceAttributesFactory(extractor);
        return fact.createSupervisedInformationUnits();
    }

    /**
     * Checks for a particular attribute id in a list of InformationUnit objects.
     * @param iuList
     * @param attribId
     * @return The object with the given attribute id if it exists in the list, else null.
     */    
    public static InformationUnit search(List<InformationUnit> iuList, String attribId) {
        for (InformationUnit iu: iuList) {
            if (iu.matches(attribId))
                return iu;
        }
        return null;
    }
}
