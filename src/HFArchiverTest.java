import org.junit.Test;

import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class HFArchiverTest {

	private static Logger logger;

	public HFArchiverTest() throws IOException
	{
		if(logger == null)
		{
			// Настраиваем логгер что бы тесты писались в другой файл. Одной конфигураций в JUL такое настроить нельзя. Поэтому настраиваем кодом.
			logger = Logger.getLogger(getClass().getName());
			logger.setLevel(Level.ALL);
			FileHandler fh = new FileHandler("huffmantests%g.log", 1_000_000, 10, true);
			fh.setLevel(Level.ALL);
			logger.addHandler(fh);
			logger.fine("HFArchiveTest logger is initialised.");
		}
	}

	@Test
	public void compressFile1() throws IOException
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(FileNotFoundException.class, () -> {
			ar.compressFile("");
		});

		assertEquals(" (The system cannot find the path specified)", thrown.getMessage());
	}

	@Test
	public void compressFile2() throws IOException
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(FileNotFoundException.class, () -> {
			ar.compressFile("1");
		});

		assertEquals("1 (The system cannot find the file specified)", thrown.getMessage());
	}

	@Test
	public void compressFile3() throws IOException
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(FileNotFoundException.class, () -> {
			ar.compressFile("1.txt");
		});

		assertEquals("1.txt (The system cannot find the file specified)", thrown.getMessage());
	}

	@Test
	public void compressFile4() throws IOException
	{
		String s = "iiiiiiiiiiiiiiiiiiiiiiffffffffffffffffffftttttttttttttttttttttt5555555555555555555555555yyyyyyyyyyyyyyyyyyyyyooooooooooooooooooooooooooddddddddddddddddddddd";
		ByteArrayInputStream in1 = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream in2 = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sout = out;
		fd.sin = in2;
		fd.fnUncompressed = "";
		fd.fnCompressed = "";
		ar.compressFileInternal(in1, fd);

		assertTrue("compressFileInternal returned zero encoded bytes!", fd.fzCompressed > 0);
		assertEquals(28, fd.lastBits);

	}

	@Test
	public void unCompressFile1()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(FileNotFoundException.class, () -> {
			ar.compressFile("");
		});

		assertEquals(" (The system cannot find the path specified)", thrown.getMessage());
	}


	@Test
	public void unCompressFile2()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(FileNotFoundException.class, () -> {
			ar.compressFile("4");
		});

		assertEquals("4 (The system cannot find the file specified)", thrown.getMessage());
	}

	@Test
	public void unCompressFile3()
	{
		HFArchiver ar = new HFArchiver();

		Throwable thrown = assertThrows(FileNotFoundException.class, () -> {
			ar.compressFile("5.hf");
		});

		assertEquals("5.hf (The system cannot find the file specified)", thrown.getMessage());
	}

	@Test
	public void compressUncomress1() throws IOException
	{
		String s = "iiiiiiiiiiiiiiiiiiiii5ffffffffffffffffffftttttttttttttttttttttti555555555555555555555555iyyyyyyyyyyyyyyyyyyyyy5oooooooooooooooooooooooo5ddddddddddddddddddddi";
		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(200);

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fnUncompressed = "";
		fd.fnCompressed = "";
		ar.compressFileInternal(inForTree, fd);


		assertEquals(56, fd.fzCompressed);
		assertTrue("compressFileInternal returned zero encoded bytes!", fd.fzCompressed > 0);
		assertEquals(133, outForCompress.size());
		assertEquals(28, fd.lastBits);

		byte[] arr = outForCompress.toByteArray();
		ByteArrayInputStream in3 = new ByteArrayInputStream(arr);
		ByteArrayOutputStream out3 = new ByteArrayOutputStream(200);

		HFUncompressor uc = new HFUncompressor();

		long tmp1 = fd.fzCompressed;
		byte tmp2 = fd.lastBits;
		fd.sin = in3;
		fd.sout = out3;
		fd.loadHeader();
		fd.fzCompressed = tmp1;
		fd.lastBits = tmp2;

		var tree = new HFTree(in3);
		tree.loadTable(fd.tableSize);

		uc.uncompress(tree, fd);

		assertEquals(s.getBytes().length, out3.size());

		String ss = out3.toString();
		logger.info(ss);

	}

	@Test
	public void compressUncomress2() throws IOException
	{
		String s = "1234567890qwertyuiopasdfghjklzxcvbnm,./;'[]1234567890qwertyuiopasdfghjklzxcvbnm,./;'[]1234567890qwertyuiopasdfghjklzxcvbnm,./;'[]1234567890qwertyuiopasdfghjklzxcvbnm,./;'[]";
		//String s = "[][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][][]";
		//String s = "123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123123";
		//String s = "XTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT";
		//String s = "UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU";
		ByteArrayInputStream inForTree = new ByteArrayInputStream(s.getBytes());
		ByteArrayInputStream inForCompress = new ByteArrayInputStream(s.getBytes());
		ByteArrayOutputStream outForCompress = new ByteArrayOutputStream(s.length());

		HFArchiver ar = new HFArchiver();
		HFFileData fd = new HFFileData();
		fd.sin = inForCompress;
		fd.sout = outForCompress;
		fd.fnUncompressed = "";
		fd.fnCompressed = "";
		ar.compressFileInternal(inForTree, fd);

		assertTrue("compressFileInternal returned zero encoded bytes!", fd.fzCompressed > 0);
		assertEquals(413, outForCompress.size());

		ByteArrayInputStream in3 = new ByteArrayInputStream(outForCompress.toByteArray());
		ByteArrayOutputStream out3 = new ByteArrayOutputStream();
		HFUncompressor uc = new HFUncompressor();
		fd.sin = in3;
		fd.sout = out3;
		uc.uncompress(ar.tree, fd);

		assertEquals(s.length(), out3.size());

	}

}