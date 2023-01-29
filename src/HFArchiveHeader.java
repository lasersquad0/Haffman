import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class HFArchiveHeader
{
	/*
1. Сигнатура типа файла — 4 байта "ROMA"
2. Версия формата файла — 2 байта
3. CRC32 несжатого файла для проверки правильности разархивации и общей целостности — 8 байт
4. Размер сжатого файла — 8 байт (long) - задает размер данных в пункте 9.
4.5 Кол-во записанных bits в последнем int закодированного потока
5. Размер несжатого файла — 8 байт (long) - хз нужно ли. только если для проверки
6. Размер строки имени файла из п5.
7. Имя файла который находится в архиве
8. Размер таблицы символов — 2 байта
9. Таблица символов — 2 * (количество знаков) байт [парами: символ — код]
10. Сжатые данные — сколько уже получится байт
*/
	private final static Logger logger = Logger.getLogger("HFLogger");
	final byte[] fileSignature = {'R', 'O', 'M', 'A'};
	final byte[] fileVersion = {'0', '1'};
	ArrayList<HFFileRec> files = new ArrayList<>();



	/** Now itwworks through asserts
	 * Replace asserts by throwing exceptions.
	 */
	 public void checkHeaderData()
	{
		byte[] b = {'R', 'O', 'M', 'A'};
		assertArrayEquals(b, fileSignature);

		assertTrue((fileVersion[0] >= '0') && (fileVersion[0] <= '9'));
		assertTrue((fileVersion[1] >= '0') && (fileVersion[1] <= '9'));

		assertTrue(files.size() > 0);
	}

	public void loadHeader(InputStream sin) throws IOException
	{
		var dos = new DataInputStream(sin);

		int res = dos.read(fileSignature);
		res |= dos.read(fileVersion);
		if(res < 0)
			throw new IOException("Wrong file format.");

		files.clear();
		short filesCount = dos.readShort();
		for (short i = 0; i < filesCount; i++)
		{
			HFFileRec fr = new HFFileRec();
			fr.load(dos);

			files.add(fr);
		}

		checkHeaderData();
	}

	public void saveHeader(OutputStream sout) throws IOException
	{
		checkHeaderData();

		var dos = new DataOutputStream(sout);

		dos.write(fileSignature);
		dos.write(fileVersion);

		dos.writeShort(files.size()); // assumed archive will have less than 65535 files in it
		for (HFFileRec fr : files)
		{
			fr.save(dos);
		}
	}

	/**
	 * Adds filenames into ArrayList of HFFileRec together with file lengths and modified attributes
	 * @param filenames list of files to compress. Note, that zero index in this array contains archive name, so first filename is filenames[1]
	 */
	public void fillFileRecs(String[] filenames)
	{
		files.clear();  // just in case

		for (int i = 1; i < filenames.length; i++)
		{
			File fl = new File(filenames[i]);
			if(fl.exists())
			{
				HFFileRec fr = new HFFileRec();
				fr.dirName = fl.getAbsolutePath();
				fr.fileName = filenames[i];
				fr.fileSize = fl.length();
				fr.modifiedDate = fl.lastModified(); //another way to do the same is Files.getLastModifiedTime()

				files.add(fr);
			}
			else
				logger.warning(String.format("File '%s' cannot be found, pass on it.", fl.getAbsolutePath()));
		}
		if(files.size() == 0)
			throw new IllegalArgumentException("There are no files to compress. Exiting...");
	}

	public HashMap<String,Integer> getFieldOffsets()
	{
		var res = new HashMap<String, Integer>();
		res.put("initial offset", fileSignature.length + fileVersion.length + Short.BYTES); // initial fixed offset
		res.put("CRC32Value", Long.BYTES);
		res.put("compressedSize", 3*Long.BYTES);
		res.put("lastBits", 4*Long.BYTES);
		res.put("HFFileRecSize", 4*Long.BYTES + Byte.BYTES+ Short.BYTES);

		return res;
	}

}


class HFFileRec
{
	String fileName;
	String dirName; // for future extensions, not used now
	long fileSize;
	long CRC32Value;
	long modifiedDate;
	long compressedSize;
	byte lastBits;

	public void save(OutputStream sout) throws IOException
	{
		var dos = new DataOutputStream(sout);

		dos.writeLong(fileSize);
		dos.writeLong(CRC32Value);
		dos.writeLong(modifiedDate);
		dos.writeLong(compressedSize);
		dos.writeByte(lastBits);
		dos.writeShort(fileName.length());
		dos.writeChars(fileName);  // writes 2 bytes for each char
	}

	public void load(InputStream sout) throws IOException
	{
		var dos = new DataInputStream(sout);

		fileSize = dos.readLong();
		CRC32Value = dos.readLong();
		modifiedDate = dos.readLong();
		compressedSize = dos.readLong();
		lastBits = dos.readByte();

		int filenameSize = dos.readShort();
		StringBuilder sb = new StringBuilder(filenameSize);
		while(filenameSize > 0)
		{
			sb.append(dos.readChar());
			filenameSize--;
		}

		fileName = sb.toString();
	}
}