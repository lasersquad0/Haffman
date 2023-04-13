import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RangeAdaptCompressor extends Compressor implements BlockCompressable, BlockUncompressable
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private int CODEBITS = 24;
	private int HIGHBYTE = CODEBITS + 8;
	private long TOP = 1L << CODEBITS;
	private long TOPTOP = 1L << (CODEBITS + 8);
	long BOTTOM = 1L << (CODEBITS - 8);
	private long low;
	private long range;
	private long nextChar;
	private final ModelOrder0Adapt[] models = new ModelOrder0Adapt[256];
	private final CRC32 crc = new CRC32();
	private long outputBytes;
	private long inputBytes;
	private int blockNum;


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
		startProgress((cdata.sizeUncompressed - 1)/Utils.BLOCK_SIZE + 1); // calculate number of blocks
	}

	@Override
	public void finishBlockCompressing()
	{
		finishCompressing();
	}

	private void startCompressing(CompressData ccData)
	{
		this.cdata = ccData;
		for (int i = 0; i < models.length; i++)
		{
			models[i] = new ModelOrder0Adapt(BOTTOM);
		}

		assert cdata.sizeUncompressed > 0;

		outputBytes = 0;
		inputBytes = 0;
		blockNum = 0;

		resetLowAndRange();

		//logger.finest(()->String.format("weights=%s", Arrays.toString(model.weights)));
		logger.finest(()->String.format("low=%X high=%X, range=%X", low, low + range, range));
	}

	private void finishCompressing()
	{
		finishProgress();

		cdata.sizeCompressed = outputBytes;
		cdata.CRC32Value = crc.getValue();
	}

	@Override
	public void compressBlock(BlockCoderData data) throws IOException
	{
		long save = outputBytes;

		updateProgress(blockNum++);

		int prevSym = 0;
		for (int i = 0; i < data.bytesInBlock; i++)
		{
			int sym = Byte.toUnsignedInt(data.srcblock[i]);
			crc.update(sym); // calculate CRC32 of *Uncompressed* file here

			encodeSymbol(sym, models[prevSym]);
			prevSym = sym;
		}

		writeLastBytes(); // additionally resets low and range variables to initial values

		data.cBlockSize = (int)(outputBytes - save);
	}

	public void compress(CompressData ccData) throws IOException
	{
		startCompressing(ccData);
		startProgress(cdata.sizeUncompressed);

		int ch, prevSym = 0;
		while((ch = cdata.sin.read()) != -1)
		{
			crc.update(ch); // calculate CRC32 of *Uncompressed* file here
			updateProgress(inputBytes++);

			encodeSymbol(ch, models[prevSym]);
			prevSym = ch;
		}

		writeLastBytes();

		cdata.sout.flush();

		finishCompressing();
	}

	private void encodeSymbol(int sym, ModelOrder0 model) throws IOException
	{
		long[] res = model.SymbolToFreqRange(sym);
		long left = res[0];
		long freq = res[1];

		assert(left + freq <= model.totalFreq);
		assert(freq > 0);
		assert(model.totalFreq <= BOTTOM);

		range = range / model.totalFreq;
		low = low + left * range;
		range = freq * range;

		//logger.finest(()->String.format("before scale sym=%X, low=%X high=%X, range=%X", sym, low, low + range, range));
		scaleC();
		//logger.finest(()->String.format("AFTER  scale sym=%X, low=%X high=%X, range=%X", sym, low, low + range, range));

		model.updateStatistics(sym);
	}

	private void scaleC() throws IOException
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

			writeByteC((int)(low >>> CODEBITS));
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

	private void startUncompressing(CompressData uData)
	{
		this.cdata = uData;
		for (int i = 0; i < models.length; i++)
		{
			models[i] = new ModelOrder0Adapt(BOTTOM);
		}

		if(uData.sizeCompressed == 0) // for support of zero-length files
			return;

		outputBytes = 0;
		blockNum = 0;

		resetLowAndRange();

		//logger.finer(()->String.format("weights=%s", Arrays.toString(model.weights)));
		logger.finer(()->String.format("low=%X high=%X, range=%X", low, low + range, range));
	}

	private void finishUncompressing()
	{
		finishProgress();

		cdata.CRC32Value = crc.getValue();
	}

	private void resetLowAndRange()
	{
		low = 0;
		range = TOPTOP - 1; // сразу растягиваем на максимальный range
	}


	private void writeLastBytes() throws IOException
	{
		low = low + (range>>1); // need value INSIDE the interval, take a middle
		for (int i = CODEBITS; i >= 0; i -= 8)
		{
			int ob = (int) ((low >> i) & 0xFF);
			writeByteC(ob);
			logger.finest(()->String.format("compress output byte: %X", ob));
		}

		resetLowAndRange();
	}

	@Override
	public void startBlockUncompressing(CompressData uData)
	{
		startUncompressing(uData);
		startProgress((uData.sizeUncompressed - 1)/Utils.BLOCK_SIZE + 1);

	}

	@Override
	public void finishBlockUncompressing()
	{
		finishUncompressing();
	}

	@Override
	public void uncompressBlock(BlockCoderData data) throws IOException
	{
		data.resetDest();
		nextChar = loadInitialBytes();

		updateProgress(blockNum++);

		int prevSym = 0;
		for (int i = 0; i < data.uBlockSize; i++)
		{
			int symbol = decodeSymbol(models[prevSym]);
			crc.update(symbol);
			data.writeDest(symbol);
			outputBytes++;
			prevSym = symbol;
		}

		resetLowAndRange();
	}
	public void uncompress(CompressData uData) throws IOException
	{
		startUncompressing(uData);
		startProgress(cdata.sizeUncompressed);

		nextChar = loadInitialBytes();

		int prevSym = 0;
		while(outputBytes < cdata.sizeUncompressed)
		{
			updateProgress(outputBytes);

			int sym = decodeSymbol(models[prevSym]);
			writeByteU(sym);
			prevSym = sym;
		}

		cdata.sout.flush();

		finishUncompressing();
	}

	private long loadInitialBytes() throws IOException
	{
		long ch = 0;
		for (int i = 0; i < HIGHBYTE>>>3; i++) // assumed we have more than X compressed bytes in a stream
		{
			long ci = cdata.sin.read();
			assert ci != -1;
			ch = (ch << 8) | ci;
		}

		return ch;
	}

	private int decodeSymbol(ModelOrder0 model) throws IOException
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

		//logger.finest(()->String.format("before scale sym=%X, low=%X high=%X, range=%X", nextChar, low, low + range, range));
		scaleU();
		//logger.finest(()->String.format("after  scale sym=%X, low=%X high=%X, range=%X", nextChar, low, low + range, range));

		model.updateStatistics(sym);

		return sym;
	}

	private void scaleU() throws IOException
	{
		boolean outByte = ((low ^ low + range) < TOP);
		while( outByte || (range < BOTTOM) )
		{
			if((!outByte) && (range < BOTTOM)) // делает low+range=TOPTOP-1 если low+range было >TOPTOP-1
			{
				//logger.finest(()->String.format("Weird range correction:BEFORE range:%X, BOTTOM: %X", range, BOTTOM));
				range = BOTTOM - (low & (BOTTOM - 1));
				//logger.finest(()->String.format("Weird range correction:AFTER range:%X, BOTTOM: %X", range, BOTTOM));
				assert range > 0;
				assert (((low+range) >>> HIGHBYTE) == 0) || (((low+range) >>> HIGHBYTE) == 1):"low+range is out of bounds3!";
			}

			int ch = cdata.sin.read();
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

	public void writeByteC(int b) throws IOException
	{
		cdata.sout.write(b);
		outputBytes++;
	}

	public void writeByteU(int sym) throws IOException
	{
		cdata.sout.write(sym);
		crc.update(sym);
		outputBytes++;
		logger.finest(()->String.format("uncompress output byte: 0x%X, count: %,d", sym, outputBytes));
	}
}