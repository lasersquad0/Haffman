import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

public class BWTCoder
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private final int ALFABET_SIZE = 256;
	private final int[] cumFreqs = new int[ALFABET_SIZE]; // always fixed size (small) array
	private int[] index; // blockSize size array
	private int[] TT; // blockSize size array


	public void encodeBlock(BlockCoderData data)
	{
		if(index == null)
			index = new int[data.srcblock.length];
		if(index.length < data.srcblock.length) // re-create index array if it has less size than iblock.length
			index = new int[data.srcblock.length];

		// инициализируем индекс сортировки
		for (int i = 0; i < data.bytesInBlock; i++)
		{
			index[i] = i;
		}

		//QuickSort.SORT_FROM_RIGHT_TO_LEFT = true;
		QuickSort.bwtsort(data.srcblock, index, data.bytesInBlock,0, data.bytesInBlock - 1, 0, 1);

		//checkSorting(data); // !! ЗАКОМЕНТАРИТЬ ПОТОМ

		// применяем сортировку по сформированному индексу к последней колонке таблицы сдвигов
		// игра с индексами - так как последняя колонка это есть начальный блок, но сдвинутый на 1 байт, и последний байт помещен на позицию 0
		// в целом можно избавится от массива index если при сортировке сравнивать байты в iblock, а переставлять местами байты в bwtblock предварительно закинув туда последний столбец
		for (int i = 0; i < data.bytesInBlock; i++)
		{
			if (index[i] > 0)
				data.destblock[i] = data.srcblock[index[i] - 1];
			else
			{
				data.destblock[i] = data.srcblock[data.bytesInBlock - 1]; // Последняя колонка после всех сдвигов это начальный блок, но сдвинутый вниз на 1 байт т.е. в нулевом индексе у нас лежит последний байт из оригинального блока
				data.bwtLineNum = i;
			}
		}

		//if(!Utils.compareBlocks(data.srcblock, data.destblock))
		//	logger.finer("[encodeBlock] Blocks are not equal after apply index!");


		if(QuickSort.maxDDepth > 100)
			logger.finer(()->String.format("Max compare depth: %d", QuickSort.maxDDepth));
		if(QuickSort.maxRecursion > 100)
			logger.finer(()->String.format("Max recursion: %d", QuickSort.maxRecursion));
		logger.finer(()->String.format("lineNum: %d", data.bwtLineNum));
	}


	public void decodeBlock(BlockCoderData data)
	{
		if(TT == null)
			TT = new int[data.srcblock.length];
		if(TT.length < data.srcblock.length) // re-create index array if it has less size than iblock.length
			TT = new int[data.srcblock.length];

		// cumFreq инициализируется в конструкторе потому что он не большой по размеру
		// заполняем частотами встречающихся символов
		Arrays.fill(cumFreqs,0);
		for (int i = 0; i < data.bytesInBlock; i++)
		{
			byte b = data.srcblock[i];
			cumFreqs[Byte.toUnsignedInt(b)]++;
		}

		// заполняем кумулятивные частоты
		// теперь cumFreqs[i] указывает на первую позицию символа i в первом столбце (отсортированном столбце)
		int sum = 0;
		for (int i = 0; i < cumFreqs.length; i++)
		{
			sum += cumFreqs[i];
			cumFreqs[i] = sum - cumFreqs[i];
		}

		//System.out.println("Cumulative counts:" + Arrays.toString(counts));
		//checkSortedBlock(data);


		// вектор обратного преобразования.
		Arrays.fill(TT, 0); // may be not needed, just in case.
		for (int i = 0; i < data.bytesInBlock; i++)
		{
			TT[cumFreqs[Byte.toUnsignedInt(data.srcblock[i])]++] = i;
		}

		// формируем BWT преобразованный назад блок
		int j = TT[data.bwtLineNum];
		for (int i = 0; i < data.bytesInBlock; i++)
		{
			data.destblock[i] = data.srcblock[j];
			j = TT[j];
		}

		//System.out.println("BWT Decoded block:" + Arrays.toString(fblock));
		//System.out.println("Back index:" + Arrays.toString(TT));

		//assert Arrays.compare(data.iblock, oblock) == 0: "Blocked are not equal!!!";
	}

	private void checkSortedBlock(BlockCoderData data)
	{
		// для проверки: формируем отсортированный блок
		byte[] sblock = new byte[data.bytesInBlock];
		for (int i = 0; i < cumFreqs.length - 1; i++)
		{
			Arrays.fill(sblock, cumFreqs[i], cumFreqs[i + 1], (byte) i);
		}
		Arrays.fill(sblock, cumFreqs[cumFreqs.length - 1], sblock.length, (byte) 255);

		//System.out.println("Sorted block:" + Arrays.toString(sblock));
		if(!Utils.compareBlocks(Arrays.copyOf(data.destblock, data.bytesInBlock), sblock))
		{
			logger.finer("Blocks iblock and sblock are not equal!");
		}
	}

	private void checkSorting(BlockCoderData data)
	{
		// для проверки: применяем сортировку к блоку с использованием index
		byte[] sblock = new byte[data.bytesInBlock];
		for (int i = 0;	i < data.bytesInBlock; i++)
			sblock[i] = data.srcblock[index[i]];

		if(!Utils.compareBlocks(Arrays.copyOf(data.srcblock, data.bytesInBlock), sblock))
		{
			logger.finer("[checkSorting] Blocks srcblock and sblock are not equal!");
		}
	}

}

class BWTTask implements Runnable
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private static int seqB = 0;
	private static int seqR = 0;
	private final String name;
	BlockCoderData data;
	TransformSeq transformer;

	private synchronized int incSeqB()
	{
		return seqB++;
	}

	private synchronized int incSeqR()
	{
		return seqR++;
	}

	BWTTask()
	{
		data = new BlockCoderData();
		data.allocate(Utils.BLOCK_SIZE);
		transformer = new TransformSeq();
		name = "BWTTask" + incSeqB();
	}

	@Override
	public void run()
	{
		int num = incSeqR();
		logger.finer(name + "(#" + num + ")" + " task has started");

		transformer.doTransformEncode(data);

		logger.finer(name + "(#" + num + ")" + " task has finished");
	}
}

class TransformSeq
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private final byte FLAG_RLE1 = 0x08;
	private final byte FLAG_BWT  = 0x04;
	private final byte FLAG_MTF  = 0x02;
	private final byte FLAG_RLE2 = 0x01;
	private final BWTCoder bwt;
	private final MTFCoder mtf;
	private final RLECoder rle;

	TransformSeq()
	{
		bwt = new BWTCoder();
		mtf = new MTFCoder();
		rle = new RLECoder();
	}

	public void doTransformEncode(BlockCoderData data)
	{

		try
		{
			data.bflags = 0;

			if(rle.encodeBlock(data)) // if encoded data is going to increase after RLE then cancel it (stop RLE encoding and forget results)
			{
				data.bflags |= FLAG_RLE1;
				data.swapBuffers();
			}
			else
			{
				logger.finer("RLE1 transform is CANCELLED because it increased block size.");
			}

			bwt.encodeBlock(data);
			data.swapBuffers();
			data.bflags |= FLAG_BWT;

			mtf.encodeBlock(data);
			data.swapBuffers();
			data.bflags |= FLAG_MTF;

			if(rle.encodeBlock(data)) // the same. if data going to increase after RLE - do not use RLE for this particular block
			{
				data.bflags |= FLAG_RLE2;
				data.swapBuffers();
			}
			else
			{
				logger.finer("RLE2 transform is CANCELLED because it increased block size.");
			}

		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void doTransformDecode(BlockCoderData data)
	{
		if ((data.bflags & FLAG_RLE2) == FLAG_RLE2)
		{
			rle.decodeBlock(data);
			data.swapBuffers();
		}

		if ((data.bflags & FLAG_MTF) == FLAG_MTF)
		{
			mtf.decodeBlock(data);
			data.swapBuffers();
		}

		if ((data.bflags & FLAG_BWT) == FLAG_BWT)
		{
			bwt.decodeBlock(data);
			data.swapBuffers();
		}

		if ((data.bflags & FLAG_RLE1) == FLAG_RLE1)
		{
			rle.decodeBlock(data);
			data.swapBuffers();
		}
	}

}

