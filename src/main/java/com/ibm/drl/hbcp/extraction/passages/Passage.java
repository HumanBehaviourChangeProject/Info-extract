package com.ibm.drl.hbcp.extraction.passages;

import com.ibm.drl.hbcp.extraction.Scored;

/**
 * A text with a score, and the name of the document where it was found.
 *
 * @author marting
 */
public interface Passage extends Scored {

    String getText();

    String getDocname();
}
