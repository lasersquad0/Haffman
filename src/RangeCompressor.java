import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RangeCompressor
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private long low;
	private long range;
	private int nextCh;
	static final int CODEBITS = 48;
	static final int HIGHBYTE = CODEBITS + 8;
	static final long TOP = 1L << CODEBITS;
	static final long TOPTOP = 1L << (CODEBITS + 8);
	static final long BOTTOM = 1L << (CODEBITS - 8);
	private CompressData cData;
	private final CRC32 crc = new CRC32();
	public long encodedBytes = 0;


	public void compress(CompressData ccData) throws IOException
	{
		this.cData = ccData;

		assert cData.sizeUncompressed > 0;

		encodedBytes = 0;
		delta = cData.sizeUncompressed/100;

		startProgress();

		low = 0;
		range = TOPTOP - 1; // сразу растягиваем на максимальный range
		//long totalFreq = cData.cumFreq[cData.cumFreq.length - 1];

		//logger.finest(()->String.format("weights=%s", Arrays.toString(model.weights)));
		logger.finest(()->String.format("low=%X high=%X, range=%X", low, low + range, range));

		long total = 0;

		while((nextCh = cData.sin.read()) != -1)
		{
			crc.update(nextCh); // calculate CRC32 of *Uncompressed* file here

			updateProgress(total++);

			//int index = cData.symbol_to_freq_index[nextCh];
			long[] res = ccData.model.SymbolToFreqRange(nextCh);//cData.cumFreq[index - 1];
			long left = res[0];
			long freq = res[1]; //model.weights[nextCh];

			assert(left + freq <= ccData.model.totalFreq);
			assert(freq > 0);
			assert(ccData.model.totalFreq <= BOTTOM);

			range = range / ccData.model.totalFreq;
			low = low + left * range;
			range = freq * range;

			logger.finest(()->String.format("before scale sym=%X, low=%X high=%X, range=%X", nextCh, low, low + range, range));
			scale();
			logger.finest(()->String.format("AFTER  scale sym=%X, low=%X high=%X, range=%X", nextCh, low, low + range, range));

			ccData.model.updateStatistics(nextCh);
		}

		writeLastBytes();

		cData.sout.flush();

		finishProgress();

		cData.sizeCompressed = encodedBytes;
		cData.CRC32Value = crc.getValue();
	}

	private void writeLastBytes() throws IOException
	{
		low = low + (range>>1); // need value INSIDE the interval, take a middle
		for (int i = CODEBITS; i >= 0; i -= 8)
		{
			int ob = (int) ((low >> i) & 0xFF);
			cData.sout.write(ob);
			encodedBytes++;
			logger.finest(()->String.format("compress output byte: %X", ob));
		}
	}

	private void scale() throws IOException
	{
		boolean outByte = ((low ^ low + range) < TOP);
		while( outByte || (range < BOTTOM) )
		{
			if( (!outByte) && (range < BOTTOM) ) // делает low+range=TOPTOP-1 если low+range было >TOPTOP-1
			{
				logger.finest(()->String.format("Weird range correction:BEFORE range:%X, BOTTOM: %X", range, BOTTOM));
				range = BOTTOM - (low & (BOTTOM - 1));
				logger.finest(()->String.format("Weird range correction:AFTER range:%X, BOTTOM: %X", range, BOTTOM));
				assert range > 0;
				assert (((low+range) >>> HIGHBYTE) == 0) || (((low+range) >>> HIGHBYTE) == 1):"low+range is out of bounds3!";
			}

			cData.sout.write((int)(low >>> CODEBITS));
			encodedBytes++;
			logger.finest(()->String.format("Output byte: 0x%X, count: %,d", low >>> CODEBITS, encodedBytes));
			range <<= 8;
			assert (range & (TOPTOP-1)) == range: "range is out of bounds!";
			low <<= 8;
			low &= (TOPTOP - 1);
			// если была хитрая коррекция выше тогда low+range==BOTTOM и после сдвига <<8, log+range==TOP, поэтому в assert добавлена проверка на ==1 это валидное значение
			assert (((low+range) >>> HIGHBYTE) == 0) || (((low+range) >>> HIGHBYTE) == 1):"low+range is out of bounds2!";
			outByte = ((low ^ low + range) < TOP);
		}
	}

	private final int SHOW_PROGRESS_AFTER = 1_000_000; // display progress only if file size is larger then this
	private void startProgress()
	{
		if(cData.sizeUncompressed > SHOW_PROGRESS_AFTER) cData.cb.start();
	}

	private void finishProgress()
	{
		if(cData.sizeUncompressed > SHOW_PROGRESS_AFTER) cData.cb.finish();
	}

	long threshold = 0;
	long delta;
	private void updateProgress(long total)
	{
		if ((cData.sizeUncompressed > SHOW_PROGRESS_AFTER) && (total > threshold))
		{
			threshold += delta;
			cData.cb.heartBeat((int) (100 * threshold / cData.sizeUncompressed));
		}
	}
}