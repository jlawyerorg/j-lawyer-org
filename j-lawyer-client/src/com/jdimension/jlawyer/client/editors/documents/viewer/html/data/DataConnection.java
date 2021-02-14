/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jdimension.jlawyer.client.editors.documents.viewer.html.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author jens
 */
public class DataConnection extends URLConnection {

    public DataConnection(URL u) {
        super(u);
    }

    @Override
    public void connect() throws IOException {
        connected = true;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        String data = url.toString();
        data = data.replaceFirst("^.*;base64,", "");
        //System.out.println("Data: " + data);
        byte[] bytes=Base64.getDecoder().decode(data);
        return new ByteArrayInputStream(bytes);
    }

}
