import java.io.IOException;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RLECompressor implements BlockCompressable{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	CompressData cData;
	private final CRC32 crc = new CRC32();
	private long outputBytes;
	private long inputBytes;
	final int EQUALS_MAXLEN = 129;
	final int UNEQUALS_MAXLEN = 128;
	private byte[] buffer;

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
		//int cntRead;
		int head = 0;
		int i = 0;

		long save = outputBytes;

		//int cntRead = cData.sin.read(buffer);
		//if(cntRead == -1) return; // cannot read anything from the stream
		int cntRead = data.bytesInBlock;
		byte[] buf = data.srcblock;

		crc.update(buf);
		inputBytes += cntRead;
		//boolean last = cntRead < buffer.length; // if we've read less than buf.length symbols, it means there are no more symbols in the stream

		while(true)
		{
			updateProgress(inputBytes);

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
				//total += count + 1;
				cData.sout.write(count);
				cData.sout.write(buf, head, i - head);
				outputBytes += 1 + i - head;
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
				//total += count + 2;
				count |= 0x80;    // set major bit
				cData.sout.write(count);
				cData.sout.write(buf[i]);   // saving just one repeated symbol
				outputBytes += 2;
				i = head = i + 1;
			}
			else i = head; // стопнули на UNEQUALS_MAXLEN в первом цикле и так оказалось что следующие 2 байта (только 2) одинаковые тогда портится i и нужно его вернуть на head
			// другое решение это вверху подправить условие на (i - head >= 1) т.о. в таких редких случаях мы будем записывать 2 одинаковых элемента как equal seq.

			assert i <= cntRead;

	/*		if((!last) && (i > cntRead - EQUALS_MAXLEN)) // not enough bytes in the buf, reading more
			{
				int remaining = cntRead - i;
				System.arraycopy(buffer, i, buf,0, remaining);
				cntRead = cData.sin.read(buffer, remaining, buffer.length - remaining);
				if(cntRead !=-1) crc.update(buffer, remaining, Math.min(buffer.length - remaining, cntRead));

				head = 0;
				i = 0;

				cntRead = (cntRead == -1) ? remaining : cntRead + remaining;
				last = (cntRead < buffer.length - remaining);
			}
*/
			if(i == cntRead)
				break;
		}

		data.cBlockSize = (int)(outputBytes - save);
	}

	public void compress(CompressData ccData) throws IOException
	{
		// старший бит=1 (|0x80)- означает что далее идут одинаковые символы.
		// (старший бит=0)- означает что далее идет последовательность неодинаковых символов
		// 129 - макс длина цепочки одинаковых символов. Отнимаем 2 от реальной длины
		// 128 - макс длина цепочки неодинаковых символов. Отнимаем 1 от реальной длины. Два одинаковых подряд включаем в неодинаковые.

		startCompressing(ccData);

		// it's easier to have working buffer = BLOCK_SIZE
		//final int MAX_BUF_SIZE= 100_000_000;
		int WORK_BUFFER = (cData.sizeUncompressed < (long) Utils.BLOCK_SIZE) ? (int) cData.sizeUncompressed : Utils.BLOCK_SIZE;

		if( (buffer == null) || (buffer.length < WORK_BUFFER))
		{
			buffer = new byte[WORK_BUFFER];
		}

		int cntRead;
		int head = 0;
		int i = 0;

		cntRead = cData.sin.read(buffer);
		if(cntRead == -1) return; // cannot read anything from the stream
		crc.update(buffer);

		long total = cntRead;
		boolean last = cntRead < buffer.length; // if we've read less than buf.length symbols, it means there are no more symbols in the stream

		while(true)
		{
			updateProgress(total);

			while ((i - head < UNEQUALS_MAXLEN) && (i < cntRead - 2))
			{
				if ((buffer[i] == buffer[i + 1]) && (buffer[i] == buffer[i + 2]))  // we've found three equal bytes in a row
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
				cData.sout.write(buffer, head, i - head);
				outputBytes += 1 + i - head;
				head = i;
			}

			assert i <= cntRead;

			if(i == cntRead)
				break;

			while ((i < cntRead - 1) && (buffer[i] == buffer[i + 1]) && (i - head + 1 < EQUALS_MAXLEN))
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
				cData.sout.write(buffer[i]);   // saving just one repeated symbol
				outputBytes += 2;
				i = head = i + 1;
			}
			else i = head; // стопнули на UNEQUALS_MAXLEN в первом цикле и так оказалось что следующие 2 байта (только 2) одинаковые тогда портится i и нужно его вернуть на head
			// другое решение это вверху подправить условие на (i - head >= 1) т.о. в таких редких случаях мы будем записывать 2 одинаковых элемента как equal seq.

			assert i <= cntRead;

			if((!last) && (i > cntRead - EQUALS_MAXLEN)) // not enough bytes in the buf, reading more
			{
				int remaining = cntRead - i;
				System.arraycopy(buffer, i, buffer,0, remaining);
				cntRead = cData.sin.read(buffer, remaining, buffer.length - remaining);
				if(cntRead !=-1) crc.update(buffer, remaining, Math.min(buffer.length - remaining, cntRead));

				head = 0;
				i = 0;

				cntRead = (cntRead == -1) ? remaining : cntRead + remaining;
				last = (cntRead < buffer.length - remaining);
			}

			if(i == cntRead)
				break;
		}

		finishCompressing();
	}

	private void finishCompressing() throws IOException
	{
		cData.sout.flush();

		finishProgress();

		cData.sizeCompressed = outputBytes;
		cData.CRC32Value = crc.getValue();
	}

	private void startCompressing(CompressData ccData)
	{
		this.cData = ccData;
		outputBytes = 0;  // сбрасываем счетчики
		inputBytes = 0;
		delta = cData.sizeUncompressed/100;

		assert cData.sizeUncompressed > 0;

		startProgress();
	}

	long threshold = 0;
	long delta;
	//final long SHOW_PROGRESS_AFTER = 1_000_000; // display progress only if file size is larger then this
	private void updateProgress(long total)
	{
		if ((cData.sizeUncompressed > Utils.SHOW_PROGRESS_AFTER) && (total > threshold))
		{
			threshold += delta;
			cData.cb.heartBeat((int) (100 * threshold / cData.sizeUncompressed));
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
