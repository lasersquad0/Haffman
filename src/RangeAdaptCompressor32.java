import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RangeAdaptCompressor32
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private long low;
	private long range;
	static final int CODEBITS = 24;
	static final int HIGHBYTE = CODEBITS + 8;
	static final long TOP = 1L << CODEBITS;
	static final long TOPTOP = 1L << (CODEBITS + 8);
	static final long BOTTOM = 1L << (CODEBITS - 8);
	private CompressData cData;
	private ModelOrder0 model;
	private final CRC32 crc = new CRC32();
	public long encodedBytes = 0;


	public void compress(CompressData ccData, ModelOrder0 mmodel) throws IOException
	{
		this.cData = ccData;
		this.model = mmodel;

		assert cData.sizeUncompressed > 0;

		encodedBytes = 0;
		delta = cData.sizeUncompressed/100;

		startProgress();

		low = 0;
		range = TOPTOP - 1; // сразу растягиваем на максимальный range
		//long totalFreq = cData.cumFreq[cData.cumFreq.length - 1];

		logger.finer(()->String.format("weights=%s", Arrays.toString(model.weights)));
		logger.finer(()->String.format("low=%X high=%X, range=%X", low, low + range, range));

		long total = 0;

		int ch;
		while((ch = cData.sin.read()) != -1)
		{
			crc.update(ch); // calculate CRC32 of *Uncompressed* file here
			updateProgress(total++);

			encodeSymbol(ch);
		}

		low = low + (range>>1); // need value INSIDE the interval, take a middle
		for (int i = 0; i < (HIGHBYTE >>> 3); i++)
		{
			int ob = (int) ((low >> (CODEBITS - i*8)) & 0xFF);
			cData.sout.write(ob);
			logger.finer(()->String.format("compress output byte: %X", ob));
			encodedBytes++;
		}

		cData.sout.flush();

		finishProgress();

		cData.sizeCompressed = encodedBytes;
		cData.CRC32Value = crc.getValue();
	}

	private void encodeSymbol(int sym) throws IOException
	{
		//int index = cData.symbol_to_freq_index[nextCh];
		long left = model.getCumFreq(sym);//cData.cumFreq[index - 1];
		long freq = model.weights[sym];

		assert(left + freq <= model.totalFreq);
		assert(freq > 0);
		assert(model.totalFreq <= BOTTOM);

		range = range / model.totalFreq;
		low = low + left * range;
		range = freq * range;

		logger.finer(()->String.format("before scale sym=%X, low=%X high=%X, range=%X", sym, low, low + range, range));
		scale();
		logger.finer(()->String.format("AFTER  scale sym=%X, low=%X high=%X, range=%X", sym, low, low + range, range));

		model.updateStatistics(sym);
	}

	private void scale() throws IOException
	{
		boolean outByte = ((low ^ low + range) < TOP);
		while( outByte || (range < BOTTOM) )
		{
			if( (!outByte) && (range < BOTTOM) ) // делает low+range=TOPTOP-1 если low+range было >TOPTOP-1
			{
				logger.finer(()->String.format("Weird range correction:BEFORE range:%X, BOTTOM: %X", range, BOTTOM));
				range = BOTTOM - (low & (BOTTOM - 1));
				logger.finer(()->String.format("Weird range correction:AFTER range:%X, BOTTOM: %X", range, BOTTOM));
				assert range > 0;
				assert (((low+range) >>> HIGHBYTE) == 0) || (((low+range) >>> HIGHBYTE) == 1):"low+range is out of bounds3!";
			}

			cData.sout.write((int)(low >>> CODEBITS));
			encodedBytes++;
			logger.finer(()->String.format("Output byte: 0x%X, count: %,d", low >>> CODEBITS, encodedBytes));
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
		if(cData.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) cData.cb.start();
	}

	private void finishProgress()
	{
		if(cData.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) cData.cb.finish();
	}

	long threshold = 0;
	long delta;
	private void updateProgress(long total)
	{
		if ((cData.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) && (total > threshold))
		{
			threshold += delta;
			cData.cb.heartBeat((int) (100 * threshold / cData.sizeUncompressed));
		}
	}
}