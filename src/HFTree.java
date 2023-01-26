import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class HFTree
{
	private final static Logger logger = Logger.getLogger("HFLogger");
	protected  int[] symbols; // all symbols that are used in input file
	protected int[] weights;  //  weights of symbols above
	protected HFNode treeRoot;
	ArrayList<HFCode> codesList = new ArrayList<>();
	HashMap<Integer,HFCode> codesMap = new HashMap<>(); // индекс - элемент из symbols, значение - код из дерева
	int minCodeLen = Integer.MAX_VALUE; // TODO Кандидат на выброс потому что нигде в алгоритмах сейчас не используется. Ранее использовалось в декодировании
	int maxCodeLen;     // TODO Кандидат на выброс потому что нигде в алгоритмах сейчас не используется. Ранее использовалось в декодировании
	private final InputStream sin;
	private final boolean externalStream;
	private final int tableRecSize = 6; // размер одной записи таблицы кодов хаффмана (6=byte+int+byte)
	long CRC32Value;  // высчитывается в вызове calcWeights() вместе с весами

	public HFTree(InputStream in)
	{
		sin = in;
		externalStream = true;
	}

	public void build() throws IOException
	{
		calcWeights();
		buildTree();
		calcCodes();
	}

	/**
	 * Читает весь файл и считает частоу встречаемости байтов в нем
	 * Для binary files считает частоты тоже корректно.
	 * *** Внимание - портит InputStream sin, передвигает его на конец файла *****
	 * @throws IOException если что-то произошло с потоком
	 */
	protected void calcWeights() throws IOException
	{
		logger.entering(this.getClass().getName(),"calcWeights");

		int BUF_SIZE = 100_000_000;  // если дали поток, то мы не знаем размер файла читаем кусками по BUF_SIZE (100М) тогда

		int[] freq = new int[256];
		byte[] buffer = new byte[BUF_SIZE];
		CRC32 cc = new CRC32();

		int cntRead;

		do
		{
			cntRead = sin.read(buffer);
			if (cntRead == -1) break;

			cc.update(buffer, 0, cntRead); // параллельно считаем CRC32 файла

			for (int i = 0; i < cntRead; i++)
				freq[Byte.toUnsignedInt(buffer[i])]++; // обходим знаковость байта

		}while(cntRead == BUF_SIZE);

		CRC32Value = cc.getValue(); // сохраняем значение CRC32 для последующего использования

		long sum = 0;
		long min = Long.MAX_VALUE, max = 0;
		int nonzero = 0;
		for (long l : freq)
		{
			sum += l;
			if(l > 0) nonzero++;
			if(l > 0) min = Math.min(min, l);
			max = Math.max(max, l);
		}

		symbols = new int[nonzero];
		weights = new int[nonzero];

		int j = 0;
		for (int i = 0; i < freq.length; i++)
		{
			if(freq[i] > 0)
			{
				symbols[j] = i;
				weights[j] = freq[i];
				j++;
			}
		}

		if(!externalStream) sin.close();

		logger.fine(String.format("symbols=%s", Arrays.toString(symbols)));
		logger.fine(String.format("weights=%s",Arrays.toString(weights)));
		logger.fine(String.format("min weight=%d, max weight=%d", min, max));
		logger.fine(String.format("nonzero=%d, sum(size)=%d", nonzero, sum));

		logger.exiting(this.getClass().getName(),"calcWeights");
	}

	protected void buildTree()
	{
		logger.entering(this.getClass().getName(),"buildTree");

		assert symbols != null;
		assert weights !=null;
		assert weights.length > 0;
		assert symbols.length > 0;

		var nodes = new ArrayList<HFNode>();
		for (int i = 0; i < symbols.length; i++)
		{
			nodes.add(new HFNode(weights[i], (char)symbols[i]));
		}

		if(nodes.size() > 1) // бывают случаи когда файл заполнен одним символом тогда дерево будет состоять из одного узла.
		{
			//Comparator<HFNode> comparator = (o1, o2) -> Integer.compare(o1.weight, o2.weight);
			nodes.sort(null);

			do
			{
				HFNode left = nodes.remove(0);
				HFNode right = nodes.remove(0);
				HFNode joint = new HFNode(left, right, left.weight + right.weight, left.symbol); //для НЕ листьев symbol не используется и не имеет значения

				boolean added = false;
				for (int i = 0; i < nodes.size(); i++)  // TODO можно оптимизировать если использовать Collections.binarySearch(nodes,joint);
				{
					if (nodes.get(i).weight >= joint.weight)
					{
						nodes.add(i, joint);
						added = true;
						break;
					}
				}
				if (!added)
					nodes.add(nodes.size(), joint);

			} while (nodes.size() != 1);
		}

		treeRoot = nodes.get(0);

		logger.exiting(this.getClass().getName(),"buildTree");
	}

	protected void calcCodes()
	{
		logger.entering(this.getClass().getName(),"calcCodes");

		if((treeRoot.leftNode == null) && (treeRoot.rightNode == null)) // we have only one node in the tree
		{
			calcCodesRecc(treeRoot, "0");
		}
		else
		{
			assert treeRoot.leftNode != null;
			calcCodesRecc(treeRoot.leftNode, "0");
			calcCodesRecc(treeRoot.rightNode, "1");
			codesList.sort(null); // важно чтобы коды были отсортированы от более короткого к более длиному что бы поиск кодов был быстрее при uncompress
		}


		logger.fine(String.format("min codelen=%d, max codelen=%d", minCodeLen, maxCodeLen));
		logger.fine(String.format("codes size=%d %s", codesList.size(), codesList));

		logger.exiting(this.getClass().getName(),"calcCodes");
	}

	private void calcCodesRecc(HFNode nd, String code)
	{
		if(nd.leftNode != null)
			calcCodesRecc(nd.leftNode, code + "0");
		if(nd.rightNode != null)
			calcCodesRecc(nd.rightNode, code + "1");

		if((nd.leftNode == null) && (nd.rightNode == null))
		{
			int cd = Integer.valueOf(code,2);

			HFCode hfc = new HFCode(nd.symbol, cd, (byte)code.length(), code);
			codesMap.put(nd.symbol, hfc);

			codesList.add(hfc);

			maxCodeLen = Math.max(maxCodeLen, code.length());
			minCodeLen = Math.min(minCodeLen, code.length());
		}
	}

	public byte[] getTable()
	{
		byte[] buffer = new byte[tableRecSize* codesList.size()];  // byte + int + byte = 6

		for (int i = 0; i < codesList.size(); i++)
		{
			HFCode hfc = codesList.get(i);
			int ch = hfc.symbol;
			int code = hfc.code;
			byte len = hfc.len;
			int index = tableRecSize*i;  // TODO можно заменить инкрементом index на tableRecSize
			buffer[index] = (byte)ch;
			buffer[index + 1] = (byte)(code >>> 24);
			buffer[index + 2] = (byte)(code >>> 16);
			buffer[index + 3] = (byte)(code >>> 8);
			buffer[index + 4] = (byte)code;
			buffer[index + 5] = len;
		}

		return buffer;
	}

	public void loadTable(int tableSize) throws IOException
	{
		var dis = new DataInputStream(sin);

		while(tableSize > 0)
		{
			int ch = dis.readByte();
			int code = dis.readInt();
			byte len = dis.readByte();

			HFCode hfc = new HFCode(ch, code, len, "");
			codesMap.put(ch, hfc);

			codesList.add(hfc);

			maxCodeLen = Math.max(maxCodeLen, len);
			minCodeLen = Math.min(minCodeLen, len);

			tableSize -= tableRecSize;
		}

		codesList.sort(null);

		logger.fine(String.format("codes size=%d %s", codesList.size(), codesList));
		logger.fine(String.format("min codelen=%d, max codelen=%d", minCodeLen, maxCodeLen));
	}

}
