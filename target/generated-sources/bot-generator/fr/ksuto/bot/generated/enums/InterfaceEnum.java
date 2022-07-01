package fr.ksuto.bot.generated.enums;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import fr.ksuto.prh.entities.AbstractPictureEnum;


public enum InterfaceEnum implements AbstractPictureEnum {
    
    TMW_ANCHOR("/picturesToEnum/interface/TMW_Anchor.png"),
    WOW("/picturesToEnum/interface/WoW.png"),
    WOW_LEGION("/picturesToEnum/interface/WoW_Legion.png"),
    WOW_VANILLA("/picturesToEnum/interface/WoW_vanilla.png");
    
    private final String url;
    
    InterfaceEnum(String url) {
        
        this.url = url;
    }
    
    public String getUrl() {
        
        return url;
    }
    
    public String getHash() {
        
        return url.replaceAll(".*/", "");
    }
    
    public String toString() {
        
        final String  regex   = "(^[a-z])|( [a-z])";
        final String  subst   = "\\U$1$2";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(getHash());
        return matcher.replaceAll(subst);
    }
}