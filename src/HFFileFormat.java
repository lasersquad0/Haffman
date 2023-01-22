import java.io.*;

public class HFFileFormat
{
	/*
1. Сигнатура типа файла — 4 байта "ROMA"
2. Версия формата файла — 2 байта
3. CRC32 несжатого файла для проверки правильности разархивации и общей целостности — 8 байт
4. Размер сжатого файла — 8 байт (long) - задает размер данных в пункте 9.
5. Размер несжатого файла — 8 байт (long) - хз нужно ли. только если для проверки
6. Размер строки имени файла из п5.
7. Имя файла который находится в архиве
8. Размер таблицы символов — 2 байта
9. Таблица символов — 2 * (количество знаков) байт [парами: символ — код]
10. Сжатые данные — сколько уже получится байт
*/
	private static final byte[] fileSignature = {'R', 'O', 'M', 'A'};
	private static final byte[] fileFormat = {'0', '1'};
	long CRC32Value;
	long fileSizeUncomp;
	String filenameUncomp;
	long fileSizeComp;
	short hfTableSize;


	public HFFileFormat()
	{

	}

	public void loadHeader(InputStream in) throws IOException
	{
		var dos = new DataInputStream(in); // TODO добавить проверки что неожиданно не закнчился stream и т.п. (если подсунули кривой файл)
		dos.read(fileSignature);  // TODO добавить проверку что это действительно правильный compressed file а не какой нить rar или zip
		dos.read(fileFormat);
		CRC32Value = dos.readLong();
		fileSizeComp = dos.readLong();
		fileSizeUncomp = dos.readLong();

		int filenameSize = dos.readShort();
		StringBuilder sb = new StringBuilder(filenameSize);
		while(filenameSize > 0)
		{
			sb.append(dos.readChar());
			filenameSize--;
		}

		filenameUncomp = sb.toString();
		hfTableSize = dos.readShort();
	}

	public void saveHeader(OutputStream out) throws IOException
	{
		var dos = new DataOutputStream(out);
		dos.write(fileSignature);
		dos.write(fileFormat);
		dos.writeLong(CRC32Value);
		dos.writeLong(fileSizeComp);
		dos.writeLong(fileSizeUncomp);
		dos.writeShort(filenameUncomp.length());
		dos.writeChars(filenameUncomp);
		dos.writeShort(hfTableSize);
	}

	public int encodedDataSizePos()
	{
		return fileSignature.length + fileFormat.length + Long.BYTES; // длина encoded файла записана по смещению 14 от начала файла архива
	}

}
