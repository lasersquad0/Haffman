import java.io.*;
import java.util.*;

public class Main {

	public static void main(String[] args) throws IOException
	{
		//final int MAX_BUF_SIZE = 1_000_000_000;
		//final int FILE_BUFFER;
		final String FN_TO_COMPRESS   = "Война и мир.txt"; //"1.docx";//"Apps.index"; //"voyna-i-mir-tom-1.txt"; "primes - 20000G small.txt";
		//final String FN_COMPRESSED    = "primes - 20000G small.hf";
		final String FN_TO_UNCOMPRESS =  "Война и мир.hf"; //"primes - 20000G small.hf";
		//final String FN_UNCOMPRESSED  = "primes - 20000G small 2.txt";//"Война и мир2.txt";

		//Comparator<HfNode> comparator = (o1, o2) -> Integer.compare(o1.weight, o2.weight);
		System.out.println("Start.");

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

		System.out.println("Finished.");
	}



	private static void printNodes(ArrayList<HFNode> nodes)
	{
		for (HFNode node : nodes)
		{
			System.out.format("'%s'=%d  ", node.symbol, node.weight);
		}
		System.out.println();
	}

	private static String printTree(HFNode nd)
	{
		String res = nd.symbol + "-";
		if(nd.leftNode != null)
			res += printTree(nd.leftNode);
		if(nd.rightNode != null)
			res += printTree(nd.rightNode);

		return res;
	}

}
