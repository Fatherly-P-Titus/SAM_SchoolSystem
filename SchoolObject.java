package SchoolManager;

public class SchoolObject {
    
    private String load_msg;
    private boolean load_stat;
    
    
    public void setLoadState(boolean stat,String msg){
        this.load_stat = stat;
        this.load_msg = msg;
    }
    
    public void set_load_msg (String msg){
        this.load_msg = msg;
    }
    
    public String get_load_msg(){
        return this.load_msg;
    }
    
    public boolean get_load_stat(){
        return this.load_stat;
    }
    
    public String infoString(){
        return "INFO STRING";
    }
}