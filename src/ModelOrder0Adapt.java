import java.util.logging.Logger;

public class ModelOrder0Adapt extends ModelOrder0
{
	//private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private final long rescaleThreshold;

	ModelOrder0Adapt(long threshold)
	{
		rescaleThreshold = threshold;
		weights = new long[256];

		// all initial frequencies are set to 1.
		totalFreq = 0;
		for (int i = 0; i < weights.length; i++)
			totalFreq += (weights[i] = 1);
	}

	@Override
	public long[] SymbolToFreqRange(int sym)
	{
		long cumFreq = 0;
		int i = 0;
		while(i < sym)
		{
			cumFreq += weights[i];
			i++;
		}

		forSym2Freq[0] = cumFreq;
		forSym2Freq[1] = weights[sym];
		return forSym2Freq;
	}

	@Override
	public long[] FreqToSymbolInfo(long cumFreq)
	{
		int sym;
		long right = 0;
		for (sym = 0; ; sym++) // TODO may be replace this 'for' by some index table?
		{
			right += weights[sym];
			if (right > cumFreq)
				break;
		}

		forFreq2Sym[0] = sym;
		forFreq2Sym[1] = right - weights[sym];
		forFreq2Sym[2] = weights[sym];
		return forFreq2Sym;
	}

	@Override
	public void updateStatistics(int sym)
	{
		weights[sym]++;
		totalFreq++;
		if (totalFreq > rescaleThreshold)
			rescale();
	}

	private void rescale()
	{
		totalFreq = 0;
		for (int i = 0; i < weights.length; i++)
			totalFreq += (weights[i] -= (weights[i] >> 1));
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
