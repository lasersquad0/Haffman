import java.io.*;
import java.time.LocalTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

	private final static Logger logger = Logger.getLogger("HFLogger");

	public static void main(String[] args) throws IOException
	{
		//Comparator<HfNode> comparator = (o1, o2) -> Integer.compare(o1.weight, o2.weight);

		//final int MAX_BUF_SIZE = 1_000_000_000;
		//final int FILE_BUFFER;
		final String FN_TO_COMPRESS   = "primes - 20000-20010G.txt"; //"primes - 20000G small.txt"; //"primes - 0-10G.txt";  //Война и мир.txt"; //"1.docx";//"Apps.index"; //"voyna-i-mir-tom-1.txt";

		final String FN_TO_UNCOMPRESS = "primes - 20000-20010G.hf"; //"primes - 0-10G.hf"; //Война и мир.hf"; "primes - 20000G small.hf";

		logger.info("Huffman archiver. Start.");

		HFArchiver arc = new HFArchiver();
		//arc.compressFile(FN_TO_COMPRESS);
		arc.unCompressFile(FN_TO_UNCOMPRESS);


		/*
		File inputFile= new File(FN_TO_COMPRESS);
		FILE_BUFFER = (inputFile.length() < MAX_BUF_SIZE) ? (int)inputFile.length(): MAX_BUF_SIZE;

		InputStream cin = new BufferedInputStream(new FileInputStream(inputFile),FILE_BUFFER);
		HFTree tree = new HFTree(cin);
		tree.calcWeights();
		tree.build();

		 */

		//System.out.format("Total bytes compressed: %d\n", total);
/*
		System.out.format("Codes: %s\n", codes.toString());
		System.out.format("CodeLens: %s\n", codeLen.toString());
		System.out.format("rcodes3 %s\n", rcodes3.toString());
		System.out.format("rcodes4 %s\n", rcodes4.toString());


		DataOutputStream cout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(FN_COMPRESSED), FILE_BUFFER));
*/

		/*
		File inputFile = new File(FN_TO_UNCOMPRESS);
		FILE_BUFFER = (inputFile.length() < MAX_BUF_SIZE) ? (int)inputFile.length(): MAX_BUF_SIZE;

		DataInputStream uin = new DataInputStream(new BufferedInputStream(new FileInputStream(inputFile), FILE_BUFFER));
		DataOutputStream uout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(FN_UNCOMPRESSED), FILE_BUFFER));

		hf.uncompressHaffman(uin, uout);
		uout.close();
*/

		logger.info("Finished.");
	}

}
