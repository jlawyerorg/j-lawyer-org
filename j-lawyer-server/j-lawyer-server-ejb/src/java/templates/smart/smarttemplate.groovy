import templates.smart.SmartFunctions;

class SmartTemplate {
    
    String caseId="SMARTTEMPLATECASEID";
	
    double doIt(double d1) {
        System.out.println("ok");
        return 42d-d1;
    }
    
    String evaluateScript() {
        return SMARTTEMPLATESCRIPT;
    }
    
    String WENNETIKETT(String etikett, String then, String otherwise) {
        if(etikett==null)
            return otherwise;
        if("".equals(etikett))
            return otherwise;
        
        return SmartFunctions.wennEtikett(caseId, etikett, then, otherwise);
    }
    
    String WENNETIKETT(String etikett, String then) {
        return WENNETIKETT(etikett, then, "");
    }
    
    String WENNGLEICH(String s1, String s2, String then, String otherwise) {
        if(s1==null)
        s1="";
        if(s2==null)
        s2="";
        if(s1.equalsIgnoreCase(s2))
        return then;
        else
        return otherwise;
    }
    
    String WENNGLEICH(String s1, String s2, String then) {
        return WENNGLEICH(s1, s2, then, "");
    }
    
    String WENNENTHAELT(String s1, String s2, String then, String otherwise) {
        if(s1==null)
        s1="";
        if(s2==null)
        s2="";
        if(s1.toLowerCase().contains(s2.toLowerCase()))
        return then;
        else
        return otherwise;
    }
    
    String WENNENTHAELT(String s1, String s2, String then) {
        return WENNENTHAELT(s1, s2, then, "");
    }
    
    String WENNLEER(String s1, String then, String otherwise) {
        if(s1==null)
        s1="";
        if(s1.isEmpty())
        return then;
        else
        return otherwise;
    }
    
    String WENNLEER(String s1, String then) {
        return WENNLEER(s1, then, "");
    }
    
    String WENNFALLDATEN(String s1, String then) {
        return WENNFALLDATEN(s1, then, "");
    }
    
    String WENNFALLDATEN(String s1, String then, String otherwise) {
        // this variable value will be replaced by LibreOfficeAccess before execution
        String allFormPrefixes="ALLFORMPREFIXES";
        if(allFormPrefixes.contains("-" + s1 + "-")) {
            return then;
        } else {
            return otherwise;
        }
    }
    
    String WENNGROESSER(String s1, String s2, String then, String otherwise) {
        double d1=0d;
        double d2=0d;
        if(s1==null)
        s1="0";
        if(s2==null)
        s2="0";
        if(s1.isEmpty())
        s1="0";
        if(s2.isEmpty())
        s2="0";
            
        s1=s1.replace(".", "");
        s2=s2.replace(".", "");
        s1=s1.replace(",", ".");
        s2=s2.replace(",", ".");
    
        try {
            d1=Double.parseDouble(s1);
        } catch (Throwable t) {
            
        }
        try {
            d2=Double.parseDouble(s2);
        } catch (Throwable t) {
            
        }
        if(d1>d2)
        return then;
        else
        return otherwise;
    }
    
    String WENNGROESSER(String s1, String s2, String then) {
        return WENNGROESSER(s1, s2, then, "");
    }
    
    String TEXT(String s1) {
        if(s1==null)
            s1="";
        return s1;
    }
    
    /*
    * E.g. when string is taken from a mapping table and the term is used at the beginning of a sentence.
    */
    String GROSS(String s1) {
        if(s1==null)
            s1="";
            
        if(s1.length()>0)
            s1=s1.substring(0, 1).toUpperCase() + s1.substring(1);
            
        return s1;
    }
    
    /*
    * E.g. when string is taken from a mapping table and the term is used within a sentence.
    */
    String KLEIN(String s1) {
        if(s1==null)
            s1="";
            
        if(s1.length()>0)
            s1=s1.substring(0, 1).toLowerCase() + s1.substring(1);
            
        return s1;
    }
    
    String FRIST(String fromDate, String addDays) {
        return FRIST(fromDate, addDays, null);
    }
    
    String FRIST(String fromDate, String addDays, String format) {
        if(fromDate==null || "".equals(fromDate))
            return "??.??.????";
        
        java.text.SimpleDateFormat df=new java.text.SimpleDateFormat("dd.MM.yyyy");
        try {
            java.util.Date testDate = df.parse(fromDate)
            java.util.Calendar cal = java.util.Calendar.getInstance(); 
            cal.setTime(testDate);
            if(cal.get(Calendar.YEAR) < 2000)
                cal.add(java.util.Calendar.YEAR, 2000);
            cal.add(java.util.Calendar.DAY_OF_MONTH, Integer.parseInt(addDays));
            testDate = cal.getTime();
            if(format==null) {
                return df.format(testDate);
            } else {
                return new java.text.SimpleDateFormat(format).format(testDate);
            }
        } catch (Throwable t) {
            return "??.??.????"
        }
        
    }
    
    String FRISTBANKTAG(String fromDate, String addDays) {
        return FRISTBANKTAG(fromDate, addDays, null);
    }
    
    String FRISTBANKTAG(String fromDate, String addDays, String format) {
        if(fromDate==null || "".equals(fromDate))
            return "??.??.????";
        
        java.text.SimpleDateFormat df=new java.text.SimpleDateFormat("dd.MM.yyyy");
        try {
            java.util.Date testDate = df.parse(fromDate)
            java.util.Calendar cal = java.util.Calendar.getInstance(); 
            cal.setTime(testDate);
            if(cal.get(Calendar.YEAR) < 2000)
                cal.add(java.util.Calendar.YEAR, 2000);
            cal.add(java.util.Calendar.DAY_OF_MONTH, Integer.parseInt(addDays));
            while(cal.get(java.util.Calendar.DAY_OF_WEEK)==java.util.Calendar.SATURDAY || cal.get(java.util.Calendar.DAY_OF_WEEK)==java.util.Calendar.SUNDAY) {
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
            }
            testDate = cal.getTime();
            if(format==null) {
                return df.format(testDate);
            } else {
                return new java.text.SimpleDateFormat(format).format(testDate);
            }
        } catch (Throwable t) {
            return "??.??.????"
        }
        
    }
    
    String MWDJU(String gender, String male, String female, String other, String org, String undef) {
        if(gender.endsWith("nnlich")) {
            // avoid umlaut issues
            return male;
        } else if("weiblich".equalsIgnoreCase(gender)) {
            return female;
        } else if("divers".equalsIgnoreCase(gender)) {
            return other;
        } else if("juristische Person".equalsIgnoreCase(gender)) {
            return org;
        } else if("undefiniert".equalsIgnoreCase(gender)) {
            return undef;
        }
        return "UNBEKANNTER WERT FUER GESCHLECHT: " + gender;
    }
    
    String GENDERN(String word, String gender, String inCase) {
        return SmartFunctions.gendern(word, gender, inCase);
    }
    
    String ZUORDNEN(String table, String key1, String key2, String key3) {
        return SmartFunctions.zuordnen(table, key1, key2, key3);
    }
    
    String ZUORDNEN(String table, String key1, String key2) {
        return SmartFunctions.zuordnen(table, key1, key2);
    }
    
    String ZUORDNEN(String table, String key1) {
        return SmartFunctions.zuordnen(table, key1);
    }
    
    String DATUMZEIT(String format) {
        java.text.SimpleDateFormat df=new java.text.SimpleDateFormat(format);
        return df.format(new java.util.Date());
    }
    
}

