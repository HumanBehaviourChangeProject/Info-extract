package com.ibm.drl.hbcp.core.attributes;

import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.api.IUnitPOJO;
import com.ibm.drl.hbcp.extractor.InformationUnit;
import com.ibm.drl.hbcp.predictor.api.Jsonable;
import com.ibm.drl.hbcp.util.LuceneField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import javax.json.Json;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

/**
 * An extracted attribute-value pair attached to an arm in a study.
 * @author marting
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ArmifiedAttributeValuePair extends ExtractedAttributeValuePair implements Jsonable {

    protected final Arm arm;
    protected final String context;
    protected final String pageNumber; // can be "0", "1", "2"..., but also "0-1", for things spread across pages

    // marting: TODO: this is only used in the non-armified setting, and should be avoided if the arm is handled
    public static final String DUMMY_ARM = "DUMMY-ARM";

    public ArmifiedAttributeValuePair(Attribute attribute, String value, String docName, Arm arm, String context, String pageNumber) {
        super(attribute, value, docName);
        this.arm = arm;
        this.context = context;
        this.pageNumber = pageNumber;
    }

    public ArmifiedAttributeValuePair(Attribute attribute, String value, String docname, Arm arm, String context) {
        this(attribute, value, docname, arm, context, "0");
    }

    public ArmifiedAttributeValuePair(Attribute attribute, String value, String docName, String arm, String context) {
        this(attribute, value, docName, new Arm(arm), context, "0");
    }

    public ArmifiedAttributeValuePair(Attribute attribute, String value, String docName, Arm arm) {
        this(attribute, value, docName, arm, "", "0");
    }

    public ArmifiedAttributeValuePair(Attribute attribute, String value, String docName, String armName) {
        this(attribute, value, docName, new Arm(armName));
    }

    public ArmifiedAttributeValuePair withArm(Arm arm) {
        return new ArmifiedAttributeValuePair(attribute, value, docName, arm, context, pageNumber);
    }

    public static ArmifiedAttributeValuePair withPage(ArmifiedAttributeValuePair avp, String pageNumber) {
        return new ArmifiedAttributeValuePair(avp.attribute, avp.value, avp.getDocName(), avp.arm, avp.context, pageNumber);
    }

    public Document getLuceneDocument() {
        Document doc = new Document();

        ///+++IE-CODE-REFACTOR: Revisit this --- should we be puttting more
        // meaningful info here?
        doc.add(new Field(InformationUnit.ATTRIB_TYPE_FIELD,
                getAttribute().getType() == AttributeType.INTERVENTION ? "Intervention" : "Context",
                LuceneField.STORED_NOT_ANALYZED.getType()));
        ///---IE-CODE-REFACTOR

        doc.add(new Field(InformationUnit.DOCNAME_FIELD,
                getDocName(),
                LuceneField.STORED_NOT_ANALYZED.getType()));

        doc.add(new Field(InformationUnit.ATTRIB_NAME_FIELD,
                getAttribute().getName(),
                LuceneField.STORED_NOT_ANALYZED.getType()));

        doc.add(new Field(InformationUnit.ARM_ID,
                getArm().getId(),
                LuceneField.STORED_NOT_ANALYZED.getType()));

        doc.add(new Field(InformationUnit.ARM_NAME,
                getArm().getStandardName(),
                LuceneField.STORED_NOT_ANALYZED.getType()));

        doc.add(new Field(InformationUnit.EXTRACTED_VALUE_FIELD,
                getValue(),
                LuceneField.STORED_NOT_ANALYZED.getType()));

        doc.add(new Field(InformationUnit.CONTEXT_FIELD,
                getContext(),
                LuceneField.STORED_NOT_ANALYZED.getType()));

        // For exporting JSONs
        IUnitPOJO iupojo = new IUnitPOJO(
                getDocName(), getValue(),
                getAttribute().getName(), getContext());

        doc.add(new Field(InformationUnit.JSON_FIELD,
                iupojo.toString(),
                LuceneField.STORED_NOT_ANALYZED.getType()));

        // Leave it upto the concrete instances to fill up specific fields,
        // e.g. the attribute id etc.
        //appendFields(doc);
        return doc;
    }

    @Override
    public JsonValue toJson() {
        return Json.createObjectBuilder()
                .add("attribute", attribute.toJson())
                .add("value", value)
                .add("docname", docName)
                .add("arm", arm.toJson())
                .add("context", context)
                .add("page", pageNumber)
                .build();
    }
}
