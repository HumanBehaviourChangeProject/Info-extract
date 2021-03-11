package com.ibm.drl.hbcp.parser.pdf;

import com.ibm.drl.hbcp.predictor.api.Jsonable;
import com.ibm.drl.hbcp.util.FileUtils;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface Document extends Element, Jsonable {

    List<? extends Page> getPages();

    default List<? extends Page> getPages(Predicate<Block> filter) {
        return getPages().stream()
                .map(p -> new Page() {
                    @Override
                    public List<? extends Block> getBlocks() {
                        return ((Page) p).getBlocks().stream()
                                .filter(filter)
                                .collect(Collectors.toList());
                    }
                })
                .collect(Collectors.toList());
    }

    default List<Block> getTables() {
        return getPages().stream()
                .flatMap(p -> p.getBlocks().stream())
                .filter(b -> b.getType() == Block.Type.TABLE)
                .collect(Collectors.toList());
    }

    @Override
    default String getValue() {
        return getValue(true);
    }

    default String getValue(boolean withTables) {
        StringBuilder sb = new StringBuilder();
        for (Page page : getPages()) {
            for (Block block : page.getBlocks()) {
                switch (block.getType()) {
                    case TABLE:
                        if (withTables) {
                            for (TableValue value : block.getTable()) {
                                sb.append(value.toText());
                                sb.append("\n");
                            }
                        }
                        break;
                    case LINES:
                        for (Line line : block.getLines()) {
                            sb.append(line.getValue());
                            sb.append("\n");
                        }
                    case OTHER:
                        // do nothing on these types
                        break;
                }
            }
        }
        return sb.toString();
    }

    /** Converts the object to its JSON equivalent. */
    @Override
    default JsonValue toJson() {
        JsonObjectBuilder docBuilder = Json.createObjectBuilder();
        JsonArrayBuilder res = Json.createArrayBuilder();
        int pageNumber = 1;
        for (Page page : getPages()) {
            for (Block block : page.getBlocks()) {
                if (block.getType() != Block.Type.OTHER) {
                    // add a block object
                    JsonObjectBuilder blockBuilder = Json.createObjectBuilder();
                    blockBuilder.add("type", block.getType().toString());
                    switch (block.getType()) {
                        case LINES:
                            blockBuilder.add("value", block.getValue());
                            break;
                        case TABLE:
                            blockBuilder.add("cells", Jsonable.getJsonArrayFromCollection(block.getTable()));
                            break;
                        default:
                            throw new AssertionError("Unhandled type of block: " + block.getType());
                    }
                    blockBuilder.add("page", pageNumber);
                    res.add(blockBuilder.build());
                }
            }
            pageNumber++;
        }
        docBuilder.add("blocks", res.build());
        return docBuilder.build();
    }

    default JsonValue toHumanReadableJson(String title, String shortTitle, String filename, String introduction) {
        JsonObjectBuilder docBuilder = Json.createObjectBuilder();
        docBuilder.add("title", title);
        docBuilder.add("shortTitle", shortTitle);
        docBuilder.add("filename", filename);
        docBuilder.add("introduction", introduction);
        JsonArrayBuilder res = Json.createArrayBuilder();
        int pageNumber = 1;
        for (Page page : getPages()) {
            // simplify the page
            SimplifiedPage simplePage = new SimplifiedPage(page);
            for (Block block : simplePage.getBlocks()) {
                if (block.getType() != Block.Type.OTHER) {
                    // add a block object
                    JsonObjectBuilder blockBuilder = Json.createObjectBuilder();
                    // rename the types a bit so they're more friendly
                    switch (block.getType()) {
                        case LINES:
                            blockBuilder.add("type", "TEXT");
                            blockBuilder.add("text", block.getValue());
                            break;
                        case TABLE:
                            blockBuilder.add("type", "TABLE");
                            blockBuilder.add("cells", Jsonable.getJsonArrayFromCollection(block.getTable()));
                            break;
                        default:
                            throw new AssertionError("Unhandled type of block: " + block.getType());
                    }
                    blockBuilder.add("page", pageNumber);
                    res.add(blockBuilder.build());
                }
            }
            pageNumber++;
        }
        docBuilder.add("content", res.build());
        return docBuilder.build();
    }

    default void writeToFile(File file) throws IOException {
        FileUtils.writeJsonToFile(toJson(), file);
    }

}
