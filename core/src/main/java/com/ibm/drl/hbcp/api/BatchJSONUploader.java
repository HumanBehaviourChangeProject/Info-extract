/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.ibm.drl.hbcp.inforetrieval.indexer.ExtractedInfoRetriever;

/**
 * Used to export the extracted values in JSON format into a Cloudant database.
 * 
 * @author dganguly
 */
public class BatchJSONUploader {
    Properties prop;
    String baseURL;
    ExtractedInfoRetriever retriever;
    String insertURL;
    String resetURL;
    
    /**
     * Use the properties file to read from the specified extracted values index.
     * @param propFile
     * @throws Exception 
     */
    public BatchJSONUploader(String propFile) throws Exception {
        retriever = new ExtractedInfoRetriever(propFile);
        prop = retriever.getProperties();
        baseURL = "https://" +
                prop.getProperty("cloudant.auth") +
                "@" +
                prop.getProperty("cloudant.store") + "/service/";
    }
    
    /**
     * Send the formatted JSON to the Cloudant server specified
     * by the property values baseURL + the value of 'cloudant.dbname' property.
     * 
     * @throws Exception 
     */
    public void sendAll_DocAttrib_Rcds() throws Exception {
        insertURL = baseURL + "store/" + prop.getProperty("cloudant.dbname") + "/";
        System.out.println(insertURL);
        HttpClient httpclient = HttpClients.createDefault();
        
        List<String> jsons = retriever.exportRecordsAsJSONArray();
        for (String json: jsons) {
            System.out.println("Sent to '" + insertURL + "' json data: " + json);
            //send(httpclient, json);
        }
        System.out.println("Sent " + jsons.size() + " records");
    }
    
    /**
     * Send the formatted JSON to the Cloudant server specified
     * by the property values baseURL + the value of 'cloudant.dbname' property.
     * The values are grouped by documents.
     */
    public void sendAll_Doc_Rcds() {
        insertURL = baseURL + "store/" + prop.getProperty("cloudant.dbname") + "/";
        System.out.println(insertURL);
        
        try {
            HttpClient httpclient = HttpClients.createDefault();

            List<String> jsons = retriever.exportPerDocRecordsAsJSONArray();

            for (String json: jsons) {
                System.out.println("Sent to '" + insertURL + "' json data: " + json);
                send(httpclient, json);
            }
        }
        catch (Exception ex) {
            System.err.println("Error in sending JSON");
            System.exit(1);
        }
    }
    
    private void resetDatabase() throws Exception {
        resetURL = baseURL + "admin/storage/reset/" + prop.getProperty("cloudant.dbname") + "/";
        //HttpClient httpclient = HttpClients.createDefault();
        //HttpPost httppost = new HttpPost(resetURL);
        //HttpResponse response = httpclient.execute(httppost);
        //HttpEntity entity = response.getEntity();

        HttpClient client = new DefaultHttpClient();
        HttpPost request = new HttpPost();
        request.setURI(new URI(resetURL));
        HttpResponse response = client.execute(request);
        String msg = EntityUtils.toString(response.getEntity());
        System.out.println(msg);
    }
    
    /**
     * Send one particular record over HTTP.
     * 
     * @param httpclient
     * @param json
     * @throws UnsupportedEncodingException
     * @throws IOException 
     */
    public void send(HttpClient httpclient, String json) throws UnsupportedEncodingException, IOException {
    
        StringEntity requestEntity = new StringEntity(
            json,
            ContentType.APPLICATION_JSON);
        
        HttpPost httppost = new HttpPost(insertURL);
        httppost.setEntity(requestEntity);
        
        //Execute and get the response.
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            InputStream instream = entity.getContent();
            try {
                System.out.println(EntityUtils.toString(entity));
            } finally {
                instream.close();
            }
        }        
    }
    
    /**
     * Closes the HTTP connection.
     * @throws Exception 
     */
    public void close() throws Exception {
        retriever.close();
    }
    
    public static void main(String[] args) {
        
        if (args.length == 0) {
            System.err.println("Usage: java BatchJSONUploader <prop-file>");
            args = new String[1];
            args[0] = "init.properties";
        }
        
        try {
            BatchJSONUploader jsonUploader = new BatchJSONUploader(args[0]);
            //jsonUploader.resetDatabase();
            //jsonUploader.sendAll_DocAttrib_Rcds();
            jsonUploader.sendAll_Doc_Rcds();
            jsonUploader.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
