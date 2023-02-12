import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RangeCompressor {
	public enum Strategy { HIGHBYTE, RANGEBOTTOM }
	private final static Logger logger = Logger.getLogger("HFLogger");
	private long low; //, high;
	private long range;
	final int CODEBITS = 32;
	final int HIGHBYTE = CODEBITS - 8;
	final long TOP = 1L << CODEBITS;
	final long BOTTOM = 1 << (CODEBITS - 16);
//	final long TOPBOTTOM = 1 << (CODEBITS - 8);
	final long BIGBYTE = 0xFFL << (CODEBITS - 16);
	final long MASK2 = 0x00000000FFFFFFFFL;
	private RangeCompressData cData;
	private final CRC32 crc = new CRC32();
	public long encodedBytes = 0;
	final long SHOW_PROGRESS_AFTER = 1_000_000; // display progress only if file size is larger then this
	private Strategy strategy; // you can use highbyte(low)==highbyte(high) strategy or range<BOTTOM (strategy==2)


	RangeCompressor(Strategy strategy)
	{
		this.strategy = strategy;
	}
	RangeCompressor()
	{
		this(Strategy.HIGHBYTE);
	}

	public void compress(CompressData ccData) throws IOException
	{
		this.cData = (RangeCompressData) ccData;
		encodedBytes = 0;  // сбрасываем счетчики

		assert cData.sizeUncompressed > 0;

		if(cData.sizeUncompressed > SHOW_PROGRESS_AFTER) cData.cb.start();

		long threshold = 0;
		long delta = cData.sizeUncompressed/100;

		low = 0;
		range = TOP - 1; // сразу растягиваем на максимальный range
		long totalFreq = cData.cumFreq[cData.cumFreq.length - 1];

		logger.finer(String.format("freq=%s", Arrays.toString(cData.cumFreq)));
		logger.finer(String.format("low=%x high=%x, range=%x", low, low + range, range));

		long total = 0;
		int ch;
		while((ch = cData.sin.read()) != -1)
		{
			if((cData.sizeUncompressed > SHOW_PROGRESS_AFTER) && (total > threshold))
			{
				threshold += delta;
				cData.cb.heartBeat((int)(100*threshold/cData.sizeUncompressed));
			}
			total++;

			crc.update(ch);

			int index = cData.symbol_to_freq_index[ch];
			long left = cData.cumFreq[index - 1];
			long right = cData.cumFreq[index];

			low = low + ((range+1) * left) / totalFreq;
			range = ((range+1) * (right - left)) / totalFreq - 1;

			logger.finer(String.format("before scale sym=%X, low=%X high=%X, range=%X", ch, low, low + range, range));

			switch (strategy)
			{
				case HIGHBYTE -> scale();
				case RANGEBOTTOM -> scale2();
			}

			logger.finer(String.format("AFTER  scale sym=%X, low=%X high=%X, range=%X", ch, low, low + range, range));
		}

		low = low + range/2; // need value INSIDE the interval, take a middle
		cData.sout.write((int)(low >>> 24));
		encodedBytes++;
		cData.sout.write((int) ((low >>> 16) & 0xFF));
		encodedBytes++;
		cData.sout.write((int) ((low >>> 8) & 0xFF));
		encodedBytes++;
		cData.sout.write((int) (low & 0xFF));
		encodedBytes++;

		logger.finer(String.format("compress output byte: %X", low >>> 24));
		logger.finer(String.format("compress output byte: %X", (low >>> 16) & 0xFF));
		logger.finer(String.format("compress output byte: %X", (low >>> 8) & 0xFF));
		logger.finer(String.format("compress output byte: %X", low & 0xFF));

		cData.sout.flush();

		if(cData.sizeUncompressed > SHOW_PROGRESS_AFTER) cData.cb.finish();

		cData.sizeCompressed = encodedBytes;
		cData.CRC32Value = crc.getValue();
	}

	private void scale() throws IOException
	{
		//long range = high - low;
		long highByte = ((low ^ (low + range)) >>> HIGHBYTE);
		while( (highByte == 0 || (range < BOTTOM)) )
		{
			//assert (range + (low & BOTTOM-1) < BOTTOM);
			//assert (low & BIGBYTE) != BIGBYTE : "Highest byte in 'low' equals 0xFF!";

			if( ((low & BIGBYTE) == BIGBYTE) && (range + (low & (BOTTOM-1)) >= BOTTOM) )
				range = BOTTOM - (low & (BOTTOM-1));

			cData.sout.write((int) (low >>> HIGHBYTE));
			encodedBytes++;
			logger.finer(String.format("Strategy1: output byte: 0x%X, count: %,d", low >>> 24, encodedBytes));

			//assert ((low ^ (low + range)) >>> HIGHBYTE) == 0 : "low/high highest bytes are no equal"; // check that highest byte is the same in high and low

			range = range << 8;
			low = (low << 8) & MASK2;

			assert (range & MASK2) == range;
			assert low + range < TOP : "'high' variable becomes more than 2^32! Overflow.";

			highByte = ((low ^ (low + range)) >>> HIGHBYTE);
		}
		//high = low + range;


	/*	while(true) // может у нас уже 2 неменяющихся байта или 3, всех их кидаем в выходной поток.
		{
			long tmp = high & MASK;
			if ((tmp ^ (low & MASK)) == 0)  // если 5й байт у low и high одинаков, то он уже не будет меняться и его можно вытянуть в выходной поток
			{
				int outputByte = (int) (tmp >>> 32);
				logger.warning(String.format("output byte: %d", outputByte));
				high &= MASK2;   // обнуляем 5й байт в high and low и расширяем range в 2^8=256 раз
				high <<= 8;
				high |= MASK3;  // set all new bits to '1'
				low &= MASK2;
				low <<= 8;
			}
			else break;
		}
	 */
	}

	private void scale2() throws IOException
	{
		//while(range < BOTTOM)
		if(range < BOTTOM)
		{
			assert range > 0: "*** range == 0 ***, abnormal, stopping!";

			if( ((low & BIGBYTE) == BIGBYTE) && (range + (low & (BOTTOM-1)) >= BOTTOM) )
				range = BOTTOM - (low & (BOTTOM-1));

			cData.sout.write((int) (low >>> HIGHBYTE));
			encodedBytes++;
			logger.finer(String.format("Strategy2: output byte: 0x%X, count: %d", low >>> 24, encodedBytes));

			//assert ((low ^ (low + range)) >>> HIGHBYTE) == 0 : "low/high highest bytes are no equal"; // check that highest byte is the same in high and low

			range = range << 8;
			low = (low << 8) & MASK2;

			assert (range & MASK2) == range;
			assert low + range < TOP : "'high' variable becomes more than 2^32! Overflow.";
		}
	}

}