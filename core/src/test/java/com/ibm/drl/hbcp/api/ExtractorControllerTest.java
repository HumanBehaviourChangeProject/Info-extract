package com.ibm.drl.hbcp.api;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.util.FileUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.tika.exception.TikaException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.Assert;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class ExtractorControllerTest {

    private static ExtractorController controller;

    @BeforeClass
    public static void createExtractorController() throws IOException, ParseException {
        controller = new ExtractorController();
    }

    /** Tests that the API can consume a single PDF file and extract entities with the unsupervised IE system
     * using an in-memory indexed version of the paper. */
    @Test
    public void testExtractMultipartFileUnsupervised() throws IOException, ParseException, TikaException, SAXException {
        try (FileInputStream is = new FileInputStream(FileUtils.potentiallyGetAsResource(new File("testcoll/Ames 2014.pdf")))) {
            MockMultipartFile pdf = new MockMultipartFile("Ames 2014.pdf", is);
            List<ArmifiedAttributeValuePair> result = controller.extractMultipartFiles(10, "5,10,20", 0.2f,
                    false, true, Lists.newArrayList(pdf)).get(0);
            Assert.notEmpty(result, "The unsupervised IE system should extract at least 1 value in test doc Ames 2014.pdf");
        }
    }

    /** Tests that the API can consume two PDF files */
    @Test
    public void testExtractMultipartFilesUnsupervised() throws IOException, ParseException, TikaException, SAXException {
        try (FileInputStream is = new FileInputStream(FileUtils.potentiallyGetAsResource(new File("testcoll/Ames 2014.pdf")))) {
            MockMultipartFile pdf = new MockMultipartFile("Ames 2014.pdf", is);
            List<ArmifiedAttributeValuePair> result = controller.extractMultipartFiles(10, "5,10,20", 0.2f,
                    false, true, Lists.newArrayList(pdf, pdf)).get(0);
            Assert.notEmpty(result, "The unsupervised IE system should extract at least 1 value in test doc Ames 2014.pdf (uploaded twice)");
        }
    }
}
