import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RangeUncompressor32
{
	public enum Strategy { HIGHBYTE, RANGEBOTTOM }
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private long low;
	private long range;
	private long nextChar;
	final int CODEBITS = 24;
	final long TOP = 1L << CODEBITS;
	final long TOPTOP = 1L << (CODEBITS + 8);
	final long BOTTOM = TOP >>> 8;
	RangeUncompressData uData;
	CRC32 crc = new CRC32();
	long decodedBytes = 0;
	//long compressedBytesCounter = 0;
	final long SHOW_PROGRESS_AFTER = 1_000_000; // display progress only if file size is larger then this
	private final Strategy strategy;

	RangeUncompressor32(Strategy strategy)
	{
		this.strategy = strategy;
	}

	RangeUncompressor32()
	{
		this(Strategy.HIGHBYTE);
	}
/*
	public void uncompress(UncompressData uuData) throws IOException
	{
		this.uData = (RangeUncompressData) uuData;
		if(uData.sizeCompressed == 0) // for support of zero-length files
			return;

		decodedBytes = 0;
		compressedBytesCounter = 0;

		var rc = new RangeCoder();
		var o0c = new Order0Coder(rc);

		int Symbol;
		rc.StartDecode(uData.sin);

		while( (Symbol = o0c.decodeSymbol()) != o0c.STOP )
		{
			uData.sout.write(Symbol);
			decodedBytes++;
			 //putc(Symbol, DecodedFile);
			logger.finer(String.format("decoded byte: 0x%X, count: %,d", Symbol, decodedBytes));
		}

		o0c.stopDecode();
	}

 */
	public void uncompress(UncompressData uuData) throws IOException
	{
		this.uData = (RangeUncompressData) uuData;
		if(uData.sizeCompressed == 0) // for support of zero-length files
			return;

		decodedBytes = 0;
		//compressedBytesCounter = 0;
		delta = uData.sizeUncompressed/100;

		low = 0;
		range = TOPTOP - 1; // сразу растягиваем на максимальный range
		long totalFreq = uData.cumFreq[uData.cumFreq.length - 1];

		logger.finer(()->String.format("symbols=%s", Arrays.toString(uData.symbols)));
		logger.finer(()->String.format("low=%X high=%X, range=%X", low, low + range, range));

		startProgress();

		nextChar = 0;
		for (int i = 0; i < 4; i++) // assumed we have more than 4 compressed bytes in a stream
		{
			long ci = uData.sin.read();
			assert ci != -1;
			nextChar <<= 8;
			nextChar |= ci;
			//compressedBytesCounter++;
		}

		while(decodedBytes < uData.sizeUncompressed)
		{
			updateProgress(decodedBytes);//compressedBytesCounter++);

			range = range/totalFreq;
			long t = (nextChar - low)/range; // ((nextChar - low) * totalFreq) / range;

			if(t >= totalFreq)
			{
				logger.warning(String.format("t(%,d) >= totalFreq (%,d)", t, totalFreq));
				t = totalFreq - 1;
			}

			int j = 0;
			while(uData.cumFreq[j] <= t) j++;

			int sym = uData.symbols[j - 1];
			uData.sout.write(sym);
			crc.update(sym);
			decodedBytes++;

			logger.finer(()->String.format("uncompress output byte: 0x%X, count: %,d", sym, decodedBytes));

			long left  = uData.cumFreq[j - 1];
			long right = uData.cumFreq[j];

			assert( (right <= totalFreq) && (right - left > 0) && (totalFreq <= BOTTOM) );

			low = low + left * range;
			assert (low & (TOPTOP - 1)) == low: "low is out of bounds!";
			range = range * (right - left);

			logger.finer(()->String.format("before scale sym=%X, low=%X high=%X, range=%X", nextChar, low, low + range, range));

			boolean outByte = ((low ^ low + range) < TOP);
			while( outByte || (range < BOTTOM) )
			{
				if((!outByte) && (range < BOTTOM)) // делает low+range=TOPTOP-1 если low+range было >TOPTOP-1
				{
					logger.finer(()->String.format("Weird range correction:BEFORE range:%X, BOTTOM: %X", range, BOTTOM));
					range = BOTTOM - (low & (BOTTOM - 1));
					//range = ((-low) & (BOTTOM - 1));
					logger.finer(()->String.format("Weird range correction:AFTER range:%X, BOTTOM: %X", range, BOTTOM));
					assert range > 0;
					assert (((low+range) >>> 32) == 0) || (((low+range) >>> 32) == 1):"low+range is out of bounds3!";
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
				assert (((low+range) >>> 32) == 0) || (((low+range) >>> 32) == 1):"low+range is out of bounds2!";
				outByte = ((low ^ low + range) < TOP);
			}

			logger.finer(()->String.format("after  scale sym=%X, low=%X high=%X, range=%X", nextChar, low, low+range, range));

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
		if(uData.sizeUncompressed > SHOW_PROGRESS_AFTER) uData.cb.finish();
	}

	private void startProgress()
	{
		if(uData.sizeUncompressed > SHOW_PROGRESS_AFTER)
			uData.cb.start();
	}

	private long threshold = 0;
	private long delta;
	private void updateProgress(long total)
	{
		if((uData.sizeUncompressed > SHOW_PROGRESS_AFTER) && (total > threshold))
		{
			threshold += delta;
			uData.cb.heartBeat((int)(100* threshold /uData.sizeUncompressed));
		}
	}

}
