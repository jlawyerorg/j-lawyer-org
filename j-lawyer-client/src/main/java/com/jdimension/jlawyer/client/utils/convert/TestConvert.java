/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.jdimension.jlawyer.client.utils.convert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 *
 * @author jens
 */
public class TestConvert {

    /**
     * @param args the command line arguments
     */
//    public static void main(String[] args) {
//        try {
//            //String html=MimeMessageParser.instance("/home/jens/Downloads/emlToPdfConverter-main/test1.eml").getMimeMessageObject().getHtmlBody();
//            String html=new ParserUtil("/home/jens/Downloads/emlToPdfConverter-main/test1.eml", true, true).convertToFullHtml(null);
//            FileWriter fw=new FileWriter("/home/jens/Downloads/emlToPdfConverter-main/test1.html");
//            fw.write(html);
//            fw.close();
//            
//            //html=MimeMessageParser.instance("/home/jens/Downloads/emlToPdfConverter-main/test2.eml").getMimeMessageObject().getHtmlBody();
//            html=new ParserUtil("/home/jens/Downloads/emlToPdfConverter-main/test2.eml", true, true).convertToFullHtml(null);
//            fw=new FileWriter("/home/jens/Downloads/emlToPdfConverter-main/test2.html");
//            fw.write(html);
//            fw.close();
//            
//            html=new ParserUtil("/home/jens/Downloads/emlToPdfConverter-main/test3.eml", true, true).convertToFullHtml(null);
//            fw=new FileWriter("/home/jens/Downloads/emlToPdfConverter-main/test3.html");
//            fw.write(html);
//            fw.close();
//            
//            html=new ParserUtil("/home/jens/Downloads/emlToPdfConverter-main/test4.eml", true, true).convertToFullHtml(null);
//            fw=new FileWriter("/home/jens/Downloads/emlToPdfConverter-main/test4.html");
//            fw.write(html);
//            fw.close();
//            
//            html=new ParserUtil("/home/jens/Downloads/emlToPdfConverter-main/test5.eml", true, true).convertToFullHtml(null);
//            fw=new FileWriter("/home/jens/Downloads/emlToPdfConverter-main/test5.html");
//            fw.write(html);
//            fw.close();
//            
//            html=new ParserUtil("/home/jens/Downloads/emlToPdfConverter-main/test6.eml", true, true).convertToFullHtml(null);
//            fw=new FileWriter("/home/jens/Downloads/emlToPdfConverter-main/test6.html");
//            fw.write(html);
//            fw.close();
//            
//            
//            //System.out.println(MimeMessageParser.instance("/home/jens/Downloads/emlToPdfConverter-main/test1.eml").);
//        } catch (Exception ex) {
//            Logger.getLogger(TestConvert.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    public static void main(String[] args) throws IOException {
        try (PDDocument document = PDDocument.load(new File("/home/jens/Downloads/Ermittlungsakte.pdf"))) {
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder markdownContent = new StringBuilder();

            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);

                // Detect headers, tables, and structure text into Markdown
                markdownContent.append(processPageContentAsMarkdown(text));
            }

            // Write the output to a Markdown file
            try (FileWriter writer = new FileWriter("/home/jens/temp/steinert.md")) {
                writer.write(markdownContent.toString());
            }
        }
    }

    private static String processPageContentAsMarkdown(String text) {
        StringBuilder md = new StringBuilder();

        // Example: Basic detection patterns (customize as needed)
        for (String line : text.split("\n")) {
            if (line.trim().isEmpty()) {
                continue; // Skip empty lines
            }
            // Example of handling headers
            if (line.matches("(?i)^chapter|^section")) {
                md.append("# ").append(line.trim()).append("\n\n");
            } // Detect tables by checking consistent column spacing (requires tuning)
            else if (line.contains(" | ")) {
                md.append(line.trim()).append("  \n");
            } // Regular paragraph text
            else {
                md.append(line.trim()).append("\n");
            }
        }
        return md.toString();
    }

}
