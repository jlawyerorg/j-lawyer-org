class SmartTemplate {
	
    double doIt(double d1) {
        System.out.println("ok");
        return 42d-d1;
    }
    
    String evaluateScript() {
        return SMARTTEMPLATESCRIPT;
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
    
}

