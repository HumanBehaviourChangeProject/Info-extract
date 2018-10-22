/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extractor;

import indexer.ResearchDoc;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import ref.JSONRefParser;

/**
 *
 * @author dganguly
 */
public class AbstractPercentageDoubleValueAttributesFactory {
    
    public static List<InformationUnit> createInformationUnits(InformationExtractor extractor) {
        List<InformationUnit> objects = new ArrayList<>();
        
        Properties prop = extractor.prop;
        
        String attribIdsStr1 = prop.getProperty("attributes.typedetect.ids_Outcome");
        String attribIdsStr2 = prop.getProperty("attributes.typedetect.ids_Effect");
                        
        String[] attribIds1 = attribIdsStr1.split(",");
        String[] attribIds2 = attribIdsStr2.split(",");
        
        for(int i=0; i<attribIds1.length; i++){
            objects.add(new PercentageDoubleValueUnit(extractor, ResearchDoc.FIELD_CONTENT, JSONRefParser.OUTCOME_VALUE, attribIds1[i]));
        }
        
        for(int i=0; i<attribIds2.length; i++){
            objects.add(new PercentageDoubleValueUnit(extractor, ResearchDoc.FIELD_CONTENT, JSONRefParser.EFFECT, attribIds2[i]));
        }

        return objects;
    }

}
