package apex.symbolic;

public abstract class VM_interface {

	public boolean crashed = false;
	public boolean shouldStop = false;
	
	public abstract APEXArray createNewArray(String type, String root, Expression length, String birth);
	
	public abstract APEXObject createNewObject(String type, String root, String birth, boolean symbolic);
	
}
