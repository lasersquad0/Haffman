import java.io.*;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RLEUncompressor
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	UncompressData uData;
	CRC32 crc = new CRC32();
	long decodedBytes = 0;
	final int SHOW_PROGRESS_AFTER = 1_000_000; // display progress only if file size is larger then this

	public void uncompress(UncompressData uData) throws IOException
	{
		decodedBytes = 0;
		this.uData = uData;

		uncompressInternal();

		uData.CRC32Value = crc.getValue();
	}

	public void uncompressInternal() throws IOException
	{
		// старший бит=1 (|0x80)- означает что далее идут одинаковые символы.
		// (старший бит=0)- означает что далее идет последовательность неодинаковых символов
		// 129 - макс длина цепочки одинаковых символов. Отнимаем 2 от реальной длины
		// 128 - макс длина цепочки неодинаковых символов. Отнимаем 1 от реальной длины. Два одинаковых подряд включаем в неодинаковые.

		if(uData.sizeCompressed == 0) // for support of zero-length files
			return;

		final int EQUALS_MAXLEN = 129;
		final int MAX_BUF_SIZE= 100_000_000;
		int WORK_BUFFER = (uData.sizeCompressed < (long)MAX_BUF_SIZE) ? (int)uData.sizeCompressed : MAX_BUF_SIZE;

		if(uData.sizeCompressed > SHOW_PROGRESS_AFTER) uData.cb.start();

		long threshold = 0;
		long delta = uData.sizeCompressed/100;

		byte[] buf = new byte[WORK_BUFFER];
		int cntRead;
		int i = 0;

		cntRead = uData.sin.read(buf, 0, (int)Math.min((long)buf.length, uData.sizeCompressed));
		if(cntRead == -1) return; // cannot read anything from the stream

		assert cntRead == (int)Math.min((long)buf.length, uData.sizeCompressed);

		boolean last = cntRead < buf.length; // if we've read less than buf.length symbols, it means there are no more symbols in the stream
		long encodedBytes = cntRead;

		while(true/*encodedBytes < uData.sizeCompressed*/)
		{
			if ((uData.sizeCompressed > SHOW_PROGRESS_AFTER) && (encodedBytes > threshold))
			{
				threshold += delta;
				uData.cb.heartBeat((int) (100 * threshold / uData.sizeCompressed));
			}

			byte code = buf[i];
			if(Byte.toUnsignedInt(code) > 127)
			{ // equal bytes here
				int num = (code & 0x7F) + 2;
				for (int j = 0; j < num ; j++) { uData.sout.write(buf[i + 1]); crc.update(buf[i + 1]);}  // saving code+2 equal bytes
				i += 2;
			}
			else
			{  // unequal bytes here
				assert i + 1 < buf.length;

				if(i + 1 + code >= buf.length)
				{
					logger.warning(String.format("WARNING! (i + 1 + code >= buf.length) i=%d code=%d buf.length=%d encodedbytes=%d compressedSize=%d", i, code, buf.length, encodedBytes, uData.sizeCompressed));
				}

				assert i + 1 + code < buf.length;

				uData.sout.write(buf, i + 1, code + 1);
				crc.update(buf, i + 1, code + 1);
				i += code + 1 + 1;
			}

			if(i == cntRead) break;

			if((!last) && (i > cntRead - EQUALS_MAXLEN)) // too low bytes in the buf, reading more
			{
				assert encodedBytes <= uData.sizeCompressed;

				int remaining = cntRead - i;
				int toRead = (int) Math.min((long) (buf.length - remaining), (long)(uData.sizeCompressed - encodedBytes));

				assert toRead >= 0;
				last = (uData.sizeCompressed - encodedBytes) < (long) (buf.length - remaining);

				if(toRead > 0)
				{
					System.arraycopy(buf, i, buf, 0, remaining);
					cntRead = uData.sin.read(buf, remaining, toRead);

					assert cntRead != -1;
					assert cntRead == toRead;

					encodedBytes += cntRead;
					i = 0;
					cntRead = (cntRead == -1) ? remaining : cntRead + remaining;
				}
			}

		}

		uData.sout.flush();

		if(uData.sizeCompressed > SHOW_PROGRESS_AFTER) uData.cb.finish();

	}

}
