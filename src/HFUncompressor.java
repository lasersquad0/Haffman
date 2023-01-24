import java.io.*;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class HFUncompressor
{
	private final static Logger logger = Logger.getLogger("HFLogger");
	HFTree tree;
	HFFileData fileData;
	//String OUTPUT_FILENAME;
	//String ARCHIVE_FILENAME;
	//final int MAX_BUF_SIZE = 1_000_000_000; // если файл >1G, то используем буфер этого размера иначе буфер размера файла
	//int FILE_BUFFER;         // фактический размер буфера для file streams in and out
	//boolean externalStreams; // if false we call close() on streams after finishing compress operation
	//InputStream sin;         // stream with data to compress
	//OutputStream sout;       // stream with compressed data
	CRC32 crc = new CRC32();
	long decodedBytes = 0;


	/*

	public HFUncompressor(InputStream in, OutputStream out)
	{
		sin = in;
		sout = out;
		externalStreams = true;
	}
/*
	private void createIOStreams() throws IOException
	{
		if(sin == null)
		{
			File inFile = new File(OUTPUT_FILENAME);
			FILE_BUFFER = (inFile.length() < MAX_BUF_SIZE) ? (int) inFile.length() : MAX_BUF_SIZE;

			sin = new BufferedInputStream(new FileInputStream(inFile), FILE_BUFFER);
		}

		if(sout == null)
			sout = new BufferedOutputStream(new FileOutputStream(ARCHIVE_FILENAME), FILE_BUFFER);

		HFTree tree = new HFTree(sin);
	}
*/
	public void uncompress(HFTree tree, HFFileData fileData) throws IOException
	{
		decodedBytes = 0;
		this.tree = tree;
		this.fileData = fileData;
		uncompressInternal();

		long v = crc.getValue();
		if(fileData.CRC32Value != v)
			logger.warning(String.format("CRC values are not equal: %d and %d", fileData.CRC32Value, v));

	}

	private void uncompressInternal() throws IOException
	{
		logger.entering(this.getClass().getName(),"uncompressInternal");

		final int i32 = Integer.SIZE; // просто для удобства чтения
		int bitsToParse = i32;
		long encodedBytes = 0;
		int mask;
		int data;
		int data2 = 0;
		int remaining = Integer.SIZE;

		while (encodedBytes < fileData.fzCompressed) // заканчиваем раскодировать как только кончились байты
		{
			// вот так хитро читаем int из потока
			int ch1 = fileData.sin.read();
			int ch2 = fileData.sin.read();
			int ch3 = fileData.sin.read();
			int ch4 = fileData.sin.read();
			if ((ch1 | ch2 | ch3 | ch4) < 0)
				break;

			encodedBytes +=4;   // считаем сколько encoded bytes прочитали и потока и сравниваем с размером fileSizeComp

			if(encodedBytes > fileData.fzCompressed)
			{
				bitsToParse = fileData.lastBits;
				logger.info("uncompressInternal LAST PASS!!!");
			}

			data =  ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));

			if(remaining < i32) // есть остатки с прошлого байта они в data2
			{
				data2 <<= (i32 - remaining);    // старые биты равняем по левому краю, освобождаем место под новые биты
				int tmp = data >>> remaining;            // готовим новые данные в data для соединения с остатками старых
				data2 |= tmp;
				int tmpRemaining = parseInt(data2, Math.min(remaining + bitsToParse,i32)); // после вызова у нас есть 2 остатка битов: часть битов data2 и вторая часть это неиспользованные биты в data
				remaining = Math.max(remaining + bitsToParse - i32, 0);

				if(remaining + tmpRemaining > i32) // Остатков может оказаться больше чем вмещает int. Объединяем и раскодируем их
				{
					data2 <<= (i32 - tmpRemaining);  // освобождаем все не занятое в data2 место под новые биты
					tmp = data >>> (remaining - (i32 - tmpRemaining));   // убираем лишние младшие биты из data что бы добавить к тому что осталось в data2 после parseInt
					mask = 0xFFFFFFFF >>> tmpRemaining;
					tmp = tmp & mask;        // очищаем лишние биты перед объединением байтов.
					data2 |= tmp;
					remaining += tmpRemaining;
					remaining -= i32;

					assert remaining <= i32;

					tmpRemaining = parseInt(data2, i32);
				}

				assert remaining <= i32;

				// соединяем остаток битов от parseInt и биты которые не влезли от data
				data2 <<= remaining;
				mask = 0xFFFFFFFF >>> (i32 - remaining);
				data &= mask; //masks[remaining]; // очищаем лишние биты перед объединением байтов.
				data2 |= data;
				remaining += tmpRemaining;

				if(remaining == i32) // Из остатков набрался целый байт. Записываем его перед чтением следующего
				{
					remaining = parseInt(data2, i32);
				}

				assert (remaining <= i32);
			}
			else
			{
				data2 = data;
				remaining = parseInt(data2, bitsToParse);
			}

			remaining = (remaining == 0) ? i32: remaining;
		}

		if((remaining > 0) && (remaining < i32))
		{
			data2 <<= (i32 - remaining);
			remaining = parseInt(data2, Math.min(remaining, bitsToParse));
		}

		fileData.sout.flush();

		logger.exiting(this.getClass().getName(),"uncompressInternal");
	}

	/**
	 *
	 * @param code int для парсинга
	 *
	 * @return возвращает кол-во не оставшихся нераспаршеных битов в code.
	 * @throws IOException если при записи в поток, что-то пошло не так
	 */
	private int parseInt(int code, int lastBits) throws IOException
	{
		int remaining = Integer.SIZE;
		int ccode = code;
		while(remaining > Math.max(tree.minCodeLen, Integer.SIZE - lastBits))
		{
			boolean found = false;
			for(int i = 0; i < tree.codesList.size(); i++)
			{
				HFCode hfc = tree.codesList.get(i);
				if(hfc.len > remaining) continue;    // если текущий код длиннее оставшегося в ccode просто пропускаем его

				int mask = 0xFFFFFFFF << (Integer.SIZE - hfc.len);
				int c = hfc.code << (Integer.SIZE - hfc.len); // готовим код для операции сравнения, делаем его выровненным по левому краю
				int code2 = ccode & mask;         // оставляем только биты равные длине кода
				code2 = code2 ^ c;            // XOR - если операнды равны то результат будет 0.
				if(code2 == 0)  // совпал код
				{
					writeByte(hfc.symbol);
					ccode = ccode << hfc.len; // выравниваем оставшиеся биты влево.
					remaining -= hfc.len;
					found = true;
					break;
				}
			}

			// Здесь в целом может быть кейс когда весь цикл прошел, а код так и не найден. Это значит что осталось мало битов в ccode и сравнение не сработало.
			// Сработает когда parent method докинет еще битов в ccode
			assert !((remaining > tree.maxCodeLen) && (!found));

			if(!found) break;    // прерываем цикл, что бы докинулось еще битов в ccode

		}

		return remaining;
	}

	private void writeByte(int v) throws IOException
	{
		fileData.sout.write(v);
		crc.update(v);
		decodedBytes++;
	}


}
