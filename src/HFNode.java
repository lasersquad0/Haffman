
class HFNode implements Comparable<HFNode>
{
	long weight;
	int symbol; // в реальности это один байт(0..255) но ввиду его знаковости делаем int что бы избежать проблем с отриц значениями
	HFNode rightNode;
	HFNode leftNode;

	public HFNode(long w, int c)
	{
		weight = w;
		symbol = c;
	}

	public HFNode(HFNode left, HFNode right, long w, int c)
	{
		leftNode = left;
		rightNode = right;
		weight = w;
		symbol = c;
	}

	@Override
	public int compareTo(HFNode o)
	{
		return Long.compare(this.weight, o.weight);
	} // сортировка по возрастанию весов
}



class HFCode implements Comparable<HFCode>
{
	int symbol; // в реальности это один байт(0..255) но ввиду его знаковости делаем int что бы избежать проблем с отриц значениями
	int code;
	byte len;
	String scode;

	public HFCode(int s, int c, byte l, String sc)
	{
		symbol = s;
		code = c;
		len = l;
		scode = sc;
	}
	@Override
	public int compareTo(HFCode o) // Сортируем по возрастанию длинн. так более частые коды будут быстрее искаться при раскодировании
	{
		return Integer.compare(this.len, o.len);
	}

	@Override
	public String toString()
	{
		return String.format("(s=%d,c=%d,sc=%s,l=%d)", symbol,code,scode,len);
	}
}
