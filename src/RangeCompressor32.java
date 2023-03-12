import java.io.IOException;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RangeCompressor32 {
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private long low;
	private long range;
	int CODEBITS = 24;
	int HIGHBYTE = CODEBITS + 8;
	long TOP = 1L << CODEBITS;
	long TOPTOP = 1L << (CODEBITS + 8);
	long BOTTOM = TOP >>> 8;
	private CompressData cData;
	private final CRC32 crc = new CRC32();
	public long encodedBytes = 0;

	public RangeCompressor32()
	{
		this(Utils.MODE.MODE32);  // by default use MODE32
	}

	public RangeCompressor32(Utils.MODE mode)
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

	public void compress(CompressData ccData) throws IOException
	{
		this.cData = ccData;
		assert cData.sizeUncompressed > 0;

		startProgress();

		encodedBytes = 0;  // сбрасываем счетчики
		delta = cData.sizeUncompressed/100;

		low = 0;
		range = TOPTOP - 1; // сразу растягиваем на максимальный range
		//long totalFreq = cData.cumFreq[cData.cumFreq.length - 1];

		//logger.finest(()->String.format("freq=%s", Arrays.toString(cData.cumFreq)));
		logger.finest(()->String.format("initial state: low=%X high=%X, range=%X", low, low + range, range));

		long total = 0;

		int ch;
		while((ch = cData.sin.read()) != -1)
		{
			crc.update(ch); // calculate CRC32 of *Uncompressed* file here
			updateProgress(++total);

			encodeSymbol(ch);
		}

		writeLastBytes();

		cData.sout.flush();
		finishProgress();

		cData.sizeCompressed = encodedBytes;
		cData.CRC32Value = crc.getValue();
	}

	private void encodeSymbol(int sym) throws IOException
	{
		long[] res = cData.model.SymbolToFreqRange(sym);
		//int index = cData.symbol_to_freq_index[sym];
		long left = res[0]; //cData.cumFreq[index - 1];
		long freq = res[1]; //cData.cumFreq[index];

		assert (left + freq <= cData.model.totalFreq);
		assert (freq > 0);
		assert (cData.model.totalFreq <= BOTTOM);

		//low = low + (left * range)/totalFreq;
		//assert (low & (TOPTOP - 1)) == low: "low is out of bounds!";
		//range = (right - left) * range/totalFreq;

		range = range/ cData.model.totalFreq;
		low = low + left * range;
		assert (low & (TOPTOP - 1)) == low: "low is out of bounds!";
		range = freq * range;

		logger.finest(()->String.format("before scale sym=%X, low=%X high=%X, range=%X", sym, low, low + range, range));

		boolean outByte = ((low ^ low + range) < TOP);
		while( outByte || (range < BOTTOM) /*&& ((range= -low & BOTTOM-1),1)*/)
		{
			if( (!outByte) && (range < BOTTOM) ) // делает low+range=TOPTOP-1 если low+range было >TOPTOP-1
			{
				logger.finest( ()-> String.format("Weird range correction:BEFORE range:%X, BOTTOM: %X", range, BOTTOM));
				range = BOTTOM - (low & (BOTTOM - 1));
				logger.finest( () -> String.format("Weird range correction:AFTER range:%X, BOTTOM: %X", range, BOTTOM));
				assert range > 0;
				assert (((low+range) >>> HIGHBYTE) == 0) || (((low+range) >>> HIGHBYTE) == 1):"low+range is out of bounds3!";
			}

			int bwrite = (int)(low >>> CODEBITS);
			writeByte(bwrite);
			logger.finest(()->String.format("Output byte: 0x%X, count: %d", bwrite, encodedBytes));
			//if(bwrite == 0) logger.fine(() ->String.format("Output byte is **ZERO**, count: %,d", encodedBytes));
			range <<= 8;
			assert (range & (TOPTOP-1)) == range: "range is out of bounds!";
			low <<= 8;
			low &= (TOPTOP - 1);
			// если была хитрая коррекция выше тогда low+range==BOTTOM и после сдвига <<8, log+range==TOP, поэтому в assert добавлена проверка на ==1 это валидное значение
			assert (((low+range) >>> HIGHBYTE) == 0) || (((low+range) >>> HIGHBYTE) == 1):"low+range is out of bounds2!";
			outByte = ((low ^ low + range) < TOP);
		}

		logger.finest(()->String.format("AFTER  scale sym=%X, low=%X high=%X, range=%X", sym, low, low + range, range));
	}

	private void writeLastBytes() throws IOException
	{
		low = low + (range>>1); // need value INSIDE the interval, take a middle
		for (int i = CODEBITS; i >= 0; i -= 8)
		{
			int ob = (int) ((low >> i) & 0xFF);
			writeByte(ob);
			logger.finest(()->String.format("Output byte: 0x%X, count: %,d", ob, encodedBytes));
		}
	}

	public void writeByte(int b) throws IOException
	{
		cData.sout.write(b);
		encodedBytes++;
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
		if((cData.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) && (total > threshold))
		{
			threshold += delta;
			cData.cb.heartBeat((int)(100* threshold /cData.sizeUncompressed));
		}
	}

	private void finishProgress()
	{
		if(cData.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) cData.cb.finish();
	}

	private void startProgress()
	{
		if(cData.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) cData.cb.start();
	}


}