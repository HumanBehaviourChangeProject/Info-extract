/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extractor;

import indexer.ResearchDoc;
import java.io.FileReader;
import java.util.*;
import ref.JSONRefParser;

/**
 * A factory class for creating objects of type 'AbstractDetectPresenceAttribute'.
 * 
 * @author dganguly
 */
public class AbstractDetectPresenceAttributesFactory {
    InformationExtractor extractor;
    Properties prop;
    
    /**
     * Creates the factory object.
     */
    public AbstractDetectPresenceAttributesFactory() throws Exception {
        prop = new Properties();
        prop.load(new FileReader(this.getClass().getClassLoader().getResource("init.properties").getPath()));
    }

    /**
     * Creates the factory object with the properties with which the specified extractor
     * object is configured.
     * @param extractor An object which invokes this factory class.
     */
    public AbstractDetectPresenceAttributesFactory(InformationExtractor extractor) {
        this.extractor = extractor;
        this.prop = extractor.prop;
    }
    
    /**
     * Creates the 'InformationUnit' objects. The true type of every object is of type
     * 'AbstractDetectPresenceAttribute'.
     * 
     * @return A list of 'AbstractDetectPresenceAttribute' type objects abstracted with its
     * generic type 'InformationUnit'.  
     */
    public List<InformationUnit> createInformationUnits() {
        
        List<InformationUnit> iuList = new ArrayList<>();
        
        createInformationUnits(iuList, JSONRefParser.INTERVENTION, "extract.bct", "attributes.typedetect.ids");        
        return iuList;
    }
    
    /**
     * Creates the 'InformationUnit' objects. The true type of every object is of type
     * 'AbstractDetectPresenceAttribute'.
     * 
     * @param iuList Appends these newly created objects to the supplied list.
     * @param code The BCT code for which to create the object.
     * @param toExtractPropName A flag whether to extract information for this attribute (as mentioned in the properties file).
     * @param attribIdNames The name of this attribute (note: this could be different from the code, which is typically shorter, e.g. '1.1' etc.).
     */
    public void createInformationUnits(
            List<InformationUnit> iuList,
            int code,
            String toExtractPropName,
            String attribIdNames) {
        
        boolean extract = Boolean.parseBoolean(prop.getProperty(toExtractPropName, "false"));
        if (!extract)
            return;
        
        String attribIdsStr = prop.getProperty(attribIdNames);
        if (attribIdsStr == null)
            return;
        
        String[] attribIds = attribIdsStr.split(",");        
        for (String attribId : attribIds) {
            iuList.add(new AbstractDetectPresenceAttribute(extractor, ResearchDoc.FIELD_CONTENT, code, attribId));
        }
    }
    
    /**
     * Creates supervised attribute extractor objects. 
     * @return 
     */
    public List<InformationUnit> createSupervisedInformationUnits() {
        List<InformationUnit> iuList = new ArrayList<>();
        Properties prop = extractor.prop;
        
        boolean extract = Boolean.parseBoolean(prop.getProperty("extract.bct", "false"));
        if (!extract)
            return iuList;
        
        String attribIdsStr = prop.getProperty("attributes.typedetect.supervised");
        if (attribIdsStr == null)
            return iuList;
        
        String[] attribIds = attribIdsStr.split(",");        
        for (int i=0; i < attribIds.length; i++) {
            iuList.add(new AbstractDetectPresenceAttribute(extractor,
                    ResearchDoc.FIELD_CONTENT, JSONRefParser.INTERVENTION, attribIds[i]));
        }
        return iuList;
    }
    
}
