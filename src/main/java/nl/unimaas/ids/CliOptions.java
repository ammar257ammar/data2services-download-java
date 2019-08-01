package nl.unimaas.ids;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "data2services-download-java")
public class CliOptions {
	@Option(names = { "-h", "--help" }, usageHelp = true, description = "Display a help message")
	boolean help = false;
	
	@Option(names= {"-ds", "--download-datasets"}, description = "Comma-separated list of datasets to download")
	String datasets = null;
	
	@Option(names= {"-dp", "--download-path"}, description = "downloads destination absolute path, should be a folder")
	String path = null;
	
	@Option(names= {"-dcsv", "--datasets-csv"}, description = "CSV containg datasets with base URIs and filenames to download")
	String csvPath = null;
	
	@Option(names= {"-im", "--if-modified"}, description = "Download only the files if they are modified from last time")
	boolean ifModified = false;
	
	@Option(names= {"-ihash", "--if-hash-changed"}, description = "Download only the files if they are modified from last time")
	boolean ifHashChanged = false;

	@Option(names= {"-un", "--username"}, description = "Username used for triplestore authentication")
	String username = null;

	@Option(names= {"-pw", "--password"}, description = "Password used for triplestore authentication")
	String password = null;


}