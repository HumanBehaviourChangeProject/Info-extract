package com.ibm.drl.hbcp.parser.pdf.abbyy.structure;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class FormattingConverter implements Converter {
    @Override
    public void marshal(Object o, HierarchicalStreamWriter hierarchicalStreamWriter, MarshallingContext marshallingContext) {
        throw new UnsupportedOperationException("Cannot output ABBYY XML.");
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext unmarshallingContext) {
        Formatting res = new Formatting();
        String value = reader.getValue();
        res.setValue(value);
        // read all "charParams" children
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            if ("charParams".equals(reader.getNodeName())) {
                res.getCharacters().add(reader.getValue());
            }
            reader.moveUp();
        }
        return res;
    }

    @Override
    public boolean canConvert(Class aClass) {
        return Formatting.class.isAssignableFrom(aClass);
    }
}
