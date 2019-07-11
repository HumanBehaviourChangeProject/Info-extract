package com.ibm.drl.hbcp.predictor.api;

import javax.json.Json;
import javax.json.JsonValue;

import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;

/**
 * An attribute instance which can be serialized to a JSON object, for use in the API.
 * @author charlesj
 */
public class AttributeInstInfo implements Jsonable {
    public final String normalizedValue;
    public final String pdfFile;
    public final String context;
    public final int pageNum;

    public AttributeInstInfo(String normalizedValue, String pdfFile, String context, int pageNum) {
        this.normalizedValue = normalizedValue;
        this.pdfFile = pdfFile;
        this.context = context;
        this.pageNum = pageNum;
    }

    public AttributeInstInfo(AnnotatedAttributeValuePair att) {
        this.normalizedValue = att.getValue();  // TODO check that this will be normalized
        this.pdfFile = att.getDocName();
        this.context = att.getContext();
        this.pageNum = att.getAnnotationPage();
    }

    @Override
    public JsonValue toJson() {
        return Json.createObjectBuilder()
                .add("pdfFile", pdfFile)
                .add("context", context)
                .add("pageNum", pageNum)
                .build();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + ((normalizedValue == null) ? 0 : normalizedValue.hashCode());
        result = prime * result + pageNum;
        result = prime * result + ((pdfFile == null) ? 0 : pdfFile.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AttributeInstInfo other = (AttributeInstInfo) obj;
        if (context == null) {
            if (other.context != null)
                return false;
        } else if (!context.equals(other.context))
            return false;
        if (normalizedValue == null) {
            if (other.normalizedValue != null)
                return false;
        } else if (!normalizedValue.equals(other.normalizedValue))
            return false;
        if (pageNum != other.pageNum)
            return false;
        if (pdfFile == null) {
            if (other.pdfFile != null)
                return false;
        } else if (!pdfFile.equals(other.pdfFile))
            return false;
        return true;
    }

}
