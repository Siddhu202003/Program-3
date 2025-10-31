package simplf; 

/**
 * A simple element structure for an associative list.
 * Note: A HashMap is used internally by Environment.java for actual map functionality.
 */
public class AssocList {
    
    final String name;
    Object value;
    final AssocList next;
    
    // Constructor matching the original file content
    public AssocList(String nameIn, Object valueIn, AssocList nextIn) {
        name = nameIn;
        value = valueIn;
        this.next = nextIn;
    }
    
    // Default constructor added for initialization flexibility
    public AssocList() {
        this.name = null;
        this.value = null;
        this.next = null;
    }
}