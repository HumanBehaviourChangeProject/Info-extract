/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.wvec;

import org.apache.lucene.util.BytesRef;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Provides compress/decompress capabilities for a String.
 * @author Debasis
 */
public class CompressionUtils {

    public static BytesRef compress(String str) {
        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(out);
            gzip.write(str.getBytes());
            gzip.close();
        }
        catch (Exception ex) {
            return new BytesRef("");
        }
        return out==null? new BytesRef("") : new BytesRef(out.toByteArray());
    }
    
    public static String decompress(byte[] bytes) {
        try {
            InputStream in = new GZIPInputStream(new ByteArrayInputStream(bytes));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[262144];  // about 300kb
            int len;
            while((len = in.read(buffer))>0)
                baos.write(buffer, 0, len);
            return new String(baos.toByteArray(), "UTF-8");
        }
        catch (Exception e) {
            return "";
        }
    }    
}
