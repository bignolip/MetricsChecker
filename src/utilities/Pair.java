package utilities;

public class Pair<K, V> 
{
	private K key;
	
	private V value;
	
	private int hashcode;
	
	private String stringRep;
	
	/*
	 * Constructors
	 */
	public Pair(K key, V value)
	{
		this.key = key;
		
		this.value = value;
		
		this.hashcode = this.key.hashCode();
		
		this.stringRep = "(" + this.key.toString() + ", " + this.value.toString() + ")";
	}
	
	/*
	 * Accessors
	 */
	public K getKey()
	{
		return this.key;
	}
	
	public V getValue()
	{
		return this.value;
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
		if (obj instanceof Pair)
		{
			Pair<?, ?> cast = (Pair<?, ?>)obj;
			
			return this.key.equals(cast.getKey()) 
					&& this.value.equals(cast.getValue());
		}
		
		return false;
	}
	
	@Override
	public String toString() 
	{
		return this.stringRep;
	}
}