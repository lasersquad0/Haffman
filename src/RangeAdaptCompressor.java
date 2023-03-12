import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RangeAdaptCompressor implements BlockCompressable
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	int CODEBITS = 24;
	int HIGHBYTE = CODEBITS + 8;
	long TOP = 1L << CODEBITS;
	long TOPTOP = 1L << (CODEBITS + 8);
	long BOTTOM = 1L << (CODEBITS - 8);
	private long low;
	private long range;
	private CompressData cdata;
	private ModelOrder0Adapt model;
	private final CRC32 crc = new CRC32();
	public long outputBytes;
	public long inputBytes;


	public RangeAdaptCompressor()
	{
		this(Utils.MODE.MODE32);  // by default use MODE32
	}

	public RangeAdaptCompressor(Utils.MODE mode)
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
	public void startBlockCompressing(CompressData ccData)
	{
		startCompressing(ccData);
	}

	@Override
	public void finishBlockCompressing() throws IOException
	{
		finishCompressing();
	}

	@Override
	public void compressBlock(BlockCoderData data) throws IOException
	{
		long save = outputBytes;

		for (int i = 0; i < data.bytesInBlock; i++)
		{
			int sym = Byte.toUnsignedInt(data.srcblock[i]);
			crc.update(sym); // calculate CRC32 of *Uncompressed* file here
			updateProgress(inputBytes++);

			encodeSymbol(sym);
		}

		writeLastBytes(); // additionally resets low and range variables to initial values

		data.cBlockSize = (int)(outputBytes - save);
	}

	public void compress(CompressData ccData) throws IOException
	{
		startCompressing(ccData);

		int ch;
		while((ch = cdata.sin.read()) != -1)
		{
			crc.update(ch); // calculate CRC32 of *Uncompressed* file here
			updateProgress(inputBytes++);

			encodeSymbol(ch);
		}

		writeLastBytes();

		cdata.sout.flush();

		finishCompressing();
	}

	private void startCompressing(CompressData ccData)
	{
		this.cdata = ccData;
		model = new ModelOrder0Adapt(BOTTOM);

		assert cdata.sizeUncompressed > 0;

		outputBytes = 0;
		inputBytes = 0;
		delta = cdata.sizeUncompressed/100;

		resetLowAndRange();

		logger.finest(()->String.format("weights=%s", Arrays.toString(model.weights)));
		logger.finest(()->String.format("low=%X high=%X, range=%X", low, low + range, range));

		startProgress();
	}

	private void finishCompressing()
	{
		finishProgress();

		cdata.sizeCompressed = outputBytes;
		cdata.CRC32Value = crc.getValue();
	}

	private void resetLowAndRange()
	{
		low = 0;
		range = TOPTOP - 1; // сразу растягиваем на максимальный range
	}

	private void encodeSymbol(int sym) throws IOException
	{
		//int index = cData.symbol_to_freq_index[nextCh];
		long[] res = model.SymbolToFreqRange(sym);//cData.cumFreq[index - 1];
		long left = res[0];
		long freq = res[1]; //cdata.model.weights[sym];

		assert(left + freq <= model.totalFreq);
		assert(freq > 0);
		assert(model.totalFreq <= BOTTOM);

		range = range / model.totalFreq;
		low = low + left * range;
		range = freq * range;

		//logger.finest(()->String.format("before scale sym=%X, low=%X high=%X, range=%X", sym, low, low + range, range));
		scale();
		//logger.finest(()->String.format("AFTER  scale sym=%X, low=%X high=%X, range=%X", sym, low, low + range, range));

		model.updateStatistics(sym);
	}


	private void scale() throws IOException
	{
		boolean outByte = ((low ^ low + range) < TOP);
		while( outByte || (range < BOTTOM) )
		{
			if( (!outByte) && (range < BOTTOM) ) // делает low+range=TOPTOP-1 если low+range было >TOPTOP-1
			{
				//logger.finest(()->String.format("Weird range correction:BEFORE range:%X, BOTTOM: %X", range, BOTTOM));
				range = BOTTOM - (low & (BOTTOM - 1));
				//logger.finest(()->String.format("Weird range correction:AFTER range:%X, BOTTOM: %X", range, BOTTOM));
				assert range > 0;
				assert (((low+range) >>> HIGHBYTE) == 0) || (((low+range) >>> HIGHBYTE) == 1):"low+range is out of bounds3!";
			}

			writeByte((int)(low >>> CODEBITS));
			//logger.finest(()->String.format("Output byte: 0x%X, count: %,d", low >>> CODEBITS, outputBytes));
			range <<= 8;
			assert (range & (TOPTOP-1)) == range: "range is out of bounds!";
			low <<= 8;
			low &= (TOPTOP - 1);
			// если была хитрая коррекция выше тогда low+range==BOTTOM и после сдвига <<8, log+range==TOP, поэтому в assert добавлена проверка на ==1 это валидное значение
			assert (((low+range) >>> HIGHBYTE) == 0) || (((low+range) >>> HIGHBYTE) == 1):"low+range is out of bounds2!";
			outByte = ((low ^ low + range) < TOP);
		}
	}

	private void writeLastBytes() throws IOException
	{
		low = low + (range>>1); // need value INSIDE the interval, take a middle
		for (int i = CODEBITS; i >= 0; i -= 8)
		{
			int ob = (int) ((low >> i) & 0xFF);
			writeByte(ob);
			logger.finest(()->String.format("compress output byte: %X", ob));
		}

		resetLowAndRange();
	}

	public void writeByte(int b) throws IOException
	{
		cdata.sout.write(b);
		outputBytes++;
	}

	private void startProgress()
	{
		if(cdata.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) cdata.cb.start();
	}

	private void finishProgress()
	{
		if(cdata.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) cdata.cb.finish();
	}

	long threshold = 0;
	long delta;
	private void updateProgress(long total)
	{
		if ((cdata.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) && (total > threshold))
		{
			threshold += delta;
			cdata.cb.heartBeat((int) (100 * threshold / cdata.sizeUncompressed));
		}
	}
}