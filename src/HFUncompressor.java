import java.io.*;

public class HFUncompressor
{
	HFTree tree;
	HFFileFormat fileFormat;
	//String OUTPUT_FILENAME;
	//String ARCHIVE_FILENAME;
	//final int MAX_BUF_SIZE = 1_000_000_000; // если файлм >1G, то используем буфер этого размера иначе буфер размера файла
	//int FILE_BUFFER;         // фактический размер буфера для file streams in and out
	boolean externalStreams; // if false we call close() on streams after finishing compress operation
	InputStream sin;         // stream with data to compress
	OutputStream sout;       // stream with compressed data
	long decodedBytes = 0;


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
	public void uncompress(HFTree tree, HFFileFormat fileFormat) throws IOException
	{
		decodedBytes = 0;
		this.tree = tree;
		this.fileFormat = fileFormat;
		uncompressInternal();
	}

	private void uncompressInternal() throws IOException
	{
		int encodedBytes = 0;
		int mask;
		int data;
		int data2 = 0;
		int remaining = Integer.SIZE;

		while (encodedBytes < fileFormat.fileSizeComp) // заканчиваем раскодировать как только кончились байты
		{
			// вот так хитро читаем int из потока
			int ch1 = sin.read();
			int ch2 = sin.read();
			int ch3 = sin.read();
			int ch4 = sin.read();
			if ((ch1 | ch2 | ch3 | ch4) < 0)
				break;

			encodedBytes +=4;   // считаем сколько encoded bytes прочитали и потока и сравниваем с размером fileSizeComp

			data =  ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));

			if(remaining < Integer.SIZE) // есть остатки с прошлого байта они в data2
			{
				data2 <<= (Integer.SIZE - remaining);    // освобождаем место под новые биты
				int tmp = data >>> remaining;
				data2 |= tmp;
				int tmpRemaining = parseInt(data2); // после вызова у нас есть 2 остатка битов: часть битов data2 и вторая часть это неиспользованые биты в data

				if(remaining + tmpRemaining > Integer.SIZE) // остатков может оказаться больше чем вмещает int. обьединяем и раскодируем их
				{
					data2 <<= (Integer.SIZE - tmpRemaining);  // освобождаем все не занятое в data2 место под новые биты
					tmp = data >>> (remaining - (Integer.SIZE - tmpRemaining));   // убираем лишние младшие биты из data что бы добавить к тому что осталось в data2 после parseInt
					mask = 0xFFFFFFFF >>> tmpRemaining;
					tmp = tmp & mask;        // очищаем лишние биты перед обьединением байтов.
					data2 |= tmp;
					remaining += tmpRemaining;
					remaining -= Integer.SIZE;

					assert remaining <= Integer.SIZE;

					tmpRemaining = parseInt(data2);
				}

				assert remaining <= Integer.SIZE;

				// соединяем остаток битов от parseInt и биты которые не влезли от data
				data2 <<= remaining;
				mask = 0xFFFFFFFF >>> (Integer.SIZE - remaining);
				data &= mask; //masks[remaining]; // очищаем лишние биты перед обьединением байтов.
				data2 |= data;
				remaining += tmpRemaining;

				if(remaining == Integer.SIZE) // из остатков набрался целый байт. записываем его перед чтением следующего
				{
					remaining = parseInt(data2);
				}

				assert (remaining <= Integer.SIZE);
			}
			else
			{
				data2 = data;
				remaining = parseInt(data2);
			}

			remaining = (remaining == 0) ? Integer.SIZE: remaining;
		}

		//data2 <<= (Integer.SIZE - remaining);
		//remaining = parseInt(data2);

		sout.flush();
	}

	/**
	 *
	 * @param code int для парсинга
	 *
	 * @return возвращает кол-во не оставшихся нераспаршеных битов в code.
	 * @throws IOException
	 */
	private int parseInt(int code) throws IOException
	{
		int remaining = Integer.SIZE;
		int ccode = code;
		while(remaining > tree.minCodeLen)
		{
			boolean found = false;
			for(int i = 0; i < tree.codesList.size(); i++)
			{
				HFCode hfc = tree.codesList.get(i);
				//int len = tree.codeLenList.get(i);
				if(hfc.len > remaining) continue;    // если текущий код длиннее оставшегося в ccode просто пропускаем его

				int mask = 0xFFFFFFFF << (Integer.SIZE - hfc.len);
				int c = hfc.code << (Integer.SIZE - hfc.len); // готовим код для операции сравнения, делаем его выровненным по левому краю
				int code2 = ccode & mask;         // оставляем только биты равные длине кода
				code2 = code2 ^ c;            // XOR - если операнды равны то результат будет 0.
				if(code2 == 0)  // совпал код
				{
					//int ch = tree.symbolsList.get(i);
					writeByte(hfc.symbol);
					ccode = ccode << hfc.len; // выравниваем оставшиеся биты влево.
					remaining -= hfc.len;
					found = true;
					break;
				}
			}

			// здесь в целом может быть кейс когда весь цикл прошел, а код так и не найден. Это значит что осталось мало битов в ccode и сравнение не сработало.
			// сработает когда parent method докинет еще битов в ccode
			assert !((remaining > tree.maxCodeLen) && (!found));

			if(!found) break;    // прерываем цикл, что бы докинулось еще битов в ccode

		}

		return remaining;
	}

	private void writeByte(int v) throws IOException
	{
		sout.write(v);
		decodedBytes++;
	}


}
