package utilities;

public class Holder<T> 
{
	private T item;
	
	private int hashcode;
	
	public Holder(T item)
	{
		this.item = item;
		this.hashcode = item.hashCode();
	}
	
	public void setItem(T item)
	{
		this.item = item;
	}
	
	public T getItem()
	{
		return this.item;
	}
	
	/*
	 * Object Overrides
	 */
	
	@Override
	public int hashCode() 
	{
		return this.hashcode;
	}
	
	@Override
	public boolean equals(Object obj) 
	{
		if (obj instanceof Holder)
		{
			return super.equals(obj);
		}
		
		Holder<?> cast = (Holder<?>)obj;
		
		return this.item.equals(cast.item);
	}
}
