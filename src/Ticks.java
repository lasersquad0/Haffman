import java.util.HashMap;

public class Ticks
{
	private static final HashMap<String,Long> s = new HashMap<>();
	private static final HashMap<String,Long> f = new HashMap<>();

	public static void start(String tickName)
	{
		s.put(tickName, System.nanoTime());
	}
	public static long finish(String tickName)
	{
		f.put(tickName, System.nanoTime());
		return getTick(tickName);
	}
	public static long getTick(String tickName)
	{
		return f.getOrDefault(tickName, System.nanoTime()) - s.getOrDefault(tickName, 0L);
	}
}
