package autoGrind.tellMeWhen;

import java.util.ArrayList;

/**
 * Created by Administrateur on 19/05/15!
 */
public class TMW {
    
    private final ArrayList<Key> alKeys = new ArrayList<>();
    
    void add(Key key) {
        
        alKeys.add(key);
    }
    
    public ArrayList<Key> getKeys() {
        
        return alKeys;
    }
}

