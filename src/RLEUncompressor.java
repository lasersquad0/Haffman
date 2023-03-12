import java.io.*;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RLEUncompressor implements BlockUncompressable
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	CompressData uData;
	private final CRC32 crc = new CRC32();
	private long decodedBytes;
	private byte[] buffer;

	@Override
	public void startBlockUncompressing(CompressData cData)
	{
		startUncompressing(cData);

		int WORK_BUFFER = (uData.sizeCompressed < (long)Utils.BLOCK_SIZE) ? (int)uData.sizeCompressed : Utils.BLOCK_SIZE;
		if( (buffer == null) || (buffer.length < WORK_BUFFER))
		{
			buffer = new byte[WORK_BUFFER];
		}
	}

	@Override
	public void finishBlockUncompressing()
	{
		finishUncompressing();
	}

	@Override
	public void uncompressBlock(BlockCoderData data)
	{
		// старший бит=1 (|0x80)- означает что далее идут одинаковые символы.
		// (старший бит=0)- означает что далее идет последовательность неодинаковых символов
		// 129 - макс длина цепочки одинаковых символов. Отнимаем 2 от реальной длины
		// 128 - макс длина цепочки неодинаковых символов. Отнимаем 1 от реальной длины. Два одинаковых подряд включаем в неодинаковые.

		//final int EQUALS_MAXLEN = 129;

		int i = 0;

		//int cntRead = uData.sin.read(buffer, 0, (int)Math.min((long)buffer.length, uData.sizeCompressed));
		//if(cntRead == -1) return; // cannot read anything from the stream
		//assert cntRead == (int)Math.min((long)buffer.length, uData.sizeCompressed);

		buffer = data.srcblock;
		int cntRead = data.bytesInBlock;

		//boolean last = cntRead < buffer.length; // if we've read less than buffer.length symbols, it means there are no more symbols in the stream
		long encodedBytes = cntRead;
		data.resetDest();

		while(true/*encodedBytes < uData.sizeCompressed*/)
		{
			updateProgress(encodedBytes);

			byte code = buffer[i];
			if(Byte.toUnsignedInt(code) > 127)
			{ // equal bytes here
				int num = (code & 0x7F) + 2;
				for (int j = 0; j < num ; j++) { data.writeDest(buffer[i + 1]); crc.update(buffer[i + 1]);}  // saving code+2 equal bytes
				i += 2;
			}
			else
			{  // unequal bytes here
				assert i + 1 < buffer.length;

				if(i + 1 + code >= buffer.length)
				{
					logger.warning(String.format("WARNING! (i + 1 + code >= buffer.length) i=%d code=%d buffer.length=%d encodedbytes=%d compressedSize=%d", i, code, buffer.length, encodedBytes, uData.sizeCompressed));
				}

				assert i + 1 + code < buffer.length;

				data.writeDest(buffer, i + 1, code + 1);
				crc.update(buffer, i + 1, code + 1);
				i += code + 1 + 1;
			}

			if(i == cntRead) break;

	/*		if((!last) && (i > cntRead - EQUALS_MAXLEN)) // too low bytes in the buffer, reading more
			{
				assert encodedBytes <= uData.sizeCompressed;

				int remaining = cntRead - i;
				int toRead = (int) Math.min((long) (buffer.length - remaining), (long)(uData.sizeCompressed - encodedBytes));

				assert toRead >= 0;
				last = (uData.sizeCompressed - encodedBytes) < (long) (buffer.length - remaining);

				if(toRead > 0)
				{
					System.arraycopy(buffer, i, buffer, 0, remaining);
					cntRead = uData.sin.read(buffer, remaining, toRead);

					assert cntRead != -1;
					assert cntRead == toRead;

					encodedBytes += cntRead;
					i = 0;
					cntRead = (cntRead == -1) ? remaining : cntRead + remaining;
				}
			}*/
		}
	}

	public void uncompress(CompressData uuData) throws IOException
	{
		startUncompressing(uuData);
		if(uData.sizeCompressed == 0) return; // for support of zero-length files

		uncompressInternal();

		uData.sout.flush();
		finishUncompressing();
	}

	private void finishUncompressing()
	{
		finishProgress();
		uData.CRC32Value = crc.getValue();
	}

	private void startUncompressing(CompressData uuData)
	{
		this.uData = uuData;

		decodedBytes = 0;
		delta = uData.sizeCompressed/100;

		startProgress();
	}

	public void uncompressInternal() throws IOException
	{
		// старший бит=1 (|0x80)- означает что далее идут одинаковые символы.
		// (старший бит=0)- означает что далее идет последовательность неодинаковых символов
		// 129 - макс длина цепочки одинаковых символов. Отнимаем 2 от реальной длины
		// 128 - макс длина цепочки неодинаковых символов. Отнимаем 1 от реальной длины. Два одинаковых подряд включаем в неодинаковые.

		final int EQUALS_MAXLEN = 129;
		//final int MAX_BUF_SIZE= 100_000_000;
		int WORK_BUFFER = (uData.sizeCompressed < (long)Utils.BLOCK_SIZE) ? (int)uData.sizeCompressed : Utils.BLOCK_SIZE;

		if( (buffer == null) || (buffer.length < WORK_BUFFER))
		{
			buffer = new byte[WORK_BUFFER];
		}

		int i = 0;

		int cntRead = uData.sin.read(buffer, 0, (int)Math.min((long)buffer.length, uData.sizeCompressed));
		if(cntRead == -1) return; // cannot read anything from the stream

		assert cntRead == (int)Math.min((long)buffer.length, uData.sizeCompressed);

		boolean last = cntRead < buffer.length; // if we've read less than buffer.length symbols, it means there are no more symbols in the stream
		long encodedBytes = cntRead;

		while(true/*encodedBytes < uData.sizeCompressed*/)
		{
			updateProgress(encodedBytes);

			byte code = buffer[i];
			if(Byte.toUnsignedInt(code) > 127)
			{ // equal bytes here
				int num = (code & 0x7F) + 2;
				for (int j = 0; j < num ; j++) { uData.sout.write(buffer[i + 1]); crc.update(buffer[i + 1]);}  // saving code+2 equal bytes
				i += 2;
			}
			else
			{  // unequal bytes here
				assert i + 1 < buffer.length;

				if(i + 1 + code >= buffer.length)
				{
					logger.warning(String.format("WARNING! (i + 1 + code >= buffer.length) i=%d code=%d buffer.length=%d encodedbytes=%d compressedSize=%d", i, code, buffer.length, encodedBytes, uData.sizeCompressed));
				}

				assert i + 1 + code < buffer.length;

				uData.sout.write(buffer, i + 1, code + 1);
				crc.update(buffer, i + 1, code + 1);
				i += code + 1 + 1;
			}

			if(i == cntRead) break;

			if((!last) && (i > cntRead - EQUALS_MAXLEN)) // too low bytes in the buffer, reading more
			{
				assert encodedBytes <= uData.sizeCompressed;

				int remaining = cntRead - i;
				int toRead = (int) Math.min((long) (buffer.length - remaining), (long)(uData.sizeCompressed - encodedBytes));

				assert toRead >= 0;
				last = (uData.sizeCompressed - encodedBytes) < (long) (buffer.length - remaining);

				if(toRead > 0)
				{
					System.arraycopy(buffer, i, buffer, 0, remaining);
					cntRead = uData.sin.read(buffer, remaining, toRead);

					assert cntRead != -1;
					assert cntRead == toRead;

					encodedBytes += cntRead;
					i = 0;
					cntRead = (cntRead == -1) ? remaining : cntRead + remaining;
				}
			}

		}

	}

	long threshold = 0;
	long delta;
	private void updateProgress(long encodedBytes)
	{
		if ((uData.sizeCompressed > Utils.SHOW_PROGRESS_AFTER) && (encodedBytes > threshold))
		{
			threshold += delta;
			uData.cb.heartBeat((int) (100 * threshold / uData.sizeCompressed));
		}
	}
	private void startProgress()
	{
		if(uData.sizeCompressed > Utils.SHOW_PROGRESS_AFTER) uData.cb.start();
	}

	private void finishProgress()
	{
		if(uData.sizeCompressed > Utils.SHOW_PROGRESS_AFTER) uData.cb.finish();
	}


}
