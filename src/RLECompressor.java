import java.io.IOException;
import java.util.logging.Logger;

public class RLECompressor {

	private final static Logger logger = Logger.getLogger("HFLogger");
	HFCompressData cData;
	public long encodedBytes = 0;

	public void compress(HFCompressData cData) throws IOException
	{
		// старший бит=1 (|0x80)- означает что далее идут одинаковые символы.
		// (старший бит=0)- означает что далее идет последовательность неодинаковых символов
		// 129 - макс длина цепочки одинаковых символов. Отнимаем 2 от реальной длины
		// 128 - макс длина цепочки неодинаковых символов. Отнимаем 1 от реальной длины. Два одинаковых подряд включаем в неодинаковые.

		final int READ_BUF_SIZE= 135;
		final int EQUALS_MAXLEN = 129;
		final int UNEQUALS_MAXLEN = 128;

		encodedBytes = 0;  // сбрасываем счетчики
		this.cData = cData;

		cData.cb.start();

		long threshold = 0;
		long delta = cData.sizeUncompressed/100;
		long total = 0;

		byte[] buf = new byte[READ_BUF_SIZE];
		int cntRead;
		int head = 0;
		int i = 0;

		cntRead = cData.sin.read(buf);
		if(cntRead == -1) return; // cannot read anything from the stream

		boolean last = cntRead < buf.length; // if we've read less than buf.length symbols, it means there are no more symbols in the stream

		while(true)
		{
			if (total > threshold)
			{
				threshold += delta;
				cData.cb.heartBeat((int) (100 * threshold / cData.sizeUncompressed));
			}

			while ((i - head < UNEQUALS_MAXLEN) && (i < cntRead - 2))
			{
				if ((buf[i] == buf[i + 1]) && (buf[i] == buf[i + 2]))  // we've found three equal bytes in a row
					break;
				i++;
			} // i refers to first symbol of the NEXT group (except for i-head>=UNEQUALS_MAXLEN)

			assert i - head <= UNEQUALS_MAXLEN;

			i = (i >= cntRead - 2) ? cntRead : i; // buffer is over, correcting i

			if (i > head)   // coding sequence of NON equal bytes
			{
				int count = (i - head - 1);  // do not forget to add 1 during decoding
				total += count + 1;
				cData.sout.write(count);
				cData.sout.write(buf, head, i - head);
				encodedBytes += 1 + i - head;
				head = i;
			}

			assert i <= cntRead;

			if(i == cntRead) break;

			while ((buf[i] == buf[i + 1]) && (i - head + 1 < EQUALS_MAXLEN) && (i < cntRead - 1))
			{
				i++;
			} // i refers to the LAST symbol of the current group (except for i-head+1>=EQUALS_MAXLEN)

			assert i - head <= EQUALS_MAXLEN;

			if (i - head >= 2) // coding sequence of equal bytes. 2 equal bytes in a row is not considered as equal seq.
			{
				int count = (i - head + 1 - 2);  // do not forget to add 2 during decoding
				total += count + 2;
				count |= 0x80;    // set major bit
				cData.sout.write(count);
				cData.sout.write(buf[i]);   // saving just one repeated symbol
				encodedBytes += 2;
				i = head = i + 1;
			}

			assert i <= cntRead;

			if(i == cntRead) break;

			if((!last) && (i > cntRead - EQUALS_MAXLEN)) // too low bytes in the buf, reading more
			{
				int remaining = cntRead - i;
				System.arraycopy(buf, i, buf,0, remaining);
				cntRead = cData.sin.read(buf, remaining, buf.length - remaining);

				head = 0;
				i = 0;

				cntRead = (cntRead == -1) ? remaining : cntRead + remaining;

				last = (cntRead < buf.length - remaining);
			}

		}

		cData.sout.flush();

		cData.cb.finish();

		cData.sizeCompressed = encodedBytes;
	}

}