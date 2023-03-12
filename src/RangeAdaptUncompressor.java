import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RangeAdaptUncompressor implements BlockUncompressable
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private long low;
	private long range;
	private long nextChar;
	int CODEBITS = 24; //48;
	int HIGHBYTE = CODEBITS + 8;
	long TOP = 1L << CODEBITS;
	long TOPTOP = 1L << (CODEBITS + 8);
	long BOTTOM = 1L << (CODEBITS - 8);
	private CompressData udata;
	private ModelOrder0Adapt model;
	private final CRC32 crc = new CRC32();
	private long decodedBytes;


	public RangeAdaptUncompressor()
	{
		this(Utils.MODE.MODE32); // by default using MODE32
	}

	public RangeAdaptUncompressor(Utils.MODE mode)
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

	@Override
	public void startBlockUncompressing(CompressData uData)
	{
		this.udata = uData;
		model = new ModelOrder0Adapt(BOTTOM);

		if(uData.sizeCompressed == 0) // for support of zero-length files
			return;

		decodedBytes = 0;
		delta = uData.sizeUncompressed/100;

		startProgress();

		resetLowRange();

		logger.finer(()->String.format("weights=%s", Arrays.toString(model.weights)));
		logger.finer(()->String.format("low=%X high=%X, range=%X", low, low + range, range));
	}

	@Override
	public void finishBlockUncompressing()
	{
		finishProgress();

		udata.CRC32Value = crc.getValue();
	}

	@Override
	public void uncompressBlock(BlockCoderData data) throws IOException
	{
		data.resetDest();
		nextChar = loadInitialBytes();

		for (int i = 0; i < data.uBlockSize; i++)
		{
			updateProgress(decodedBytes);

			int symbol = decodeSymbol();
			crc.update(symbol);
			data.writeDest(symbol);
			decodedBytes++;
		}

		resetLowRange();
	}

	public void uncompress(CompressData uuData) throws IOException
	{
		this.udata = uuData;
		model = new ModelOrder0Adapt(BOTTOM);

		if(udata.sizeCompressed == 0) // for support of zero-length files
			return;

		decodedBytes = 0;
		delta = udata.sizeUncompressed/100;

		startProgress();

		resetLowRange();

		logger.finest(()->String.format("weights=%s", Arrays.toString(model.weights)));
		logger.finest(()->String.format("low=%X high=%X, range=%X", low, low + range, range));

		nextChar = loadInitialBytes();

		while(decodedBytes < udata.sizeUncompressed)
		{
			updateProgress(decodedBytes);

			int sym = decodeSymbol();
			writeByte(sym);
		}

		udata.sout.flush();

		finishProgress();

		udata.CRC32Value = crc.getValue();
	}

	private void resetLowRange()
	{
		low = 0;
		range = TOPTOP - 1; // сразу растягиваем на максимальный range
	}

	private long loadInitialBytes() throws IOException
	{
		long ch = 0;
		for (int i = 0; i < HIGHBYTE>>>3; i++) // assumed we have more than X compressed bytes in a stream
		{
			long ci = udata.sin.read();
			assert ci != -1;
			ch = (ch << 8) | ci;
		}

		return ch;
	}

	private int decodeSymbol() throws IOException
	{
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

		long[] res = model.FreqToSymbolInfo(t); //uData.symbols[j - 1];
		int sym = (int)res[0];
		long left = res[1]; //uData.cumFreq[j - 1];
		long freq = res[2]; //udata.model.weights[sym]; //uData.cumFreq[j];

		assert( (left + freq <= model.totalFreq) && (freq > 0) && (model.totalFreq <= BOTTOM) );

		low = low + left * range;
		assert (low & (TOPTOP - 1)) == low: "low is out of bounds!";
		range = freq * range;

		logger.finest(()->String.format("before scale sym=%X, low=%X high=%X, range=%X", nextChar, low, low + range, range));
		scale();
		logger.finest(()->String.format("after  scale sym=%X, low=%X high=%X, range=%X", nextChar, low, low + range, range));

		model.updateStatistics(sym);

		return sym;
	}

	private void scale() throws IOException
	{
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

			int ch = udata.sin.read();
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

	public void writeByte(int sym) throws IOException
	{
		udata.sout.write(sym);
		crc.update(sym);
		decodedBytes++;
		logger.finest(()->String.format("uncompress output byte: 0x%X, count: %,d", sym, decodedBytes));
	}


	private void startProgress()
	{
		if(udata.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) udata.cb.start();
	}

	private void finishProgress()
	{
		if(udata.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) udata.cb.finish();
	}

	long threshold = 0;
	long delta;
	private void updateProgress(long total)
	{
		if ((udata.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) && (total > threshold))
		{
			threshold += delta;
			udata.cb.heartBeat((int) (100 * threshold / udata.sizeUncompressed));
		}
	}
}
