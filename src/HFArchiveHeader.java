import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;

public class HFArchiveHeader
{
	/*
	Общая структура файла архива
1. Сигнатура типа файла — 4 байта "ROMA"
2. Версия формата файла — 2 байта - '00', '01'
3. Кол-во файлов в архиве. Для последующего корректного чтения Списка файлов.
4. Таблица с именами файлов, размерами и остальными полями.
5. Размер таблицы кодов для первого файла — 2 байта (если требуется)
6. Таблица кодов — 6 * (количество знаков) байт [тройки: символ(byte) — код(int) - длина(byte)]
7. Сжатые данные — кол-во сжатых байт сохраняется в таблице файлов
пункты 6 и 7 повторяются для всех файлов.
*/
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	final byte[] fileSignature = {'R', 'O', 'M', 'A'};
	final byte[] fileVersion = {'0', '1'};
	ArrayList<HFFileRec> files = new ArrayList<>();



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
		var dis = new DataInputStream(sin);

		int res = dis.read(fileSignature);
		res |= dis.read(fileVersion);
		if(res < 0)
			throw new IOException("Wrong file format.");

		files.clear();
		short filesCount = dis.readShort();
		for (short i = 0; i < filesCount; i++)
		{
			HFFileRec fr = new HFFileRec();
			fr.load(dis);

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
	public void fillFileRecs(String[] filenames, Utils.CompTypes alg)
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
				fr.alg = (byte)alg.ordinal();
				fr.blockCount = 0;
				fr.blockSize = Utils.BLOCK_SIZE;

				files.add(fr);
			}
			else
				logger.warning(String.format("File '%s' cannot be found, pass on it.", fl.getAbsolutePath()));
		}
		if(files.size() == 0)
			throw new IllegalArgumentException("There are no files to compress. Exiting...");
	}

	/**
	 * Записываем в уже сформированный архив размер закодированных (сжатых) потоков в байтах для каждого файла в архиве
	 * @param arcFilename Name of the archive
	 * @throws IOException if something goes wrong
	 */
	public void updateHeaders( String arcFilename) throws IOException
	{
		RandomAccessFile raf = new RandomAccessFile(new File(arcFilename), "rw");

		int InitialOffset = fileSignature.length + fileVersion.length + Short.BYTES; // start of files table in an archive
		int CRC32ValueOffset = Long.BYTES;
		int CompressedSizeOffset = 3*Long.BYTES;
		int BlockCountOffset = 4*Long.BYTES + Byte.BYTES;
		int FileRecSize = 4*Long.BYTES + 2*Integer.BYTES + Byte.BYTES + Short.BYTES;  // Short.BYTES - this is for saving filename length


		int pos = InitialOffset;

		for (HFFileRec fr : files)
		{
			raf.seek(pos + CRC32ValueOffset);
			raf.writeLong(fr.CRC32Value);

			raf.seek(pos + CompressedSizeOffset);
			raf.writeLong(fr.compressedSize);

			raf.seek(pos + BlockCountOffset);
			raf.writeInt(fr.blockCount);

			pos = pos + FileRecSize + fr.fileName.length() * 2; // length()*2 because writeChars() saves each char as 2 bytes
		}

		raf.close();
	}

}

/*
Структура записи о файле в архиве
1. Размер ориг файла — 8 байт (long)
2. CRC32 ориг файла для проверки правильности разархивации и общей целостности — 8 байт
3. Modified date ориг файла
4. Размер сжатого файла — 8 байт (long) - задает размер данных для разархивации
5. Код алгоритма которым был сжат файл
6. Количество блоков сжатого файла (для неблочных алгоритмов записывается ноль)
7. Размер строки имени файла
8. Строка имени файла который находится в архиве. На каждый символ используется 2 байта при записи.
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
	byte alg;
	int blockCount;
	int blockSize;

	public void save(OutputStream sout) throws IOException
	{
		var dos = new DataOutputStream(sout);

		dos.writeLong(fileSize);
		dos.writeLong(CRC32Value);
		dos.writeLong(modifiedDate);
		dos.writeLong(compressedSize);
		dos.writeByte(alg);
		dos.writeInt(blockCount);
		dos.writeInt(blockSize);
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
		alg = dos.readByte();
		blockCount = dos.readInt();
		blockSize = dos.readInt();

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
