import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RangeUncompressor32
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private long low;
	private long range;
	private long nextChar;
	int CODEBITS = 24;
	int HIGHBYTE = CODEBITS + 8;
	long TOP = 1L << CODEBITS;
	long TOPTOP = 1L << (CODEBITS + 8);
	long BOTTOM = TOP >>> 8;
	CompressData uData;
	CRC32 crc = new CRC32();
	long decodedBytes = 0;


	public RangeUncompressor32()
	{
		this(Utils.MODE.MODE32); // by default using MODE32
	}

	public RangeUncompressor32(Utils.MODE mode)
	{
		if(mode == Utils.MODE.MODE32)
			initConsts(24);
		else
			initConsts(48);
	}

	private void initConsts(int codebits)
	{
		CODEBITS = codebits;
		HIGHBYTE = CODEBITS + 8;
		TOP      = 1L << CODEBITS;
		TOPTOP   = 1L << (CODEBITS + 8);
		BOTTOM   = 1L << (CODEBITS - 8);
	}
	public void uncompress(CompressData uuData) throws IOException
	{
		this.uData = uuData;
		if(uData.sizeCompressed == 0) // for support of zero-length files
			return;

		decodedBytes = 0;
		delta = uData.sizeUncompressed/100;

		low = 0;
		range = TOPTOP - 1; // сразу растягиваем на максимальный range
		//long totalFreq = uData.cumFreq[uData.cumFreq.length - 1];

		//logger.finest(()->String.format("symbols=%s", Arrays.toString(uData.symbols)));
		logger.finest(()->String.format("low=%X high=%X, range=%X", low, low + range, range));

		startProgress();

		nextChar = 0;
		for (int i = 0; i < HIGHBYTE/8; i++) // assumed we have more than 4 compressed bytes in a stream
		{
			long ci = uData.sin.read();
			assert ci != -1;
			nextChar <<= 8;
			nextChar |= ci;
		}

		while(decodedBytes < uData.sizeUncompressed)
		{
			updateProgress(decodedBytes);

			//long t = (nextChar - low) * totalFreq / range;

			range = range/uData.model.totalFreq;
			long t = (nextChar - low)/range;

			if(t >= uData.model.totalFreq)
			{
				logger.warning(String.format("t(%,d) >= totalFreq (%,d)", t, uData.model.totalFreq));
				t = uData.model.totalFreq - 1;
			}

			//int j = 0;
			//while(uData.cumFreq[j] <= t) j++;
			//int sym = uData.symbols[j - 1];

			long[] res = uData.model.FreqToSymbolInfo(t);
			int sym = (int)res[0];
			long left = res[1];
			long freq = res[2];

			uData.sout.write(sym);
			crc.update(sym);
			decodedBytes++;

			logger.finest(()->String.format("uncompress output byte: 0x%X, count: %d", sym, decodedBytes));

			//long left  = uData.cumFreq[j - 1];
			//long right = uData.cumFreq[j];

			assert (left + freq <= uData.model.totalFreq);
			assert (freq > 0);
			assert (uData.model.totalFreq <= BOTTOM);

			//low = low + (left * range)/totalFreq;
			//assert (low & (TOPTOP - 1)) == low: "low is out of bounds!";
			//range = (right - left) * range/totalFreq;

			low = low + left * range;
			assert (low & (TOPTOP - 1)) == low: "low is out of bounds!";
			range = range * freq;

			logger.finest(()->String.format("before scale sym=%X, low=%X high=%X, range=%X", nextChar, low, low + range, range));

			boolean outByte = ((low ^ low + range) < TOP);
			while( outByte || (range < BOTTOM) )
			{
				if((!outByte) && (range < BOTTOM)) // делает low+range=TOPTOP-1 если low+range было >TOPTOP-1
				{
					logger.finest(()->String.format("Weird range correction:BEFORE range:%X, BOTTOM: %X", range, BOTTOM));
					range = BOTTOM - (low & (BOTTOM - 1));
					logger.finest(()->String.format("Weird range correction:AFTER range:%X, BOTTOM: %X", range, BOTTOM));
					assert range > 0;
					assert (((low+range) >>> HIGHBYTE) == 0) || (((low+range) >>> HIGHBYTE) == 1):"low+range is out of bounds3!";
				}

				int ch = uData.sin.read();
				if (ch == -1) break;
				nextChar = (nextChar << 8) | ch;
				nextChar &= (TOPTOP - 1);
				range <<= 8;
				assert (range & (TOPTOP-1)) == range: "range is out of bounds!";
				low <<= 8;
				low &= (TOPTOP - 1);
				// если была хитрая коррекция выше тогда low+range==BOTTOM и после сдвига <<8, log+range==TOP, поэтому в assert добавлена проверка на ==1 это валидное значение
				assert (((low+range) >>> HIGHBYTE) == 0) || (((low+range) >>> HIGHBYTE) == 1):"low+range is out of bounds2!";
				outByte = ((low ^ low + range) < TOP);
			}

			logger.finest(()->String.format("after  scale sym=%X, low=%X high=%X, range=%X", nextChar, low, low+range, range));

/*
			double tt = ((nextChar - low) * (double)totalFreq) / range;
			long t = (long)tt;

			int j = 0;
			assert t <= totalFreq;
			while (/*(j < uData.cumFreq.length) && (uData.cumFreq[j] <= t)) j++;

			uData.sout.write(uData.symbols[j - 1]);
			decodedBytes++;

			logger.finer(String.format("uncompress output byte: 0x%X, count: %,d", uData.symbols[j - 1], decodedBytes));

			long left  = uData.cumFreq[j - 1];
			long right = uData.cumFreq[j];

			long r = (range+1)/totalFreq;
			low = low + left * r;
			range =  (right - left) * r - 1;

			logger.finer(String.format("before scale sym=%X, low=%X high=%X, range=%X", nextChar, low, low + range, range));

			normalize();

			logger.finer(String.format("after  scale sym=%X, low=%X high=%X, range=%X", nextChar, low, low+range, range));
			*/
		}

		uData.sout.flush();

		finishProgress();

		uData.CRC32Value = crc.getValue();
	}

	private void finishProgress()
	{
		if(uData.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) uData.cb.finish();
	}

	private void startProgress()
	{
		if(uData.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) uData.cb.start();
	}

	private long threshold = 0;
	private long delta;
	private void updateProgress(long total)
	{
		if((uData.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) && (total > threshold))
		{
			threshold += delta;
			uData.cb.heartBeat((int)(100* threshold /uData.sizeUncompressed));
		}
	}

}
