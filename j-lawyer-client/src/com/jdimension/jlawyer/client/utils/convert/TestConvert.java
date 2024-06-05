/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.jdimension.jlawyer.client.utils.convert;

import java.io.FileWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jens
 */
public class TestConvert {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            //String html=MimeMessageParser.instance("/home/jens/Downloads/emlToPdfConverter-main/test1.eml").getMimeMessageObject().getHtmlBody();
            String html=new ParserUtil("/home/jens/Downloads/emlToPdfConverter-main/test1.eml", true, true).convertToFullHtml(null, null);
            FileWriter fw=new FileWriter("/home/jens/Downloads/emlToPdfConverter-main/test1.html");
            fw.write(html);
            fw.close();
            
            //html=MimeMessageParser.instance("/home/jens/Downloads/emlToPdfConverter-main/test2.eml").getMimeMessageObject().getHtmlBody();
            html=new ParserUtil("/home/jens/Downloads/emlToPdfConverter-main/test2.eml", true, true).convertToFullHtml(null, null);
            fw=new FileWriter("/home/jens/Downloads/emlToPdfConverter-main/test2.html");
            fw.write(html);
            fw.close();
            
            html=new ParserUtil("/home/jens/Downloads/emlToPdfConverter-main/test3.eml", true, true).convertToFullHtml(null, null);
            fw=new FileWriter("/home/jens/Downloads/emlToPdfConverter-main/test3.html");
            fw.write(html);
            fw.close();
            
            html=new ParserUtil("/home/jens/Downloads/emlToPdfConverter-main/test4.eml", true, true).convertToFullHtml(null, null);
            fw=new FileWriter("/home/jens/Downloads/emlToPdfConverter-main/test4.html");
            fw.write(html);
            fw.close();
            
            html=new ParserUtil("/home/jens/Downloads/emlToPdfConverter-main/test5.eml", true, true).convertToFullHtml(null, null);
            fw=new FileWriter("/home/jens/Downloads/emlToPdfConverter-main/test5.html");
            fw.write(html);
            fw.close();
            
            html=new ParserUtil("/home/jens/Downloads/emlToPdfConverter-main/test6.eml", true, true).convertToFullHtml(null, null);
            fw=new FileWriter("/home/jens/Downloads/emlToPdfConverter-main/test6.html");
            fw.write(html);
            fw.close();
            
            
            //System.out.println(MimeMessageParser.instance("/home/jens/Downloads/emlToPdfConverter-main/test1.eml").);
        } catch (Exception ex) {
            Logger.getLogger(TestConvert.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
