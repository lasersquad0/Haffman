import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;

public class RangeArchiver extends Archiver
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	//private final Utils.CompTypes COMPRESSOR_CODE; // see default constructor
	private final int OUTPUT_BUF_SIZE = 100_000_000; // buffer for output archive file
	//private int[] symbols;
	//private long[] weights;
	//private long[] cumFreq = null;
	//private int[] symbol_to_freq_index; // = new int[256];


	RangeArchiver()
	{
		this(Utils.CompTypes.ARITHMETIC);
	}
	RangeArchiver(Utils.CompTypes compCode)
	{
		if( (compCode != Utils.CompTypes.ARITHMETIC) && (compCode != Utils.CompTypes.ARITHMETIC32) && (compCode != Utils.CompTypes.ARITHMETIC64) )
			throw new IllegalArgumentException(String.format("Incorrect compressor type '%s' is specified for '%s' compressor.", compCode.toString(), this.getClass().getName()));

		COMPRESSOR_CODE = compCode;
	}

	@Override
	public void compressFiles(String[] filenames) throws IOException
	{
		if(filenames.length < 2)
			throw new IllegalArgumentException("There are no files to compress. Exiting...");

		logger.info("Using Arithmetic Range compression algorithm.");

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.fillFileRecs(filenames, COMPRESSOR_CODE); // that needs stay before creating output stream, to avoid creating empty archive files

		String arcFilename = getArchiveFilename(filenames[0]); 	// first parameter in array is name of archive
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(arcFilename), OUTPUT_BUF_SIZE);

		fh.saveHeader(sout);

		for (int i = 0; i < fh.files.size(); i++)
		{
			compressFile(sout, fh.files.get(i));
		}

		sout.close();

		fh.updateHeaders(arcFilename); // it is important to save info into files table that becomes available only after compression

		logger.info("All files are processed.");
	}

	private void compressFile(OutputStream sout, HFFileRec fr) throws IOException
	{
		logger.info(String.format("Analysing file '%s'.", fr.origFilename));

		File fl = new File(fr.origFilename); // TODO возможно хранить File в HFFileRec потому как fillFileRecs тоже создаются File
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), Utils.getOptimalBufferSize(fr.fileSize));

		var model = new ModelOrder0Fixed();
		model.calcWeights(fr.origFilename);
		var cData = new CompressData(sin, sout,fr.fileSize, model);

		logger.info("Starting compression...");

		if((COMPRESSOR_CODE == Utils.CompTypes.AARITHMETIC) || (COMPRESSOR_CODE == Utils.CompTypes.AARITHMETIC32) )
		{
			var c = new RangeCompressor32(Utils.MODE.MODE32);
			model.rescaleTo(c.BOTTOM);
			model.saveFreqs(sout);

			c.compress(cData);
		}
		else
		{
			var c = new RangeCompressor32(Utils.MODE.MODE64);
			model.rescaleTo(c.BOTTOM);
			model.saveFreqs(sout);

			c.compress(cData);
		}

		fr.compressedSize = cData.sizeCompressed;
		fr.CRC32Value = cData.CRC32Value;

		sin.close();

		printCompressionDone(fr);
	}

	@Override
	public void unCompressFiles(String arcFilename) throws IOException
	{
		logger.info(String.format("Loading archive file '%s'.", arcFilename));

		File fl = new File(arcFilename);
		long fileLen = fl.length();
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), Utils.getOptimalBufferSize(fileLen));

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.loadHeader(sin);

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);
			unCompressFile(fr, sin);
		}

		sin.close();

		logger.info("All files are extracted.");
	}

	@Override
	public void unCompressFile(HFFileRec fr, InputStream sin) throws IOException
	{
		logger.info(String.format("Extracting file '%s'...", fr.fileName));

		var model = new ModelOrder0Fixed();
		model.loadFreqs(sin); //здесь читаем таблицы symbols и cumFreq из потока

		OutputStream sout = new BufferedOutputStream(new FileOutputStream(fr.fileName), Utils.getOptimalBufferSize(fr.fileSize));
		var uData = new CompressData(sin, sout, fr.compressedSize, fr.fileSize, model);

		if(COMPRESSOR_CODE == Utils.CompTypes.ARITHMETIC32)
		{
			var uc = new RangeUncompressor32(Utils.MODE.MODE32);
			uc.uncompress(uData);
		}
		else
		{
			var uc = new RangeUncompressor32(Utils.MODE.MODE64);
			uc.uncompress(uData);
		}

		if (uData.CRC32Value != fr.CRC32Value)
			logger.warning(String.format("CRC values for file '%s' are not equal: %d and %d", fr.fileName, uData.CRC32Value, fr.CRC32Value));

		sout.close();

		logger.info(String.format("Extracting done '%s'.", fr.fileName));
	}



	/**
	 * записываем в архив размер закодированного потока в байтах и lastBits для каждого файла в архиве
	 * @param fh Header with correct compressedSize, lastBits and CRC32Values
	 * @param arcFilename Name of the archive
	 * @throws IOException if something goes wrong
	 */
	/*
	@Override
	public void updateHeaders(HFArchiveHeader fh, String arcFilename) throws IOException
	{
		RandomAccessFile raf = new RandomAccessFile(new File(arcFilename), "rw");

		var offsets = fh.getFieldOffsets();

		int pos = offsets.get(FHeaderOffs.InitialOffset);

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);

			raf.seek(pos + offsets.get(FHeaderOffs.CRC32Value));
			raf.writeLong(fr.CRC32Value);

			raf.seek(pos + offsets.get(FHeaderOffs.compressedSize));
			raf.writeLong(fr.compressedSize);

			//raf.seek(pos + offsets.get("lastBits"));
			//raf.writeByte(fr.lastBits);

			// calculate pos of the next file record, for the next loop iteration
			pos = pos + offsets.get(FHeaderOffs.FileRecSize) + fr.fileName.length()*2; // length()*2 because writeChars() saves each char as 2 bytes
		}

		raf.close();
	}
*/


	/**
	 * Читает весь файл и считает частоты встречаемости байтов в нем
	 * Для binary files считает частоты тоже корректно.
	 * *** Внимание - портит InputStream sin, передвигает его на конец файла *****
	 * @throws IOException если что-то произошло с потоком
	 */
/*	protected void calcWeights(InputStream sin) throws IOException
	{
		logger.entering(Main.class.getName(),"calcWeights");

		int BUF_SIZE = 100_000_000;  // если дали поток, то мы не знаем размер файла читаем кусками по BUF_SIZE (100М) тогда

		long[] freq = new long[256];
		byte[] buffer = new byte[BUF_SIZE];
		//CRC32 cc = new CRC32();

		int cntRead;
		do
		{
			cntRead = sin.read(buffer);
			if (cntRead == -1) break;
			//	cc.update(buffer, 0, cntRead); // параллельно считаем CRC32 файла
			for (int i = 0; i < cntRead; i++)
				freq[Byte.toUnsignedInt(buffer[i])]++; // обходим знаковость байта

		}while(cntRead == BUF_SIZE);

		//CRC32Value = cc.getValue(); // сохраняем значение CRC32 для последующего использования

		long sum = 0;
		long min = Long.MAX_VALUE, max = 0;
		int nonzero = 0;
		for (long l : freq)
		{
			sum += l;
			if(l > 0) nonzero++;
			if(l > 0) min = Math.min(min, l);
			max = Math.max(max, l);
		}

		symbols = new int[nonzero];
		weights = new long[nonzero];

		int j = 0;
		for (int i = 0; i < freq.length; i++)
		{
			if(freq[i] > 0)
			{
				symbols[j] = i;
				weights[j] = freq[i];
				j++;
			}
		}

		// preparing data for compressor/uncompressor
		cumFreq = new long[weights.length + 1];
		cumFreq[0] = 0;
		for (int i = 0; i < weights.length; i++)
		{
			cumFreq[i+1] = cumFreq[i] + weights[i];
		}
		logger.finer(String.format("freq=%s", Arrays.toString(cumFreq)));

		for (int i = 0; i < symbols.length; i++)
		{
			symbol_to_freq_index[symbols[i]] = i + 1;
		}

		logger.finer(String.format("symbols=%s", Arrays.toString(symbols)));
		logger.finer(String.format("weights=%s",Arrays.toString(weights)));
		logger.finer(String.format("min weight=%d, max weight=%d", min, max));
		logger.finer(String.format("nonzero=%d, sum(size)=%d", nonzero, sum));

		logger.exiting(Main.class.getName(),"calcWeights");
	}
*/
}


