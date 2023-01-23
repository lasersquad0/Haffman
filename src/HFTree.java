import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

public class HFTree
{
	private int[] symbols; // all symbols that are used in input file
	private int[] weights;  //  weights of symbols above
	private HFNode treeRoot;
	ArrayList<HFCode> codesList = new ArrayList<>();
	//ArrayList<String> codesStrList = new ArrayList<>(); // просто список String кодов, номера индексов совпадают с номерами индексов codesList.
	HashMap<Integer,HFCode> codesMap = new HashMap<>(); // индекс - элемент из symbols, значение - код из дерева
	int minCodeLen = Integer.MAX_VALUE;
	int maxCodeLen;
	private final InputStream sin;
	private boolean externalStream;
//	private String FILENAME_FOR_WEIGHTS;
	private final int MAX_BUF_SIZE = 100_000_000;
	private final int tableRecSize = 6; // размер одной записи таблицы кодов хаффмана (6=byte+int+byte)
	long CRC32Value;  // высчитывается в вызове calcWeights() вместе с весами

	public HFTree(InputStream in)
	{
		sin = in;
		externalStream = true;
	}

/*	public HFTree(String filename)
	{
		FILENAME_FOR_WEIGHTS = filename;
		externalStream = false;
	}
*/
	public void build() throws IOException
	{
		calcWeights();
		buildTree();
		calcCodes();
	}

	/**
	 * Читает весь файл и считает часточу встречаемости байтов в нем
	 * Для binary files считает частоты тоже корректно.
	 * *** Внимание - портит InputStream sin, передвигает его на конец файла *****
	 * @throws IOException
	 */
	private void calcWeights() throws IOException
	{
		int FILE_BUFFER = MAX_BUF_SIZE; // если дали поток, то мы не знаем размер файла читаем кусками по MAX_BUF_SIZE (100М) тогда

/*		if(sin == null) // если не дали поток значит есть имя файла, создаем поток из имени файла
		{
			File inFile = new File(FILENAME_FOR_WEIGHTS);
			FILE_BUFFER = (inFile.length() < MAX_BUF_SIZE) ? (int) inFile.length() : MAX_BUF_SIZE;

			sin = new BufferedInputStream(new FileInputStream(inFile), FILE_BUFFER);
		}
*/
		int[] freq = new int[256];
		byte[] buffer = new byte[FILE_BUFFER];
		CRC32 cc = new CRC32();

		int cntRead;
		do
		{
			cntRead = sin.read(buffer);
			if (cntRead == -1) break;

			cc.update(buffer, 0, cntRead); // параллельно считаем CRC32 файла

			for (int i = 0; i < cntRead; i++)
				freq[Byte.toUnsignedInt(buffer[i])]++; // обходим знаковость байта

		}while(cntRead == FILE_BUFFER);

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

		System.out.println(Arrays.toString(symbols));
		System.out.println(Arrays.toString(weights));
		System.out.format("min=%d, max=%d\n", min, max);
		System.out.format("nonzero=%d, sum(size)=%d\n", nonzero, sum);
	}

	private void buildTree()
	{
		var nodes = new ArrayList<HFNode>();
		for (int i = 0; i < symbols.length; i++)
		{
			nodes.add(new HFNode(weights[i], (char)symbols[i]));
		}

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

		}while(nodes.size() != 1);

		treeRoot = nodes.get(0);
	}

	private void calcCodes()
	{
		calcCodesRecc(treeRoot.leftNode,"0");
		calcCodesRecc(treeRoot.rightNode,"1");

		codesList.sort(null); // важно чтобы коды были отсортированы от более короткого к более длиному что бы поиск кодов был быстрее при uncompress

		//System.out.format("symbols size=%d %s\n",symbolsList.size(), symbolsList.toString());
		//System.out.format("codes size=%d %s\n",codesList.size(), codesList.toString());
		//System.out.format("codeLen size=%d %s\n",codeLenList.size(), codeLenList.toString());
		System.out.format("min=%d, max=%d\n", minCodeLen, maxCodeLen);
		//System.out.format("codesStr size=%d %s\n",codesStrList.size(), codesStrList.toString());
		System.out.format("hfcodes size=%d %s\n", codesList.size(), codesList.toString());

/*
		for (var entry : codes.entrySet())
		{
			var key1 = entry.getKey();
			var val1 = entry.getValue();
		}
 */
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
			//symbolsList.add(nd.symbol);
			//codesList.add(cd);
			//codeLenList.add((byte)code.length());
			//codesStrList.add(code);

			HFCode hfc = new HFCode(nd.symbol, cd, (byte)code.length(), code);
			codesMap.put(nd.symbol, hfc);
			//codeLen.put(nd.symbol, (byte)code.length());  // длина кода в битах точно не больше 255 (один байт).

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
			int ch = hfc.symbol; //symbolsList.get(i);
			int code = hfc.code; //codesList.get(i);
			byte len = hfc.len; //codeLenList.get(i);
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
			//symbolsList.add(ch);
			//codesList.add(code);
			//codeLenList.add(len);

			HFCode hfc = new HFCode(ch, code, len, "");
			codesMap.put(ch, hfc);
			//codeLen.put(ch, len);
			codesList.add(hfc);

			maxCodeLen = Math.max(maxCodeLen, len);
			minCodeLen = Math.min(minCodeLen, len);

			tableSize -= tableRecSize;
		}

		codesList.sort(null);

	//	System.out.format("symbols size=%d %s\n",symbolsList.size(), symbolsList.toString());
	//	System.out.format("codes size=%d %s\n",codesList.size(), codesList.toString());
	//	System.out.format("codeLen size=%d %s\n",codeLenList.size(), codeLenList.toString());
		System.out.format("hfcodes size=%d %s\n", codesList.size(), codesList.toString());
		System.out.format("min=%d, max=%d\n", minCodeLen, maxCodeLen);
	}

}
