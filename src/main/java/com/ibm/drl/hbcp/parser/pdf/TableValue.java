package com.ibm.drl.hbcp.parser.pdf;

import com.ibm.drl.hbcp.predictor.api.Jsonable;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class TableValue implements Jsonable {
    private final String value;
    private final List<String> rowHeaders;
    private final List<String> columnHeaders;
    private final Block tableBlock; // always a block of type TABLE

    private final static String TEMPLATE = "The [row] of [column] has a value of [value].";

    public String toText() {
        return TEMPLATE
                .replace("[row]",
                        StringUtils.join(rowHeaders.stream()
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList()), ": "))
                .replace("[column]",
                        StringUtils.join(columnHeaders.stream()
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList()), ": "))
                .replace("[value]", value.trim())
                .replaceAll("[\\r\\n]+", "");
    }

    @Override
    public JsonValue toJson() {
        JsonObjectBuilder res = Json.createObjectBuilder();
        res.add("value", value);
        res.add("rowHeaders", Jsonable.getJsonArrayFromStrings(rowHeaders));
        res.add("columnHeaders", Jsonable.getJsonArrayFromStrings(columnHeaders));
        return res.build();
    }
}