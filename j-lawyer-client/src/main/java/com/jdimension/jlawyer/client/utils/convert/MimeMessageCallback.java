package com.jdimension.jlawyer.client.utils.convert;

import javax.mail.Part;

@FunctionalInterface
public interface MimeMessageCallback {

    void walk(Part part, int level) throws Exception;

}