import java.io.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.apache.commons.cli.*;

public class Main {

	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);

	public static void main(String[] args) throws IOException, ParseException
	{
		// ДЛЯ АРХИВИРОВАНИЯ МНОГИХ ФАЙЛОВ НУЖНО
		//  при разархивировании восстанавливать modified date.
		//  как хранить папку заархивированого файла - создавать внутри папку такую же как имя самого файла архива и "ложить файлы туда"
		//  архивацию всех файлов их папки пока не поддерживаем - либо обдумать как правильно хранить папки и файлы внутри архива.
		//  многопоточность для разных файлов. подумать над многопоточностью для одного файла.

		//Comparator<HfNode> comparator = (o1, o2) -> Integer.compare(o1.weight, o2.weight);

		InputStream is = Main.class.getResourceAsStream("huffmanlog.properties");
		if(is != null)
			LogManager.getLogManager().readConfiguration(is);
		else
			logger.fine("NULL returned - Main.class.getResourceAsStream(\"huffmanlog.properties\")");


		logger.info("ROMA archiver. Start.");

		CommandLine cmd = getCommandLine(args);

		if(cmd.hasOption('a'))
		{
			String[] ss = cmd.getOptionValues('a');
			//System.out.println(Arrays.toString(ss));
			if(cmd.hasOption("rle"))
			{
				var arc = new RLEArchiver();
				arc.compressFiles(ss);    // Note ss[0] is a name of archive while ss[1], ss[2] and so on - files to be added to archive
			}
			else if(cmd.hasOption("range"))
			{
				var arc = new RangeArchiver();
				arc.compressFiles(ss);
			}
			else if(cmd.hasOption("ra"))
			{
				var arc = new RangeAdaptArchiver();
				arc.compressFiles(ss);
			}
			else // default method is Hyffman
			{
				var arc = new HFArchiver();
				arc.compressFiles(ss);    // Note ss[0] is a name of archive while ss[1], ss[2] and so on - files to be added to archive
			}

		} else if(cmd.hasOption('x') || cmd.hasOption('e'))
		{
			String[] ss = cmd.getOptionValues('x');
			if(cmd.hasOption("rle"))
			{
				RLEArchiver arc = new RLEArchiver();
				arc.unCompressFiles(ss[0]);
			}
			else if(cmd.hasOption("range"))
			{
				var arc = new RangeArchiver();
				arc.unCompressFiles(ss[0]);
			}
			else if(cmd.hasOption("ra"))
			{
				var arc = new RangeAdaptArchiver();
				arc.unCompressFiles(ss[0]);
			}
			else // default method is Hyffman
			{
				HFArchiver arc = new HFArchiver();
				arc.unCompressFiles(ss[0]);
			}

		} else if(cmd.hasOption('l'))
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

		logger.info("ROMA archiver. Finished.");
	}


	final static Options options = new Options();
	private static CommandLine getCommandLine(String[] args) throws ParseException
	{

		options.addOption(Option.builder("a").argName("<archive> <files...>").hasArgs().desc("Add files to archive").build());
		options.addOption(Option.builder("x").argName("archive").hasArg().desc("Extract files with full path").build());
		options.addOption(Option.builder("e").argName("archive").hasArg().desc("Extract files without archived paths").build());
		options.addOption("l", true, "Display content of archive");
		options.addOption("hf", "huffman", false, "Use Huffman compression method (default)");
		options.addOption("rle", "runlength", false, "Use RLE (run length encoding) compression method");
		options.addOption("range", "arithmetic", false, "Use Arithmetic Range compression method");
		options.addOption("ra", "rangeadaptive", false, "Use Adaptive Arithmetic Range compression method");

		CommandLineParser parser = new DefaultParser();

		return parser.parse(options, args);
	}

}
