package com.ibm.drl.hbcp.parser.pdf;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;

import com.ibm.drl.hbcp.predictor.api.Jsonable;

import lombok.Value;

@Value
public class TableValue implements Jsonable {
    String value;
    List<String> rowHeaders;
    List<String> columnHeaders;
    Block tableBlock; // always a block of type TABLE

    // template with switched roles
    private final static String TEMPLATE = "The [row] of [column] has a value of [value].";
//    private final static String TEMPLATE = "The [column] of [row] has a value of [value].";
    private final static Pattern CONTAINS_LETTER_REGEX = Pattern.compile("[a-zA-Z]");

    public TableValue(String value, List<String> rowHeaders, List<String> columnHeaders, Block tableBlock) {
        this.value = value;
        this.rowHeaders = getFilteredHeaders(rowHeaders);
        this.columnHeaders = getFilteredHeaders(columnHeaders);
        this.tableBlock = tableBlock;
    }

    public String toText() {
        return TEMPLATE
                .replace("[row]",
                        StringUtils.join(rowHeaders, ": "))
                .replace("[column]",
                        StringUtils.join(columnHeaders, ": "))
                .replace("[value]", value.trim())
                .replaceAll("[\\r\\n]+", "");
    }

    private List<String> getFilteredHeaders(List<String> headers) {
        return headers.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(this::isValidSingleHeader)
                .collect(Collectors.toList());
    }

    private boolean isValidSingleHeader(String header) {
        return CONTAINS_LETTER_REGEX.matcher(header).find() &&
                !header.toLowerCase().matches(".*table [0-9]+.*");
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