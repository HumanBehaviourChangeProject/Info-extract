package com.ibm.drl.hbcp.parser.jsonstructure;


import lombok.Getter;
import lombok.Setter;

/**
 * POJO mirroring the expected structure of JSON annotation files.
 * All classes in this package correspond to a JSON object type in the file.
 *
 * @author marting
 */
@Getter @Setter
public class JsonAnnotationFile {

    private JsonCodeSet[] codeSets;
    private JsonReference[] references;
}
