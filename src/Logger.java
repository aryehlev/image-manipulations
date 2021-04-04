package src;

public interface Logger {
	public void log(String s);
	
	public default void log(Object obj) {
		log(obj == null ? "null" : obj.toString());
	}
}
