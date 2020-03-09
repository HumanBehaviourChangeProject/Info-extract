package com.ibm.drl.hbcp.core.attributes;

import com.ibm.drl.hbcp.predictor.api.Jsonable;

import javax.json.Json;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Describes an arm, like "control group". It has, among other things, an ID and a name.
 * @author marting
 */
public class Arm implements Jsonable {
    protected String id;
    protected final String standardName;
    private final List<String> allNames;

    /** Arm for the "empty" annotation (id 0, empty name), which by convention means the annotation applies to all arms
     * in the document. */
    public static final Arm EMPTY = new Arm("0", "");
    public static final Arm MAIN = new Arm("1", "Main group");
    public static final Arm CONTROL = new Arm("2", "Control group");

    public Arm(String id, String standardName, List<String> allNames) {
        this.id = id;
        this.standardName = standardName;
        this.allNames = allNames;
        allNames.add(standardName);
    }

    public Arm(String id, String standardName) {
        this(id, standardName, new ArrayList<>());
    }

    public Arm(String standardName) {
        /* TODO: this is likely a source of potential bugs: if standard name is empty (intending to be the "empty" arm),
         * the id should be "0", not empty string */
        this(standardName, standardName);
    }

    public boolean isEmptyArm() { return equals(Arm.EMPTY); }

    public final String getId() { return id; }

    public final List<String> getAllNames() { return allNames; }

    public final String getStandardName() { return standardName; }
    
    public void addName(String name){
        this.allNames.add(name);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Arm attribute = (Arm) o;
        return Objects.equals(id, attribute.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.valueOf(id) + "(" + standardName + ")";
    }

    @Override
    public JsonValue toJson() {
        return Json.createObjectBuilder()
                .add("id", id)
                .add("name", standardName)
                .add("names", Jsonable.getJsonStringList(allNames))
                .build();
    }
}
