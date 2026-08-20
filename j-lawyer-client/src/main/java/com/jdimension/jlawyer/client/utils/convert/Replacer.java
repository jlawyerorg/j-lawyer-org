package com.jdimension.jlawyer.client.utils.convert;

import java.util.regex.Matcher;

@FunctionalInterface
public interface Replacer {

    String replace(Matcher match) throws Exception;

}
