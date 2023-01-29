import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;
import org.apache.commons.cli.*;

public class Main {

	private final static Logger logger = Logger.getLogger("HFLogger");

	public static void main(String[] args) throws IOException, ParseException
	{
		// ДЛЯ АРХИВИРОВАНИЯ МНОГИХ ФАЙЛОВ НУЖНО
		//  при разархивировании восстанавливать modified date.
		//  как хранить папку заархивированого файла - создавать внутри папку такую же как имя самого файла архива и "ложить файлы туда"
		//  архивацию всех файлов их папки пока не поддерживаем - либо обдумать как правильно хранить папки и файлы внутри архива.
		//  многопоточность для разных файлов. подумать над многопоточностью для одного файла.


		//Comparator<HfNode> comparator = (o1, o2) -> Integer.compare(o1.weight, o2.weight);
//		final String FN_TO_COMPRESS   = "voyna-i-mir-tom-1.txt";//"primes - 20000G small.txt"; //"slack.exe";//"primes - 20000-20010G.txt"; //"oneletter.txt"; //"primes - 20000G small.txt"; //"primes - 0-10G.txt";  //Война и мир.txt"; //
//		final String FN_TO_UNCOMPRESS = "voyna-i-mir-tom-1.hf";//"primes - 20000G small.hf";//"slack.hf"; //"primes - 20000-20010G.hf"; //"oneletter.hf";// "primes - 0-10G.hf"; //Война и мир.hf"; "primes - 20000G small.hf";

		//LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("huffmanlog.properties"));

		logger.info("Huffman archiver. Start.");

//		var arg = new String[]{"-a", "archive.hf", "voyna-i-mir-tom-1.txt", "primes - 20000-20010G.txt", "primes - 20000G small.txt", "Война и мир.txt"};

		CommandLine cmd = getCommandLine(args);

		if(cmd.hasOption('a'))
		{
			String[] ss = cmd.getOptionValues('a');
			System.out.println(Arrays.toString(ss));
			HFArchiver arc = new HFArchiver();
			arc.compressFile2(ss); 	// Note ss[0] is a name of archive while ss[1], ss[2] and so on - files to be added to archive

		} else if(cmd.hasOption('x'))
		{
			String[] ss = cmd.getOptionValues('x');
			HFArchiver arc = new HFArchiver();
			arc.unCompressFile2(ss[0]);

		} else if(cmd.hasOption('e'))
		{
			String[] ss = cmd.getOptionValues('e');
			HFArchiver arc = new HFArchiver();
			arc.unCompressFile2(ss[0]);

		}else if(cmd.hasOption('l'))
		{
			String[] ss = cmd.getOptionValues('l');
			HFArchiver arc = new HFArchiver();
			arc.listFiles(ss[0]);

		} else
		{
			HelpFormatter formatter = new HelpFormatter();
			formatter.setWidth(formatter.getWidth() * 2);
			formatter.printHelp("Huffman", options, true);
		}

		logger.info("Finished.");
	}


	final static Options options = new Options();
	private static CommandLine getCommandLine(String[] args) throws ParseException
	{
		options.addOption("l", true, "Display content of archive");
		options.addOption(Option.builder("a").argName("<archive> <files...>").hasArgs().desc("Add files to archive").build());
		options.addOption(Option.builder("x").argName("archive").hasArg().desc("Extract files with full path").build());
		options.addOption(Option.builder("e").argName("archive").hasArg().desc("Extract files without archived paths").build());

		CommandLineParser parser = new DefaultParser();

		return parser.parse(options, args);
	}

}
