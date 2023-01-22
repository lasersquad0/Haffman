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

	/*
	public HFUncompressor(String archiveFilename)
	{
		ARCHIVE_FILENAME = archiveFilename;
		OUTPUT_FILENAME = getArchiveFilename(archiveFilename);
		externalStreams = false;
	}

	public HFUncompressor(String archiveFilename, String outputFilename)
	{
		ARCHIVE_FILENAME = archiveFilename;
		OUTPUT_FILENAME = outputFilename;
		externalStreams = false;
	}
*/
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
		uncompressNoBuildTree2();
	}

	public void uncompressNoBuildTree2() throws IOException
	{
		int encodedBytes = 0;
		int mask;
		int data;
		int data2 = 0;
		int remaining = Integer.SIZE;
		while (encodedBytes < fileFormat.fileSizeComp) // заканчиваем раскодировать как только кончились байты
		{
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
					tmp = tmp & mask;    //masks[Integer.SIZE - tmpRemaining];     // очищаем лишние биты перед обьединением байтов.
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
				int len = tree.codeLenList.get(i);
				if(len > remaining) continue;    // если текущий код длиннее оставшегося в ccode просто пропускаем его

				int c = tree.codesList.get(i);
				int mask = 0xFFFFFFFF << (Integer.SIZE - len);
				int code2 = ccode & mask;         // оставляем только биты равные длине кода
				c = c << (Integer.SIZE - len); // готовим код, делаем его выровненым по левому краю
				int code3 = code2 ^ c;        // xor - если операнды равны то результат будет 0.
				if(code3 == 0)  // совпал код
				{
					int ch = tree.symbolsList.get(i);
					writeByte(ch);
					ccode = ccode << len; // выравниваем оставшиеся биты влево.
					remaining -=len;
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
/*
	public void uncompressNoBuildTree() throws IOException
	{
		int bt;
		byte wbt = 0;
		int remaining = Byte.SIZE;
		while ((bt = sin.read()) != -1)
		{
			// готовим 1 полный байт для парсинга
			if(remaining < Byte.SIZE) // есть остатки с прошлого байта они в wbt
			{
				wbt <<= (Byte.SIZE - remaining);    // освобождаем место под новые биты
				int tmp = bt >>> remaining;
				wbt |= tmp;
				int tmpRemaining = parseByte(wbt); // после вызова у нас есть 2 остатка битов: часть битов wbt и вторая часть это неиспользованые биты в bt
				if(remaining + tmpRemaining > Byte.SIZE) // остатков может оказаться больше чем вмещает 1 байт. обьединяем и разкодируем их
				{
					wbt <<= (Byte.SIZE - tmpRemaining);  // освобождаем все не занятое в wbt место под новые биты
					tmp = bt >>> (remaining - (Byte.SIZE - tmpRemaining));
					tmp = tmp & masks[Byte.SIZE - tmpRemaining];     // очищаем лишние биты перед обьединением байтов.
					wbt |= tmp;
					remaining += tmpRemaining;
					remaining -= Byte.SIZE;
					tmpRemaining = parseByte(wbt);
				}

				wbt <<= remaining;
				bt &= masks[remaining]; // очищаем лишние биты перед обьединением байтов.
				wbt |= bt;
				remaining += tmpRemaining;

				if(remaining == Byte.SIZE) // из остатков набрался целый байт. записываем его перед чтением следующего
				{
					remaining = parseByte(wbt);
				}

				assert (remaining <= Byte.SIZE);
			}
			else
			{
				wbt = (byte)bt;
				remaining = parseByte(wbt);
			}

			remaining = (remaining == 0) ? Byte.SIZE: remaining;
		}

		wbt <<= (Byte.SIZE - remaining);
		remaining = parseByte(wbt);

		sout.flush();

	}

	private int parseByte(byte cd) throws IOException
	{
		final byte CODE_LEN_3 = 3;
		final byte CODE_LEN_4 = 4;
		//final byte MASK_3 = 0b00000111;
		//final byte MASK_4 = 0b00001111;

		int remaining = Byte.SIZE;
		while(remaining > CODE_LEN_3) // можем прочитать еще один символ из байта
		{
			byte cd3 = (byte)(cd >>> remaining - CODE_LEN_3); // сначала ищем трехбитовые коды, сдвигаем так что бы осталось 3 бита
			cd3 &= masks[3];  // MASK_3;        // clearing all the rest bits but 3
			Integer ch = tree.rcodes3.get(cd3); // проверяем есть ли такой код в таблице кодов
			if (ch != null)
			{
				writeByte(ch);
				remaining -= CODE_LEN_3;
			}
			else
			{
				byte cd4 = (byte) (cd >>> remaining - CODE_LEN_4); // сдвигаем так что бы осталось 4 бита
				cd4 &= masks[4];  //MASK_4;
				ch = tree.rcodes4.get(cd4);
				writeByte(ch);
				remaining -= CODE_LEN_4;
			}
		}

		return remaining;
	}
*/
	private void writeByte(int v) throws IOException
	{
		sout.write(v);
		decodedBytes++;
	}


}
