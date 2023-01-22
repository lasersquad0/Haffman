
class HFNode implements Comparable<HFNode>
{
	int weight;
	int symbol; // в реальности это один байт(0..255) но ввиду его знаковости делаем int что бы избежать проблем с отриц значениями
	HFNode rightNode;
	HFNode leftNode;

	public HFNode(int w, int c)
	{
		weight = w;
		symbol = c;
	}

	public HFNode(HFNode left, HFNode right, int w, int c)
	{
		leftNode = left;
		rightNode = right;
		weight = w;
		symbol = c;
	}

	@Override
	public int compareTo(HFNode o)
	{
		return Integer.compare(this.weight, o.weight);
	}
}
