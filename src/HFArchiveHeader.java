import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class HFArchiveHeader
{
	/*
	Общая структура файла архива
1. Сигнатура типа файла — 4 байта "ROMA"
2. Версия формата файла — 2 байта - '00', '01'
3. Кол-во файлов в архиве. Для последующего корректного чтения Списка файлов.
4. Таблица с именами файлов, размерами и остальными полями.
5. Размер таблицы кодов для первого файла — 2 байта
6. Таблица кодов — 6 * (количество знаков) байт [тройки: символ(byte) — код(int) - длина(byte)]
7. Сжатые данные — кол-во сжатых байт сохраняется в таблице файлов
пункты 6 и 7 повторяются для всех файлов.
*/
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	final byte[] fileSignature = {'R', 'O', 'M', 'A'};
	final byte[] fileVersion = {'0', '1'};
	ArrayList<HFFileRec> files = new ArrayList<>();



	/** Now itwworks through asserts
	 * Replace asserts by throwing exceptions.
	 */
	 public void checkHeaderData()
	{
		assert fileSignature[0] == 'R';
		assert fileSignature[1] == 'O';
		assert fileSignature[2] == 'M';
		assert fileSignature[3] == 'A';

		assert (fileVersion[0] >= '0') && (fileVersion[0] <= '9');
		assert (fileVersion[1] >= '0') && (fileVersion[1] <= '9');

		assert files.size() > 0;
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
	public void fillFileRecs(String[] filenames, char alg) 
	{
		files.clear();  // just in case

		for (int i = 1; i < filenames.length; i++)
		{
			File fl = new File(filenames[i]);
			if(fl.exists())
			{
				HFFileRec fr = new HFFileRec();
				fr.dirName = fl.getAbsolutePath(); // найти как возвращать только директорию. здесь сейчас возвращается весь путь с файлом вместо директории.
				fr.origFilename = filenames[i];
				fr.fileName = fl.getName();//filenames[i]; // store name of the file without path
				fr.fileSize = fl.length();
				fr.modifiedDate = fl.lastModified(); //another way to do the same is Files.getLastModifiedTime()
				fr.alg = (byte)alg;

				files.add(fr);
			}
			else
				logger.warning(String.format("File '%s' cannot be found, pass on it.", fl.getAbsolutePath()));
		}
		if(files.size() == 0)
			throw new IllegalArgumentException("There are no files to compress. Exiting...");
	}

	public HashMap<FHeaderOffs,Integer> getFieldOffsets()
	{
		var res = new HashMap<FHeaderOffs, Integer>();
		res.put(FHeaderOffs.InitialOffset, fileSignature.length + fileVersion.length + Short.BYTES); // initial fixed offset
		res.put(FHeaderOffs.CRC32Value, Long.BYTES);
		res.put(FHeaderOffs.compressedSize, 3*Long.BYTES);
		res.put(FHeaderOffs.lastBits, 4*Long.BYTES);
		res.put(FHeaderOffs.FileRecSize, 4*Long.BYTES + 2*Byte.BYTES+ Short.BYTES); // Short.BYTES - this is for saving filename length

		return res;
	}

}

/*
Структура записи о файле
1. Размер ориг файла — 8 байт (long)
2. CRC32 ориг файла для проверки правильности разархивации и общей целостности — 8 байт
3. Modified date ориг файла
4. Размер сжатого файла — 8 байт (long) - задает размер данных для разархивации
5 Кол-во значимых bits в последнем int закодированного потока
6. Размер строки имени файла
7. Строка имени файла который находится в архиве. На каждый символ используется 2 байта при записи.
*/

class HFFileRec
{
	// **** Нужно внести изменения в HashMap когда добавляются новые поля сюда!! *********
	String fileName;
	String origFilename;
	String dirName; // for future extensions, not used now
	long fileSize;
	long CRC32Value;
	long modifiedDate;
	long compressedSize;
	byte lastBits;
	byte alg;

	public void save(OutputStream sout) throws IOException
	{
		var dos = new DataOutputStream(sout);

		dos.writeLong(fileSize);
		dos.writeLong(CRC32Value);
		dos.writeLong(modifiedDate);
		dos.writeLong(compressedSize);
		dos.writeByte(lastBits);
		dos.writeByte(alg);
		dos.writeShort(fileName.length());
		dos.writeChars(fileName);  // NOTE! writes 2 bytes for each char
	}

	public void load(InputStream sout) throws IOException
	{
		var dos = new DataInputStream(sout);

		fileSize = dos.readLong();
		CRC32Value = dos.readLong();
		modifiedDate = dos.readLong();
		compressedSize = dos.readLong();
		lastBits = dos.readByte();
		alg = dos.readByte();

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

enum FHeaderOffs
{
	InitialOffset,
	CRC32Value,
	compressedSize,
	lastBits,
	FileRecSize
}
