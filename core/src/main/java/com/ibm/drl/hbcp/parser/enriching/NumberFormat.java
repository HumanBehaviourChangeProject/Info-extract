package com.ibm.drl.hbcp.parser.enriching;

import com.ibm.drl.hbcp.util.ParsingUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NumberFormat {

    private final Type type;
    private final char c;

    public static final NumberFormat INT = new NumberFormat(Type.Integer, 'n');
    public static final NumberFormat DEC = new NumberFormat(Type.Decimal, 'd');

    public enum Type {
        Integer,
        Decimal,
        SpecialCharacter
    }

    public static List<NumberFormat> getFormat(String numberString) {
        if (ParsingUtils.parseAllDoubleStrings(numberString).isEmpty()) {
            return new ArrayList<>();
        } else {
            List<NumberFormat> res = new ArrayList<>();
            boolean isBeforeDecimalPoint = true;
            for (int i = 0; i < numberString.length(); i++) {
                char c = numberString.charAt(i);
                if (Character.isDigit(c) && isBeforeDecimalPoint) {
                    if (res.size() == 0 || !res.get(res.size() - 1).equals(INT))
                        res.add(INT);
                } else if (Character.isDigit(c) && !isBeforeDecimalPoint) {
                    res.add(DEC);
                } else if (!Character.isWhitespace(c)){
                    res.add(new NumberFormat(Type.SpecialCharacter, c));
                    if (c == '.') {
                        isBeforeDecimalPoint = false;
                    } else {
                        isBeforeDecimalPoint = true;
                    }
                }
            }
            return res;
        }
    }
}
