import java.util.logging.Logger;

public abstract class ModelOrder0
{
	long totalFreq;

	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);

	protected final long[] forSym2Freq = new long[2];
	public abstract long[] SymbolToFreqRange(int sym);
	protected final long[] forFreq2Sym = new long[3];
	public abstract long[] FreqToSymbolInfo(long cumFreq);
	public abstract void updateStatistics(int sym);


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
