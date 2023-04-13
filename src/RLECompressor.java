import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RLECompressor extends Compressor implements BlockCompressable, BlockUncompressable
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private final CRC32 crc = new CRC32();
	private long outputBytes;
	private long inputBytes;
	private final int EQUALS_MAXLEN = 129;
	private final int UNEQUALS_MAXLEN = 128;
	private byte[] buffer;
	RLECoder coder = new RLECoder();

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

	/*
	public void encodeBlock(BlockCoderData data) throws IOException
	{
		if(tout == null)
			tout = new ByteArrayOutputStream2(data.destblock);
		else
			tout.setNewBuff(data.destblock);

		compressBlockInternal(data, tout);
		data.bytesInBlock = data.cBlockSize;
	}

	private void compressBlockInternal(BlockCoderData data, OutputStream out) throws IOException
	{
		int cntRead = data.bytesInBlock;
		byte[] buf = data.srcblock;

		int compressed = 0;
		int head = 0;
		int i = 0;
		while(true)
		{
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
				out.write(count);
				out.write(buf, head, i - head);
				compressed += 1 + i - head;
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
				out.write(count);
				out.write(buf[i]);   // saving just one repeated symbol
				compressed += 2;
				i = head = i + 1;
			}
			else i = head; // стопнули на UNEQUALS_MAXLEN в первом цикле и так оказалось что следующие 2 байта (только 2) одинаковые тогда портится i и нужно его вернуть на head
			// другое решение это вверху подправить условие на (i - head >= 1) т.о. в таких редких случаях мы будем записывать 2 одинаковых элемента как equal seq.

			assert i <= cntRead;

			if(i == cntRead)
				break;
		}

		data.cBlockSize = compressed;
	}
*/
	@Override
	public void compressBlock(BlockCoderData data) throws IOException
	{
		crc.update(data.srcblock);

		inputBytes += data.bytesInBlock;
		updateProgress(inputBytes);

		//long save = outputBytes;

		coder.encodeBlockAlg(data, cdata.sout);

		//data.cBlockSize = (int)(outputBytes - save);
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
		int WORK_BUFFER = (cdata.sizeUncompressed < (long) Utils.BLOCK_SIZE) ? (int) cdata.sizeUncompressed : Utils.BLOCK_SIZE;

		if( (buffer == null) || (buffer.length < WORK_BUFFER))
		{
			buffer = new byte[WORK_BUFFER];
		}

		int cntRead;
		int head = 0;
		int i = 0;

		cntRead = cdata.sin.read(buffer);
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
				cdata.sout.write(count);
				cdata.sout.write(buffer, head, i - head);
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
				cdata.sout.write(count);
				cdata.sout.write(buffer[i]);   // saving just one repeated symbol
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
				cntRead = cdata.sin.read(buffer, remaining, buffer.length - remaining);
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
		cdata.sout.flush();

		finishProgress();

		cdata.sizeCompressed = outputBytes;
		cdata.CRC32Value = crc.getValue();
	}

	private void startCompressing(CompressData ccData)
	{
		this.cdata = ccData;
		outputBytes = 0;  // сбрасываем счетчики
		inputBytes = 0;

		assert cdata.sizeUncompressed > 0;

		startProgress(cdata.sizeUncompressed);
	}

	@Override
	public void startBlockUncompressing(CompressData cData)
	{
		startCompressing(cData);

		//int WORK_BUFFER = (cdata.sizeCompressed < (long)Utils.BLOCK_SIZE) ? (int)cdata.sizeCompressed : Utils.BLOCK_SIZE;
		//if( (buffer == null) || (buffer.length < WORK_BUFFER))
		//{
		//	buffer = new byte[WORK_BUFFER];
		//}
	}

	@Override
	public void finishBlockUncompressing()
	{
		//finishUncompressing();
		finishProgress();
		cdata.CRC32Value = crc.getValue();
	}
/*
	public void decodeBlock(BlockCoderData data)
	{
		// старший бит=1 (|0x80)- означает что далее идут одинаковые символы.
		// (старший бит=0)- означает что далее идет последовательность неодинаковых символов
		// 129 - макс длина цепочки одинаковых символов. Отнимаем 2 от реальной длины
		// 128 - макс длина цепочки неодинаковых символов. Отнимаем 1 от реальной длины. Два одинаковых подряд включаем в неодинаковые.

		byte[] buf = data.srcblock;

		data.resetDest();
		int i = 0;
		while(true)
		{
			byte code = buf[i];
			if(Byte.toUnsignedInt(code) > 127)
			{ // equal bytes here
				int num = (code & 0x7F) + 2;
				for (int j = 0; j < num ; j++) // saving code+2 equal bytes
				{
					data.writeDest(buf[i + 1]);
					//crc.update(buff[i + 1]);
				}
				i += 2;
			}
			else
			{  // unequal bytes here
				assert i + 1 < data.bytesInBlock;

				if(i + 1 + code >= data.bytesInBlock)
				{
					logger.warning(String.format("WARNING! (i + 1 + code >= data.bytesInBlock) i=%d code=%d data.bytesInBlock=%d encodedbytes=%d compressedSize=%d", i, code, data.bytesInBlock, data.bytesInBlock, cdata.sizeCompressed));
				}

				assert i + 1 + code < data.bytesInBlock;

				data.writeDest(buf, i + 1, code + 1);
				//crc.update(buff, i + 1, code + 1);
				i += code + 1 + 1;
			}

			if(i == data.bytesInBlock) break;
		}
		data.bytesInBlock = data.ind;
	}
*/
	@Override
	public void uncompressBlock(BlockCoderData data)
	{
		inputBytes += data.bytesInBlock;
		updateProgress(inputBytes);

		coder.decodeBlock(data);
		crc.update(data.destblock, 0, data.bytesInBlock);
	}


	public void uncompress(CompressData uuData) throws IOException
	{
		startCompressing(uuData);
		if(cdata.sizeCompressed == 0) return; // for support of zero-length files

		uncompressInternal();

		cdata.sout.flush();
		finishProgress();
		cdata.CRC32Value = crc.getValue();
		//finishUncompressing();
	}

	public void uncompressInternal() throws IOException
	{
		// старший бит=1 (|0x80)- означает что далее идут одинаковые символы.
		// (старший бит=0)- означает что далее идет последовательность неодинаковых символов
		// 129 - макс длина цепочки одинаковых символов. Отнимаем 2 от реальной длины
		// 128 - макс длина цепочки неодинаковых символов. Отнимаем 1 от реальной длины. Два одинаковых подряд включаем в неодинаковые.

		final int EQUALS_MAXLEN = 129;
		//final int MAX_BUF_SIZE= 100_000_000;
		int WORK_BUFFER = (cdata.sizeCompressed < (long)Utils.BLOCK_SIZE) ? (int)cdata.sizeCompressed : Utils.BLOCK_SIZE;
		if( (buffer == null) || (buffer.length < WORK_BUFFER))
		{
			buffer = new byte[WORK_BUFFER];
		}

		int i = 0;

		int cntRead = cdata.sin.read(buffer, 0, (int)Math.min((long)buffer.length, cdata.sizeCompressed));
		if(cntRead == -1) return; // cannot read anything from the stream

		assert cntRead == (int)Math.min((long)buffer.length, cdata.sizeCompressed);

		boolean last = cntRead < buffer.length; // if we've read less than buffer.length symbols, it means there are no more symbols in the stream
		long encodedBytes = cntRead;

		while(true/*encodedBytes < uData.sizeCompressed*/)
		{
			updateProgress(encodedBytes);

			byte code = buffer[i];
			if(Byte.toUnsignedInt(code) > 127)
			{ // equal bytes here
				int num = (code & 0x7F) + 2;
				for (int j = 0; j < num ; j++) { cdata.sout.write(buffer[i + 1]); crc.update(buffer[i + 1]);}  // saving code+2 equal bytes
				i += 2;
			}
			else
			{  // unequal bytes here
				assert i + 1 < buffer.length;

				if(i + 1 + code >= buffer.length)
				{
					logger.warning(String.format("WARNING! (i + 1 + code >= buffer.length) i=%d code=%d buffer.length=%d encodedbytes=%d compressedSize=%d", i, code, buffer.length, encodedBytes, cdata.sizeCompressed));
				}

				assert i + 1 + code < buffer.length;

				cdata.sout.write(buffer, i + 1, code + 1);
				crc.update(buffer, i + 1, code + 1);
				i += code + 1 + 1;
			}

			if(i == cntRead) break;

			if((!last) && (i > cntRead - EQUALS_MAXLEN)) // too low bytes in the buffer, reading more
			{
				assert encodedBytes <= cdata.sizeCompressed;

				int remaining = cntRead - i;
				int toRead = (int) Math.min((long) (buffer.length - remaining), (long)(cdata.sizeCompressed - encodedBytes));

				assert toRead >= 0;
				last = (cdata.sizeCompressed - encodedBytes) < (long) (buffer.length - remaining);

				if(toRead > 0)
				{
					System.arraycopy(buffer, i, buffer, 0, remaining);
					cntRead = cdata.sin.read(buffer, remaining, toRead);

					assert cntRead != -1;
					assert cntRead == toRead;

					encodedBytes += cntRead;
					i = 0;
					cntRead = (cntRead == -1) ? remaining : cntRead + remaining;
				}
			}

		}

	}

}
