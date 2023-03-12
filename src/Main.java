import java.io.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import jdk.jshell.execution.Util;
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

			if(cmd.hasOption("b"))
			{
				executeOptionB(cmd);
			}

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
			else if(cmd.hasOption("range32"))
			{
				var arc = new RangeArchiver(Utils.CompTypes.ARITHMETIC32);
				arc.compressFiles(ss);
			}
			else if(cmd.hasOption("range64"))
			{
				var arc = new RangeArchiver(Utils.CompTypes.ARITHMETIC64);
				arc.compressFiles(ss);
			}
			else if(cmd.hasOption("ra"))
			{
				var arc = new RangeAdaptArchiver();
				arc.compressFiles(ss);
			}
			else if(cmd.hasOption("ra32"))
			{
				var arc = new RangeAdaptArchiver(Utils.CompTypes.AARITHMETIC32);
				arc.compressFiles(ss);
			}
			else if(cmd.hasOption("ra64"))
			{
				var arc = new RangeAdaptArchiver(Utils.CompTypes.AARITHMETIC64);
				arc.compressFiles(ss);
			}
			else if(cmd.hasOption("hf"))
			{
				var arc = new HFArchiver();
				arc.compressFiles(ss);    // Note ss[0] is a name of archive while ss[1], ss[2] and so on - files to be added to archive
			}
			else // default method is Hyffman
			{
				logger.warning("Compression algorithm is not specified, using Huffman by default.");
				var arc = new HFAdaptArchiver();
				arc.compressFiles(ss);    // Note ss[0] is a name of archive while ss[1], ss[2] and so on - files to be added to archive
			}

		} else if(cmd.hasOption('x') || cmd.hasOption('e'))
		{
			String[] ss = cmd.getOptionValues('x');
			ArchiveManager.uncompressFiles(ss[0]); // it gets info about needed compressor from FileRec of each file in archive, and calls appropriate compressor respectively.

		} else if(cmd.hasOption('l'))
		{
			String[] ss = cmd.getOptionValues('l');
			HFArchiver arc = new HFArchiver();
			arc.listFiles(ss[0], cmd.hasOption('v'));

		} else
		{
			HelpFormatter formatter = new HelpFormatter();
			formatter.setWidth(formatter.getWidth() * 2);
			formatter.printHelp("Huffman", options, true);
		}

		logger.info("ROMA archiver. Finished.");
	}

	private static void executeOptionB(CommandLine cmd)
	{
		int factor = 1;
		String s = cmd.getOptionValue('b').toUpperCase();
		if(s.lastIndexOf("K") > 0)
		{
			s = s.substring(0, s.length()-1);
			factor = 1_024;
		}
		else if(s.lastIndexOf("M") > 0)
		{
			s = s.substring(0, s.length()-1);
			factor = 1024*1024;
		}

		try
		{
			final int MIN_BSIZE = 1000;
			final int MAX_BSIZE = 300_000_000;
			int tmp = Integer.parseInt(s) * factor;
			if( (tmp < MIN_BSIZE) || (tmp > MAX_BSIZE) )
				logger.info(String.format("Wrong value for option '-b'. Block size should be in interval from %d to %d", MIN_BSIZE, MAX_BSIZE));
			else
				Utils.BLOCK_SIZE = tmp;
		}
		catch(NumberFormatException nfe)
		{
			logger.info("Wrong value for command line option '-b'.");
		}
	}


	final static Options options = new Options();
	private static CommandLine getCommandLine(String[] args) throws ParseException
	{
		options.addOption(Option.builder("v").longOpt("verbose").desc("Print more detailed (verbose) information to screen.").build());
		options.addOption(Option.builder("SIZE").longOpt("b").hasArg().desc("Use specified block size").build());
		options.addOption(Option.builder("a").argName("<archive> <files...>").hasArgs().desc("Add files to archive").build());
		options.addOption(Option.builder("x").argName("archive").hasArg().desc("Extract files with full path").build());
		options.addOption(Option.builder("e").argName("archive").hasArg().desc("Extract files without archived paths").build());
		options.addOption(Option.builder("l").argName("archive").hasArg().desc("Display content of archive").build());
		options.addOption("hf", "huffman", false, "Use Huffman compression method (default)");
		options.addOption("rle", "runlength", false, "Use RLE (run length encoding) compression method");
		options.addOption("range", "arithmetic", false, "Use Arithmetic Range compression method (default)");
		options.addOption("range32", "arithmetic32", false, "Use Arithmetic Range compression method 32 bit");
		options.addOption("range64", "arithmetic64", false, "Use Arithmetic Range compression method 64 bit");
		options.addOption("ra", "rangeadaptive", false, "Use Adaptive Arithmetic Range compression method (default)");
		options.addOption("ra32", "rangeadaptive32", false, "Use Adaptive Arithmetic Range compression method 32 bit");
		options.addOption("ra64", "rangeadaptive64", false, "Use Adaptive Arithmetic Range compression method 64 bit");

		CommandLineParser parser = new DefaultParser();

		return parser.parse(options, args);
	}
}
