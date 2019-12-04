package apex.symbolic;

public class Register {

	
	public Expression value;
	public String name;
	public boolean isWideHigh, isWideLow;
	public Register next;
	
	public Register(String name)
	{
		this(name, null);
	}
	
	public Register(String name, Expression value)
	{
		this.name = name;
		this.value = value;
		isWideHigh = isWideLow = false;
	}
	
	public Register clone()
	{
		Register reg = new Register(name, value==null?null:value.clone());
		reg.next = next;
		reg.isWideHigh = isWideHigh;
		reg.isWideLow = isWideLow;
		return reg;
	}
}
