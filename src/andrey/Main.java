package andrey;

import java.io.*;


public class Main {
	public static void main(String[] args) throws IOException
	{
		System.out.println("andrey.Main start.");

		//final String INPUT_FILENAME = "archive.hf";

		//DataInputStream reader = new DataInputStream(new BufferedInputStream(new FileInputStream("archive.hf")));

//		var fi = new FileInputStream("archive.hf");
//		byte[] arr = fi.readAllBytes();

//		System.out.println(arr.length);


		DataInputStream reader = new DataInputStream(new FileInputStream("archive.hf"));
		var bw = new BufferedWriter(new FileWriter("codes.txt"), 1000);

		int count = 0;
		byte rd;
		try
		{
			while (true)
			{
				rd = reader.readByte();
				count++;
				String s = Integer.toBinaryString(((int) rd) & 0xFF);
				bw.write(s);
				//	bw.write("  " + Byte.valueOf(rd).toString());
				bw.newLine();
			}
		}
		catch (Exception E)
		{
			bw.flush();
			bw.close();

		}

		System.out.format("read bytes count: %d\n",count);

		/*
		final String INPUT_FILENAME = "../PrimesStat/pri mes - 2000-2100G.txt";



		long[] freq = new long[255]; //цифры и запятая

		final int READ_BUFFER = 1_000_000_000;
		char[] buffer = new char[READ_BUFFER];

		var br = new BufferedReader(new FileReader(INPUT_FILENAME), READ_BUFFER);

		System.out.format("Reading file '%s'...\n", INPUT_FILENAME);

		int cntRead;
		do
		{
			cntRead = br.read(buffer);
			if (cntRead == -1) break;

			for (int i = 0; i < cntRead; i++)
				freq[buffer[i]]++;

			System.out.print('.');

		}while(cntRead == READ_BUFFER);

		System.out.print('\n');
		System.out.println(Arrays.toString(freq));

		long sum = 0;
		for (int i = 0; i < freq.length; i++)
		{
			sum += freq[i];
		}

		var bw = new BufferedWriter(new FileWriter("frq.txt"), 1000);

		String s = "";
		for (int i = 0; i < freq.length; i++)
		{
			if(freq[i] > 0)
			{
				long percent = (long)Math.round(100*(double)freq[i]/(double)sum);
				s += String.format("%c=%d  %d%%\n", (char) (i), freq[i], percent);
			}
		}

		bw.write(s);
		bw.flush();
		bw.close();
*/
		System.out.println("Finished.");
	}

}
