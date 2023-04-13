import java.io.*;
import java.util.logging.Logger;

public class ModelOrder0Fixed extends ModelOrder0
{
	//private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private int[] symbols;
	private long[] cumFreq;
	private int[] symbol_to_freq_index;


	@Override
	public long[] SymbolToFreqRange(int sym)
	{
		int index = symbol_to_freq_index[sym];
		forSym2Freq[0] = cumFreq[index - 1]; // this is 'left' of the range
		forSym2Freq[1] = cumFreq[index] - cumFreq[index - 1]; // this is freq of sym, NOT 'right' of the range

		return forSym2Freq;
	}

	@Override
	public long[] FreqToSymbolInfo(long cumFr)
	{
		int j = 0;
		while(cumFreq[j] <= cumFr) j++;

		forFreq2Sym[0] = symbols[j - 1];
		forFreq2Sym[1] = cumFreq[j - 1]; // =left
		forFreq2Sym[2] = weights[j - 1];

		return forFreq2Sym;
	}

	public void calcWeights(String filename) throws IOException
	{
		File fl = new File(filename);
		InputStream sin = new BufferedInputStream(new FileInputStream(filename), Utils.getOptimalBufferSize(fl.length()));
		calcWeights(sin);
		sin.close();
	}

	/**
	 * Читает весь файл из потока и считает частоты встречаемости байтов в нем
	 * Для binary files считает частоты тоже корректно.
	 * *** Внимание - портит InputStream sin, передвигает его на конец файла *****
	 * @throws IOException если что-то произошло с потоком
	 */
	public void calcWeights(InputStream s) throws IOException
	{
		long[] freq = new long[256];

		int ch;
		while( (ch = s.read()) != -1 )
		{
			freq[ch]++;
		}

		int nonzero = 0;
		for (long l : freq)
			if(l > 0) nonzero++;

		symbols = new int[nonzero];
		weights = new long[nonzero];

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

		// preparing data for compressor/uncompressor
		cumFreq = new long[weights.length + 1];
		cumFreq[0] = 0;
		for (int i = 0; i < weights.length; i++)
		{
			cumFreq[i+1] = cumFreq[i] + weights[i];
		}
		symbol_to_freq_index = new int[256];
		for (int i = 0; i < symbols.length; i++)
		{
			symbol_to_freq_index[symbols[i]] = i + 1;
		}

		totalFreq = cumFreq[cumFreq.length - 1];
	}

	/** Does nothing in Fixed model
	 * @param sym
	 */
	@Override
	public void updateStatistics(int sym)
	{
		/*weights[sym]++;
		totalFreq++;
		if (totalFreq > rescaleThreshold)
			rescale();*/
	}

	/**
	 *  Divides weights into halves and recalculates totalFreq.
	 *  WARNING: only totalFreq variable is updated! cumFreq array left unchanged.
	 */
	private void rescale()
	{
		totalFreq = 0;
		for (int i = 0; i < weights.length; i++)
			totalFreq += (weights[i] -= (weights[i] >> 1));
	}

	public void rescaleTo(long limit)
	{
		if (limit > totalFreq)
			return; // no scaling needed

		//long SummFreq;
		do
		{
			rescale();
			//SummFreq = 0;
			//for (int i = 0; i < weights.length; i++)
			//{
			//	weights[i] -= (weights[i] >> 1); // this formula guarantees that cumFre[i] will not become zero.
			//	SummFreq += weights[i];
			//}
		}while(totalFreq > limit);

		// updating cumFreq
		cumFreq[0] = 0;
		for (int i = 0; i < weights.length; i++)
		{
			cumFreq[i + 1] = cumFreq[i] + weights[i];
		}
	}

	public void loadFreqs(InputStream in) throws IOException
	{
		var ds = new DataInputStream(in);

		int count = Byte.toUnsignedInt(ds.readByte()) + 1; // stored is an INDEX of last element in symbols to fit it in byte, that is why we are adding 1
		symbols = new int[count];
		cumFreq = new long[count + 1];
		weights = new long[count];

		for (int i = 0; i < count; i++)
		{
			symbols[i] = ds.readByte();
		}

		int type = ds.readByte(); // reading type of elements the following cumFreq array

		for (int i = 0; i < count + 1; i++)
		{
			switch (type)
			{
				case 1 -> cumFreq[i] = ds.readLong();
				case 2 -> cumFreq[i] = (long)ds.readInt();
				case 3 -> cumFreq[i] = (long)ds.readByte();
			}
		}


		totalFreq = cumFreq[cumFreq.length - 1];

		//restoring weights from cumFreq
		for (int i = 0; i < weights.length; i++)
		{
			weights[i] = cumFreq[i + 1] - cumFreq[i];
		}
	}

	public void saveFreqs(OutputStream out) throws IOException
	{
		var ds = new DataOutputStream(out);

		ds.writeByte(symbols.length - 1); // saving INDEX of the last element in symbols because we cannot not have more than 256 symbols there
		for (int sym : symbols)
		{
			ds.writeByte(sym);
		}

		int type = getFreqsType();

		ds.writeByte(type); // saving a type of elements in the next array (either byte, int or long)

		for (long l : cumFreq)
		{
			switch (type)
			{
				case 1 -> ds.writeLong(l);
				case 2 -> ds.writeInt((int) l);
				case 3 -> ds.writeByte((byte) l);
			}
		}
	}

	private int getFreqsType()
	{
		long max = 0;
		for (long l : cumFreq)
			if (max < l) max = l;

		if(max <= Byte.MAX_VALUE)
		{
			return 3; //saving as byte values
		}
		if(max < Integer.MAX_VALUE)
		{
			return 2; // saving as int values
		}
		return 1; // saving as long values
	}

	/*
	int decodeSymbol()
	{
		int sym;
		long HiCount, count = rc.getFreq(SummFreq);

		for (HiCount = sym = 0; ; sym++)
		{
			HiCount += Freq[sym];
			if (HiCount > count)
				break;
		}

		rc.Decode(HiCount - Freq[sym], Freq[sym], SummFreq);

		updateStatistics(sym);

		return sym;
	}


	private void encodeSymbol(int sym)
		{
			long cumFreq = 0;
			int i = 0;

			while(i < sym)
			{ //for (int i = 0; i < sym; i++)
				cumFreq += weights[i];
				i++;
			}

			//rc.Encode(LowCount, Freq[i], SummFreq);

			updateStatistics(sym);
		}
	*/

}
