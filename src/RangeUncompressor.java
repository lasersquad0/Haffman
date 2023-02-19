import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RangeUncompressor
{
	public enum Strategy { HIGHBYTE, RANGEBOTTOM }
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private long low;
	private long range;
	private long nextChar = 0;
	static final int CODEBITS = 48;
	static final int HIGHBYTE = CODEBITS + 8;
	static final long TOP = 1L << CODEBITS;
	static final long TOPTOP = 1L << (CODEBITS + 8);
	static final long BOTTOM = 1L << (CODEBITS - 8);
	private UncompressData uData;
	private ModelOrder0 model;
	private final CRC32 crc = new CRC32();
	private long decodedBytes = 0;
	private final long SHOW_PROGRESS_AFTER = 1_000_000; // display progress only if file size is larger then this
	private final Strategy strategy;

	RangeUncompressor(Strategy strategy)
	{
		this.strategy = strategy;
	}

	RangeUncompressor()
	{
		this(Strategy.HIGHBYTE);
	}

	public void uncompress(UncompressData uuData, ModelOrder0 model) throws IOException
	{
		this.uData = uuData;
		this.model = model;

		if(uData.sizeCompressed == 0) // for support of zero-length files
			return;

		decodedBytes = 0;
		delta = uData.sizeUncompressed/100;

		startProgress();

		low = 0;
		range = TOPTOP - 1; // сразу растягиваем на максимальный range
		//long totalFreq = uData.cumFreq[uData.cumFreq.length - 1];

		logger.finer(()->String.format("weights=%s", Arrays.toString(model.weights)));
		logger.finer(()->String.format("low=%X high=%X, range=%X", low, low + range, range));

		nextChar = 0;
		for (int i = 0; i < 7; i++) // assumed we have more than 7 compressed bytes in a stream
		{
			long ci = uData.sin.read();
			assert ci != -1;
			nextChar = (nextChar << 8) | ci;
		}

		while(decodedBytes < uData.sizeUncompressed)
		{
			updateProgress(decodedBytes);

			range = range / model.totalFreq;
			long t = (nextChar - low)/range; // ((nextChar - low) * totalFreq) / range;

			assert t < model.totalFreq;
			/*
			if(t >= model.totalFreq)
			{
				logger.warning(String.format("t(%,d) >= totalFreq (%,d)", t, model.totalFreq));
				t = model.totalFreq - 1;
			}*/

			//int j = 0;
			//while ((uData.cumFreq[j] <= t)) j++;

			long[] res = model.getSym(t); //uData.symbols[j - 1];
			int sym = (int)res[0];
			long left = res[1]; //uData.cumFreq[j - 1];
			long freq = model.weights[sym]; //uData.cumFreq[j];

			assert( (left + freq <= model.totalFreq) && (freq > 0) && (model.totalFreq <= BOTTOM) );

			low = low + left * range;
			assert (low & (TOPTOP - 1)) == low: "low is out of bounds!";
			range = freq * range;

			logger.finer(()->String.format("before scale sym=%X, low=%X high=%X, range=%X", nextChar, low, low + range, range));
			scale();
			logger.finer(()->String.format("after  scale sym=%X, low=%X high=%X, range=%X", nextChar, low, low + range, range));

			model.updateStatistics(sym);

			uData.sout.write(sym);
			crc.update(sym);
			decodedBytes++;
			logger.finer(()->String.format("uncompress output byte: 0x%X, count: %,d", sym, decodedBytes));
		}

		uData.sout.flush();

		finishProgress();

		uData.CRC32Value = crc.getValue();
	}

	private void scale() throws IOException
	{
		boolean outByte = ((low ^ low + range) < TOP);
		while( outByte || (range < BOTTOM) )
		{
			if((!outByte) && (range < BOTTOM)) // делает low+range=TOPTOP-1 если low+range было >TOPTOP-1
			{
				logger.finer(()->String.format("Weird range correction:BEFORE range:%X, BOTTOM: %X", range, BOTTOM));
				range = BOTTOM - (low & (BOTTOM - 1));
				logger.finer(()->String.format("Weird range correction:AFTER range:%X, BOTTOM: %X", range, BOTTOM));
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

	}

	private void startProgress()
	{
		if(uData.sizeUncompressed > SHOW_PROGRESS_AFTER) uData.cb.start();
	}

	private void finishProgress()
	{
		if(uData.sizeUncompressed > SHOW_PROGRESS_AFTER) uData.cb.finish();
	}

	long threshold = 0;
	long delta;
	private void updateProgress(long total)
	{
		if ((uData.sizeUncompressed > SHOW_PROGRESS_AFTER) && (total > threshold))
		{
			threshold += delta;
			uData.cb.heartBeat((int) (100 * threshold / uData.sizeUncompressed));
		}
	}
}
