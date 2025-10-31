package simplf; 

import java.util.List;

interface SimplfCallable {

    Object call(Interpreter interpreter, List<Object> args);
    
    // Used by the Interpreter to check the number of arguments passed in a call.
    int arity(); 
}