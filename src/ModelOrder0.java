import java.util.logging.Logger;

public class ModelOrder0
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	final long rescaleThreshold; // = (1<<16);
	long[] weights = new long[256]; //[256 + 1];
	long totalFreq;
	long[] forGetSym = new long[2];

	ModelOrder0(long rThreshold)
	{
		rescaleThreshold = rThreshold;

		// all initial frequencies are set to 1.
		totalFreq = 0;
		for (int i = 0; i < weights.length; i++)
			totalFreq += (weights[i] = 1);
	}

	public long getCumFreq(int sym)
	{
		long cumFreq = 0;
		int i = 0;
		while(i < sym)
		{
			cumFreq += weights[i];
			i++;
		}
		return cumFreq;
	}
/*
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
	public void updateStatistics(int sym)
	{
		weights[sym]++;
		totalFreq++;
		if (totalFreq > rescaleThreshold)
			rescale();
	}

	public long[] getSym(long cumFreq)
	{
		int sym;
		long right;
		for (right = sym = 0; ; sym++)
		{
			right += weights[sym];
			if (right > cumFreq)
				break;
		}

		forGetSym[0] = sym;
		forGetSym[1] = right - weights[sym];
		return forGetSym;
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
	 */
	

	private void rescale()
	{
		totalFreq = 0;
		for (int i = 0; i < weights.length; i++)
			totalFreq += (weights[i] -= (weights[i] >> 1));
	}
}
