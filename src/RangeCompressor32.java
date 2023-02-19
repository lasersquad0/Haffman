import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RangeCompressor32 {
	public enum Strategy { HIGHBYTE, RANGEBOTTOM }
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private long low;
	private long range;
	private int nextCh;
	static final int CODEBITS = 24;
	static final long TOP = 1L << CODEBITS;
	static final long TOPTOP = 1L << (CODEBITS + 8);
	static final long BOTTOM = TOP >>> 8;
	//final long INT_MASK = 0x00000000FFFFFFFFL;
	private RangeCompressData cData;
	private final CRC32 crc = new CRC32();
	public long encodedBytes = 0;
	final long SHOW_PROGRESS_AFTER = 1_000_000; // display progress only if file size is larger then this
	private final Strategy strategy; // you can use highbyte(low)==highbyte(high) strategy or range<BOTTOM (strategy==2)


	RangeCompressor32(Strategy strategy)
	{
		this.strategy = strategy;
	}
	RangeCompressor32()
	{
		this(Strategy.HIGHBYTE);
	}
/*
	public void compress(CompressData ccData) throws IOException
	{
		this.cData = (RangeCompressData) ccData;
		assert cData.sizeUncompressed > 0;

		encodedBytes = 0;  // сбрасываем счетчики

		var rc = new RangeCoder();
		Order0Coder o0c = new Order0Coder(rc);
		int Symbol;

		rc.StartEncode(cData.sout);

		while((Symbol = cData.sin.read()) != -1)
		//while ((Symbol = getc(DecodedFile)) >= 0)
		{
			o0c.encodeSymbol(Symbol);
		}

		o0c.stopEncode();

		cData.sout.flush();

		cData.sizeCompressed = encodedBytes = rc.getPassed();
		cData.CRC32Value = crc.getValue();
	}
*/
	public void compress(CompressData ccData) throws IOException
	{
		this.cData = (RangeCompressData) ccData;
		assert cData.sizeUncompressed > 0;

		startProgress();

		encodedBytes = 0;  // сбрасываем счетчики
		delta = cData.sizeUncompressed/100;

		low = 0;
		range = TOPTOP - 1; // сразу растягиваем на максимальный range
		long totalFreq = cData.cumFreq[cData.cumFreq.length - 1];

		logger.finer(()->String.format("freq=%s", Arrays.toString(cData.cumFreq)));
		logger.finer(()->String.format("initial state: low=%X high=%X, range=%X", low, low + range, range));

		long total = 0;
	//	int ch;

		while((nextCh = cData.sin.read()) != -1)
		{
			crc.update(nextCh); // calculate CRC32 of *Uncompressed* file here
			updateProgress(++total);

			int index = cData.symbol_to_freq_index[nextCh];
			long left = cData.cumFreq[index - 1];
			long right = cData.cumFreq[index];

			assert(right <= totalFreq);
			assert( (right-left) > 0);
			assert(totalFreq <= BOTTOM);

			range = range/totalFreq;
			low = low + left * range;
			assert (low & (TOPTOP - 1)) == low: "low is out of bounds!";
			range =  (right - left) * range;

			logger.finer(()->String.format("before scale sym=%X, low=%X high=%X, range=%X", nextCh, low, low + range, range));

			boolean outByte = ((low ^ low + range) < TOP);
			while( outByte || (range < BOTTOM) /*&& ((range= -low & BOTTOM-1),1)*/)
			{
				if( (!outByte) && (range < BOTTOM) ) // делает low+range=TOPTOP-1 если low+range было >TOPTOP-1
				{
					logger.finer( () -> String.format("Weird range correction:BEFORE range:%X, BOTTOM: %X", range, BOTTOM));
					range = BOTTOM - (low & (BOTTOM - 1));
					logger.finer( () -> String.format("Weird range correction:AFTER range:%X, BOTTOM: %X", range, BOTTOM));
					assert range > 0;
					assert (((low+range) >>> 32) == 0) || (((low+range) >>> 32) == 1):"low+range is out of bounds3!";
				}

				cData.sout.write((int)(low >>> 24));
				encodedBytes++;
				logger.finer(() ->String.format("Output byte: 0x%X, count: %,d", low >>> 24, encodedBytes));
				range <<= 8;
				assert (range & (TOPTOP-1)) == range: "range is out of bounds!";
				low <<= 8;
				low &= (TOPTOP - 1);
				// если была хитрая коррекция выше тогда low+range==BOTTOM и после сдвига <<8, log+range==TOP, поэтому в assert добавлена проверка на ==1 это валидное значение
				assert (((low+range) >>> 32) == 0) || (((low+range) >>> 32) == 1):"low+range is out of bounds2!";
				outByte = ((low ^ low + range) < TOP);
			}

			logger.finer(()->String.format("AFTER  scale sym=%X, low=%X high=%X, range=%X", nextCh, low, low + range, range));
		}

		//low = low + range/2; // need value INSIDE the interval, take a middle
		cData.sout.write((int)(low >>> 24));
		encodedBytes++;
		logger.finer(()->String.format("Output byte: 0x%X, count: %,d", low >>> 24, encodedBytes));
		cData.sout.write((int) ((low >>> 16) & 0xFF));
		encodedBytes++;
		logger.finer(()->String.format("Output byte: 0x%X, count: %,d", (low >>> 16) & 0xFF, encodedBytes));
		cData.sout.write((int) ((low >>> 8) & 0xFF));
		encodedBytes++;
		logger.finer(()->String.format("Output byte: 0x%X, count: %,d", (low >>> 8) & 0xFF, encodedBytes));
		cData.sout.write((int) (low & 0xFF));
		encodedBytes++;
		logger.finer(()->String.format("Output byte: 0x%X, count: %,d", low & 0xFF, encodedBytes));

		cData.sout.flush();
		finishProgress();

		cData.sizeCompressed = encodedBytes;
		cData.CRC32Value = crc.getValue();
	}

/*

	private void writeBuffer(byte buf, byte extra) throws IOException
	{
		cData.sout.write(buf);
		encodedBytes++;

		logger.finer(String.format("Output byte: 0x%X, count: %,d", buf, encodedBytes));

		for(; help > 0; help--)
		{
			cData.sout.write(extra);
			encodedBytes++;
			logger.finer(String.format("Output byte: 0x%X, count: %,d", extra, encodedBytes));
		}
	}
*/

	private long threshold = 0;
	private long delta;
	private void updateProgress(long total)
	{
		if((cData.sizeUncompressed > SHOW_PROGRESS_AFTER) && (total > threshold))
		{
			threshold += delta;
			cData.cb.heartBeat((int)(100* threshold /cData.sizeUncompressed));
		}
	}

	private void finishProgress()
	{
		if(cData.sizeUncompressed > SHOW_PROGRESS_AFTER) cData.cb.finish();
	}

	private void startProgress()
	{
		if(cData.sizeUncompressed > SHOW_PROGRESS_AFTER) cData.cb.start();
	}


}