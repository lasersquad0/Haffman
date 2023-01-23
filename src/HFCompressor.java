import java.io.*;

public class HFCompressor
{
	final String HF_ARCHIVE_EXT = ".hf";
	HFTree tree;
	byte[] writeBuffer = new byte[4];  // буфер для записи int в OutputStream
	//String INPUT_FILENAME;
	//String ARCHIVE_FILENAME;
	final int MAX_BUF_SIZE = 1_000_000_000; // если файлм >1G, то используем буфер этого размера иначе буфер размера файла
	//int FILE_BUFFER;         // фактический размер буфера для file streams in and out
	boolean externalStreams; // if false we call close() on streams after finishing compress operation
	InputStream sin;         // stream with data to compress
	OutputStream sout;       // stream with compressed data
	public long encodedBytes = 0;


/*	public HFCompressor(String filename)
	{
		INPUT_FILENAME = filename;
		ARCHIVE_FILENAME = getArchiveFilename(INPUT_FILENAME);
		externalStreams = false;
	}

	public HFCompressor(String inputFilename, String archiveFilename)
	{
		INPUT_FILENAME = inputFilename;
		ARCHIVE_FILENAME = archiveFilename;
		externalStreams = false;
	}
*/
	public HFCompressor(InputStream in, OutputStream out)
	{
		sin = in;
		sout = out;
		externalStreams = true;
	}

	public void compress(HFTree tree) throws IOException
	{
		encodedBytes = 0;  // сбрасываем счетчик
		this.tree = tree;
		//createIOStreams();
		//tree.build();
		//fileFormat.CRC32Value = tree.CRC32Value;
		//byte[] table = tree.getTable();
		//fileFormat.hfTableSize = (short)table.length;
		//fileFormat.saveHeader(sout);
		//sout.write(table);
		compressNoBuildTree();
	}

	public void compressNoBuildTree() throws IOException
	{
		int ch;
		int accum = 0;
		int counter = 0;
		while ((ch = sin.read()) != -1)
		{
			HFCode hfc = tree.codesMap.get(ch);
			//int code = tree.codes.get(ch);
			//byte len = tree.codeLen.get(ch);

			if (counter + hfc.len > Integer.SIZE) // новый byte не влазит в остаток слова, делим на 2 части
			{
				accum = accum << Integer.SIZE - counter; // освобождаем сколько осталось места в слове
				int len2 = hfc.len + counter - Integer.SIZE; // кол-во не вмещающихся битов
				int code2 = hfc.code >>> len2; // в текущее слово вставляем только часть битов. остальная часть пойдет в новое слово
				accum = accum | code2;
				writeInt(accum); // заполнили слово полностью
				accum = 0;
				int mask = 0xFFFFFFFF >>> Integer.SIZE - len2;
				hfc.code &= mask;   // затираем биты которые ранее вставили в предыдущее слово
				accum = accum | hfc.code;
				counter = len2;
			}
			else
			{
				accum = accum << hfc.len;   // освобождаем на длину вставляемого кода
				accum = accum | hfc.code;
				counter += hfc.len;
				if (counter == Integer.SIZE)
				{
					writeInt(accum); // заполнили слово ровно полностью
					accum = 0;
					counter = 0;
				}
			}
		}

		assert counter < 32;

		if(counter > 0) // поток закончился, а еще остались данные в accum, записываем их
		{
			accum = accum << Integer.SIZE - counter; // досдвигаем accum так что бы "пустые" биты остались справа а не слева
			writeInt(accum);   // записываем весь
			// корректируем счетчик encoded bytes что бы при раскодировании не возникали лишние байты.
			int corr = counter % 8 == 0 ? counter/8 : counter/8 + 1;
			encodedBytes = encodedBytes - 4 + corr;
		}

		sout.flush();

		if(!externalStreams)
		{
			sout.close();
			sin.close();
		}
	}
	/*
	private void createIOStreams() throws IOException
	{
		File inFile = new File(INPUT_FILENAME);
		FILE_BUFFER = (inFile.length() < MAX_BUF_SIZE) ? (int) inFile.length() : MAX_BUF_SIZE;

		sin = new BufferedInputStream(new FileInputStream(inFile), FILE_BUFFER);
		sout = new BufferedOutputStream(new FileOutputStream(ARCHIVE_FILENAME), FILE_BUFFER);

		// для tree открываем ДРУГОЙ поток потому что дереву нужно его пройти полностью при создании весов кодов.
		//tree = new HFTree(new BufferedInputStream(new FileInputStream(INPUT_FILENAME), FILE_BUFFER));

		//fileFormat = new HFFileFormat();
		//fileFormat.fileSizeUncomp = inFile.length();
	}
*/

/*	private void writeBytes(int v, int num) throws IOException
	{
		writeBuffer[0] = (byte)(v >>> 24);
		writeBuffer[1] = (byte)(v >>> 16);
		writeBuffer[2] = (byte)(v >>>  8);
		writeBuffer[3] = (byte)(v >>>  0);
		sout.write(writeBuffer, 0, num);
		encodedBytes += num;
	}
*/

	private void writeInt(int v) throws IOException
	{
		writeBuffer[0] = (byte)(v >>> 24);
		writeBuffer[1] = (byte)(v >>> 16);
		writeBuffer[2] = (byte)(v >>>  8);
		writeBuffer[3] = (byte)(v >>>  0);
		sout.write(writeBuffer, 0, 4);
		encodedBytes += 4;
	}
/*
	private String getArchiveFilename(String filename)
	{
		return filename.substring(0, filename.lastIndexOf(".")) + HF_ARCHIVE_EXT;
	}
 */

}

