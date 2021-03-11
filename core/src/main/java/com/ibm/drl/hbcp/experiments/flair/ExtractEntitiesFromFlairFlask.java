/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.experiments.flair;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.reparsing.Reparser;
import com.ibm.drl.hbcp.util.Environment;
import com.ibm.drl.hbcp.util.FileUtils;

/**
 *
 * @author yhou
 */
public class ExtractEntitiesFromFlairFlask {
    
    public static void extractEntitiesFromSingleSent() {
        HttpURLConnection conn = null;
        DataOutputStream os = null;
        try {
        	String flair_address = Environment.getFlairURL() + ":" + Environment.getFlairPort();
            URL url = new URL("http://" + flair_address + "/api/v1/extractEntitiesSingleSent");
            String[] inputData = {"{\"sentence\":\"Given that there are many Asian language groups in the United States, we chose three language groups Chinese, Korean, and Vietnamese for practical reasons\"}"};
            for (String input : inputData) {
                byte[] postData = input.getBytes(StandardCharsets.UTF_8);
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("charset", "utf-8");
                conn.setRequestProperty("Content-Length", Integer.toString(input.length()));
                os = new DataOutputStream(conn.getOutputStream());
                os.write(postData);
                os.flush();

                if (conn.getResponseCode() != 200) {
                    throw new RuntimeException("Failed : HTTP error code : "
                            + conn.getResponseCode());
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        (conn.getInputStream())));

                String output;
                System.out.println("Output from Server .... \n");
                while ((output = br.readLine()) != null) {
                    System.out.println(output);
                }
                conn.disconnect();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    
    public static List<String> extractEntitiesFromMultiSent(List<String> sents){
        List<String> res = new ArrayList<>();
        HttpURLConnection conn = null;
        DataOutputStream os = null;
        try {
        	String flair_address = Environment.getFlairURL() + ":" + Environment.getFlairPort();
            URL url = new URL("http://" + flair_address + "/api/v1/extractEntitiesMultiSent"); 
//            String a1 = "Given that there are many Asian language groups in the United States";
//            String a2 = "Given that there are many Asian language groups in the United States and Ireland, we chose three language groups Chinese, Korean, and Vietnamese for practical reasons";
//            List<String> sents = new ArrayList();
//            sents.add(a1);
//            sents.add(a2);
//            String input = "{\"sentences\":" + "[" + "\"" + a1 + "\"" + "," + "\"" + a2 + "\"" + "]" + "}" ;
            String input = "{\"sentences\":" + "[" ;
            for(int i=0; i<sents.size()-1; i++){
                input = input + "\"" + sents.get(i) + "\"" + ",";
            }
            input = input + "\"" + sents.get(sents.size()-1) + "\""  + "]" + "}";
  
//            System.err.println(input);
            byte[] postData = input.getBytes(StandardCharsets.UTF_8);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty( "charset", "utf-8");
            conn.setRequestProperty("Content-Length", Integer.toString(input.length()));
            os = new DataOutputStream(conn.getOutputStream());
            os.write(postData);
            os.flush();

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
                res.add(output);
            }
            conn.disconnect();
     
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }finally
        {
            if(conn != null){
                conn.disconnect();
             }
        }
        return res;
    }

    public static List<String> generateTestingSentence(Document doc) {
        List<String> sentences = new ArrayList<>();
        try {
            for (String str : doc.getValue().split("\n")) {
                if (str.equalsIgnoreCase("acknowledgements") || str.equalsIgnoreCase("references")) {
                    break;
                }
                if (str.matches(".*?http:.*?")) {
                    continue;
                }
                if (str.split(" ").length < 6) {
                    continue;
                }
                sentences.add(escape(str));
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return sentences;
    }

    private static String escape(String raw) {
        String escaped = raw;
        escaped = escaped.replace("\\", "\\\\");
        escaped = escaped.replace("\"", "\\\"");
        escaped = escaped.replace("\b", "\\b");
        escaped = escaped.replace("\f", "\\f");
        escaped = escaped.replace("\n", "\\n");
        escaped = escaped.replace("\r", "\\r");
        escaped = escaped.replace("\t", "\\t");
        // TODO: escape other non-printing characters using uXXXX notation
        return escaped;
    }

    public static List<String> generateTestingSentence(String docName) {
        List<String> sentences = new ArrayList<>();
//        File xmlPdfOutput = new File("./data/pdfs_Sprint1234_extracted/" + docName + ".xml");
        File jsonPdfOutput = FileUtils.potentiallyGetAsResource(new File("data/pdfs_extracted/" + docName + ".json"));
        try {
            Reparser parser = new Reparser(jsonPdfOutput);
            for (String str : parser.toText().split("\n")) {
                if (str.equalsIgnoreCase("acknowledgements") || str.equalsIgnoreCase("references")) {
                    break;
                }
                if (str.matches(".*?http:.*?")) {
                    continue;
                }
                if (str.split(" ").length < 6) {
                    continue;
                }
                sentences.add(str);
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return sentences;
    }
    
    
    
    
    public static void main(String args[]){
//        extractEntitiesFromSingleSent();
        List<String> testSents = generateTestingSentence("Zhu 2012.pdf");
        extractEntitiesFromMultiSent(testSents);
    }

}

