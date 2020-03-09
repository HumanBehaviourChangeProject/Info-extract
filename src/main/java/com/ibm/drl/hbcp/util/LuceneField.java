package com.ibm.drl.hbcp.util;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

import java.util.function.Consumer;

/**
 * A utility class to facilitate the Lucene 5 to Lucene 8 port.
 * A lot of the static members of Field previously deprecated in 5 disappeared in 8, most of the functionality
 * is present in the FieldType class. LuceneField is a wrapper around FieldType, useful for readability.
 *
 * @author marting
 */
public class LuceneField {

    private final FieldType type;

    public final static LuceneField STORED_NOT_ANALYZED = new LuceneField().stored(true).analyzed(false);

    public LuceneField() {
        type = new FieldType();
        type.setIndexOptions(IndexOptions.DOCS);
    }

    private LuceneField(LuceneField base) {
        type = new FieldType(base.type);
    }

    public LuceneField stored(boolean isStored) {
        LuceneField res = new LuceneField(this);
        res.type.setStored(isStored);
        return res;
    }

    public LuceneField analyzed(boolean isAnalyzed) {
        LuceneField res = new LuceneField(this);
        res.type.setTokenized(isAnalyzed);
        return res;
    }

    public LuceneField termVectorsWithPositions() {
        LuceneField res = new LuceneField(this);
        res.type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        res.type.setStoreTermVectors(true);
        res.type.setStoreTermVectorPositions(true);
        return res;
    }

    public LuceneField with(Consumer<FieldType> change) {
        LuceneField res = new LuceneField(this);
        change.accept(res.type);
        return res;
    }

    public FieldType getType() { return type; }
}
