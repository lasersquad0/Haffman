import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.*;

import static org.junit.Assert.*;


public class HFTreeTest {

	private static Logger logger;

	public HFTreeTest() throws IOException
	{
		if(logger == null)
		{
			// Настраиваем логгер что бы тесты писались в другой файл. Одной конфигураций в JUL такое настроить нельзя. Поэтому настраиваем кодом.
			logger = Logger.getLogger(getClass().getName());
			logger.setLevel(Level.ALL);
			FileHandler fh = new FileHandler("huffmantests%g.log", 1_000_000, 10, true);
			fh.setLevel(Level.ALL);
			logger.addHandler(fh);
			logger.fine("HFTreeTest logger is initialised.");
		}

		logger.fine("HFTreeTest constructor called.");
	}


	@Test
	public void calcWeights1() throws IOException
	{
		// Эмулируем файл длиной сего 1 байт
		byte[] buf = {'f'};
		ByteArrayInputStream bs = new ByteArrayInputStream(buf);
		HFTree hft = new HFTree(bs);

		hft.calcWeights();
		assertNotNull(hft.symbols);
		assertNotNull(hft.weights);

		/*Throwable thrown = assertThrows(IOException.class, () -> {
			hft.calcWeights();
		});
		assertNotNull(thrown.getMessage());
		 */
	}

	@Test
	public void calcWeights21() throws IOException
	{
		// Эмулируем пустой файл. 0 байт длиной.
		byte[] buf = {};
		ByteArrayInputStream bs = new ByteArrayInputStream(buf);
		HFTree hft = new HFTree(bs);

		hft.calcWeights();

		assertNotNull(hft.symbols); // notnull потому что new int[0] отрабатывает успешно (массив с 0 элементами создается)
		assertNotNull(hft.weights);
	}
	@Test
	public void calcWeights2() throws IOException
	{
		// Эмулируем пустой файл. 0 байт длиной.
		byte[] buf = {};
		ByteArrayInputStream bs = new ByteArrayInputStream(buf);
		HFTree hft = new HFTree(bs);

		assertNull(hft.symbols);
		assertNull(hft.weights);
		/*Throwable thrown = assertThrows(AssertionError.class, () -> {
			hft.calcWeights();
		});
		assertNotNull(thrown.getMessage());*/
	}

	@Test
	public void calcWeights3() throws IOException
	{
		// Эмулируем правильный файл небольшого размера
		String s = "20000000000021,20000000000041,20000000000089,20000000000111,20000000000117,20000000000119,20000000000129,20000000000143,20000000000147,20000000000207,20000000000213,20000000000219,20000000000257,20000000000317,20000000000339,";
		byte[] buf = s.getBytes();
		ByteArrayInputStream bs = new ByteArrayInputStream(buf);
		HFTree hft = new HFTree(bs);

		hft.calcWeights();

		assertNotNull(hft.symbols);
		assertNotNull(hft.weights);
		int[] a = {44, 48, 49, 50, 51, 52, 53, 55, 56, 57};
		int[] b = {15, 154, 15, 21, 5, 3, 1, 5, 1, 5};
		assertTrue("Symbols aray is incorrect.", Arrays.equals(hft.symbols, a));
		assertTrue("Weights aray is incorrect.", Arrays.equals(hft.weights, b));
		assertEquals("CRC32 value does not match.", 2086686246, hft.CRC32Value);
	}

	@Test
	public void buildTree1()
	{
		byte[] buf = {};
		ByteArrayInputStream bs = new ByteArrayInputStream(buf);
		HFTree hft = new HFTree(bs);

		Throwable thrown = assertThrows(AssertionError.class, () -> {
			hft.buildTree();
		});
		//assertNotNull(thrown.getMessage());

	}

	@Test
	public void buildTree11() throws IOException
	{
		byte[] buf = {};
		ByteArrayInputStream bs = new ByteArrayInputStream(buf);
		HFTree hft = new HFTree(bs);

		hft.calcWeights();
		assertNotNull(hft.symbols);
		assertNotNull(hft.weights);
		assertEquals(0, hft.symbols.length);
		assertEquals(0, hft.weights.length);

		Throwable thrown = assertThrows(AssertionError.class, () -> {
			hft.buildTree();
		});
		//assertNotNull(thrown.getMessage());

	}
	@Test
	public void buildTree2() throws IOException
	{
		byte[] buf = {'f'};
		ByteArrayInputStream bs = new ByteArrayInputStream(buf);
		HFTree hft = new HFTree(bs);

		hft.calcWeights();
		assertNotNull(hft.symbols);
		assertNotNull(hft.weights);
		assertEquals(1, hft.symbols.length);
		assertEquals(1, hft.weights.length);

		hft.buildTree();

		assertNotNull(hft.treeRoot);
		assertNotNull(hft.symbols);
		assertNotNull(hft.weights);
		assertEquals(1, hft.symbols.length);
		assertEquals(1, hft.weights.length);
	}

	@Test
	public void buildTree3() throws IOException
	{
		byte[] buf = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		ByteArrayInputStream bs = new ByteArrayInputStream(buf);
		HFTree hft = new HFTree(bs);

		hft.build();

		assertNotNull(hft.symbols);
		assertNotNull(hft.weights);
		assertNotNull(hft.treeRoot);
		assertNull(hft.treeRoot.leftNode);
		assertNull(hft.treeRoot.rightNode);
		assertEquals(0, hft.treeRoot.symbol);
		assertEquals(1, hft.codesList.size());
		assertEquals(1, hft.codesMap.size());

	}
}