import java.io.IOException;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RLECompressor {

	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	CompressData cData;
	CRC32 crc = new CRC32();
	public long encodedBytes = 0;
	final long SHOW_PROGRESS_AFTER = 1_000_000; // display progress only if file size is larger then this
	final int EQUALS_MAXLEN = 129;
	final int UNEQUALS_MAXLEN = 128;

	public void compress(CompressData cData) throws IOException
	{
		// старший бит=1 (|0x80)- означает что далее идут одинаковые символы.
		// (старший бит=0)- означает что далее идет последовательность неодинаковых символов
		// 129 - макс длина цепочки одинаковых символов. Отнимаем 2 от реальной длины
		// 128 - макс длина цепочки неодинаковых символов. Отнимаем 1 от реальной длины. Два одинаковых подряд включаем в неодинаковые.

		this.cData = cData;
		encodedBytes = 0;  // сбрасываем счетчики

		assert cData.sizeUncompressed > 0;

		final int MAX_BUF_SIZE= 100_000_000;
		int WORK_BUFFER = (cData.sizeUncompressed < (long)MAX_BUF_SIZE) ? (int)cData.sizeUncompressed : MAX_BUF_SIZE;

		if(cData.sizeUncompressed > SHOW_PROGRESS_AFTER) cData.cb.start();

		long threshold = 0;
		long delta = cData.sizeUncompressed/100;

		byte[] buf = new byte[WORK_BUFFER];
		int cntRead;
		int head = 0;
		int i = 0;

		cntRead = cData.sin.read(buf);
		if(cntRead == -1) return; // cannot read anything from the stream
		crc.update(buf);

		long total = cntRead;
		boolean last = cntRead < buf.length; // if we've read less than buf.length symbols, it means there are no more symbols in the stream

		while(true)
		{
			if ((cData.sizeUncompressed > SHOW_PROGRESS_AFTER) && (total > threshold))
			{
				threshold += delta;
				cData.cb.heartBeat((int) (100 * threshold / cData.sizeUncompressed));
			}


			while ((i - head < UNEQUALS_MAXLEN) && (i < cntRead - 2))
			{
				if ((buf[i] == buf[i + 1]) && (buf[i] == buf[i + 2]))  // we've found three equal bytes in a row
					break;
				i++;
			} // i refers to first symbol of the NEXT group


			if((i >= cntRead - 2) && (i - head + 2 <= UNEQUALS_MAXLEN))
				i = cntRead;

			assert i - head <= UNEQUALS_MAXLEN;

			if (i > head)   // coding sequence of NON equal bytes
			{
				int count = (i - head - 1);  // do not forget to add 1 during decoding
				assert count >= 0;
				total += count + 1;
				cData.sout.write(count);
				cData.sout.write(buf, head, i - head);
				encodedBytes += 1 + i - head;
				head = i;
			}

			assert i <= cntRead;

			if(i == cntRead)
				break;

			while ((i < cntRead - 1) && (buf[i] == buf[i + 1]) && (i - head + 1 < EQUALS_MAXLEN))
			{
				i++;
			} // i refers to the LAST symbol of the current group (except for i-head+1>=EQUALS_MAXLEN)

			assert i - head <= EQUALS_MAXLEN;

			if (i - head >= 2) // coding sequence of equal bytes. 2 equal bytes in a row is not considered as equal seq.
			{
				int count = (i - head + 1 - 2);  // do not forget to add 2 during decoding
				assert count >= 0;
				total += count + 2;
				count |= 0x80;    // set major bit
				cData.sout.write(count);
				cData.sout.write(buf[i]);   // saving just one repeated symbol
				encodedBytes += 2;
				i = head = i + 1;
			}
			else i = head; // стопнули на UNEQUALS_MAXLEN в первом цифле и так оказалось что следующие 2 байта (только 2) одинаковые тогда портится i и нужно его вернуть на head
			// другое решение это вверху подправить условие на (i - head >= 1) т.о. в таких редких случаях мы будем записывать 2 одинаковых элемента как equal seq.

			assert i <= cntRead;

			if((!last) && (i > cntRead - EQUALS_MAXLEN)) // too low bytes in the buf, reading more
			{
				int remaining = cntRead - i;
				System.arraycopy(buf, i, buf,0, remaining);
				cntRead = cData.sin.read(buf, remaining, buf.length - remaining);
				crc.update(buf, remaining, buf.length - remaining);

				head = 0;
				i = 0;

				cntRead = (cntRead == -1) ? remaining : cntRead + remaining;
				last = (cntRead < buf.length - remaining);
			}

			if(i == cntRead)
				break;

		}

		cData.sout.flush();

		if(cData.sizeUncompressed > SHOW_PROGRESS_AFTER) cData.cb.finish();

		cData.sizeCompressed = encodedBytes;
		cData.CRC32Value = crc.getValue();
	}

}
