package nl.unimaas.ids;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import com.opencsv.CSVReader;


import picocli.CommandLine;

public class DownloadOperations {

	public static void main(String[] args) throws Exception {
		try { 

			CliOptions cli = CommandLine.populateCommand(new CliOptions(), args);
			
			if(cli.help)
				printUsageAndExit();
			
			if (cli.path.endsWith("/"))
				cli.path = cli.path.substring(0, cli.path.length() - 1);
			
			if(cli.csvPath == null) {
				printUsageAndExit(new FileNotFoundException("The CSV file is not found"));
			}
			
			if(cli.datasets != null && cli.path != null) {
				
				String[] datasetsArr = cli.datasets.split(",");
				
				File dataFolder = new File(cli.path);
				
				if(!dataFolder.exists()) {
					dataFolder.mkdirs();
				}
				
				if(!dataFolder.isDirectory()) {
					printUsageAndExit(new FileNotFoundException("The path entered is not a folder"));
				}
				
				Reader reader = Files.newBufferedReader(Paths.get(cli.csvPath));
	            CSVReader csvReader = new CSVReader(reader);
	            
	            List<String[]> listDatasets = new ArrayList<>();
	            listDatasets = csvReader.readAll();
	            reader.close();
	            csvReader.close();
				
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh");
				
				for(String dataset: datasetsArr) {
					
					String baseUri = "";
					String filenames = "";
					
					File datasetFolder = new File(cli.path+"/"+dataset);
					
					//System.out.println(datasetFolder.getAbsolutePath());
					
					if(!datasetFolder.exists()) {
						datasetFolder.mkdirs();
					}
					
					if(!datasetFolder.isDirectory()) {
						printUsageAndExit(new FileNotFoundException("The dataset path is not a folder"));
					}
					
					for(int i = 0; i < listDatasets.size(); i++) {
						
						if(listDatasets.get(i)[0].equals(dataset)) {
							
							baseUri = listDatasets.get(i)[1];
							filenames = listDatasets.get(i)[2];
							
							if (baseUri.endsWith("/"))
								baseUri = baseUri.substring(0, baseUri.length() - 1);
							
							break;
						}
					}
					
					if(baseUri.equals("") || filenames.equals("")) {
						printUsageAndExit(new FileNotFoundException("Dataset entry is missing from the CSV file"));
					}
					
					if(datasetFolder.list().length  == 0 
							|| ((datasetFolder.list().length  != 0 && cli.ifModified != true) 
							&& (datasetFolder.list().length  != 0 && cli.ifHashChanged != true))) {
						
						for(String filename: filenames.split(",")) {

							String remoteName = filename; 
							
							Date date =  getNewestVersion(baseUri, remoteName);

							//System.out.println(date.toString());
							
							String ext = filename.substring(filename.lastIndexOf("."));
							
							filename = filename.substring(0, filename.lastIndexOf("."));
														
							filename = filename+dateFormat.format(date)+ext;
							
							downloadFile(baseUri, remoteName, filename, datasetFolder);
							System.out.println("Downloading file: " + filename);
						}

					}else{	
						
						for(String filename: filenames.split(",")) {
							
							String remoteName = filename; 
							
							Date date =  getNewestVersion(baseUri, remoteName);
							
							//System.out.println(date.toString());
							
							String ext = filename.substring(filename.lastIndexOf("."));
							
							filename = filename.substring(0, filename.lastIndexOf("."));
							
							List<Date> versions = new ArrayList<Date>(); 
														
							for(File version: datasetFolder.listFiles()) {
								
								String versionStr = version.getName();
																
								if(versionStr.startsWith(filename)) {
									
									versionStr = versionStr.substring(filename.length(), versionStr.lastIndexOf("."));
									versions.add(dateFormat.parse(versionStr));
								}	
								
							}
							
							Collections.sort(versions, new Comparator<Date>(){
								 
					            @Override
					            public int compare(Date o1, Date o2) {
					                return o1.compareTo(o2);
					            }
					        });
							
							if(cli.ifModified) {
								
								if(dateFormat.parse(dateFormat.format(date)).compareTo(versions.get(versions.size()-1)) > 0) {
									
									filename = filename+dateFormat.format(date)+ext;
									
									downloadFile(baseUri, remoteName, filename, datasetFolder);
									System.out.println("Downloading file: " + filename);
									
								}else {
									System.out.println("Last-Modified is the same - download ignored");
								}
								
							}else if(cli.ifHashChanged) {
								
								String hashFile = filename+dateFormat.format(versions.get(versions.size()-1))+".crc32";
								
								long crc32 = 0;
								
								try (BufferedReader br = new BufferedReader(new FileReader(datasetFolder.getAbsolutePath()+"/"+hashFile))) {
									crc32 = Long.parseLong(br.readLine().trim());	
								}
								
								filename = filename+dateFormat.format(date)+ext;
								
								//System.out.println(crc32 + " - " + getChecksumFromURL(baseUri, remoteName));
								
								if(crc32 != getChecksumFromURL(baseUri, remoteName)) {
									downloadFile(baseUri, remoteName, filename, datasetFolder);
									System.out.println("Downloading file: " + filename);
								}else {
									System.out.println("hash is the same - download ignored");
								}
								
							}
							
							//System.out.println(dateFormat.parse(dateFormat.format(date)) + " - " + versions.get(versions.size()-1));

							
							
						}		
					
					}
					
				}
				
				
				
			}else {
				printUsageAndExit(new Exception("datasets and path parameters should be set properly"));
			}

			
		} catch (Exception e) {
			printUsageAndExit(e);
		}
	}
	
	private static Date getNewestVersion(String baseUri, String remoteName) throws IOException, NoSuchAlgorithmException, KeyManagementException, ParseException {
		
		SSLContext context = SSLContext.getInstance("TLSv1.2");

		context.init(null, null, null);

		HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

		URL url = new URL(baseUri+"/"+remoteName);
		
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

		//System.out.println("inside function "+ connection.getLastModified());

		return new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z").parse(connection.getHeaderField("Last-Modified"));
	}
	
	private static long getChecksumFromURL(String baseUri, String remoteName) throws NoSuchAlgorithmException, KeyManagementException, IOException {
		
		SSLContext context = SSLContext.getInstance("TLSv1.2");

		context.init(null, null, null);

		HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

		URL url = new URL(baseUri+"/"+remoteName);
		
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
		
		InputStream inputStream = connection.getInputStream();
                 
        Checksum cs = new CRC32();
        
        int bytesRead = -1;
        byte[] buffer = new byte[8*1024];
        
        while ((bytesRead = inputStream.read(buffer)) != -1) {
        	
            cs.update(buffer, 0, bytesRead);
        }

        inputStream.close();
				
		
		return cs.getValue();
	}
	
	private static void downloadFile(String baseUri, String remoteName, String filename, File versionFolder) throws MalformedURLException, IOException, NoSuchAlgorithmException, KeyManagementException {
		
		SSLContext context = SSLContext.getInstance("TLSv1.2");

		context.init(null, null, null);

		HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

		URL url = new URL(baseUri+"/"+remoteName);
		
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
		
		InputStream inputStream = connection.getInputStream();
                 
        FileOutputStream outputStream = new FileOutputStream(new File(versionFolder.getAbsolutePath()+"/"+filename));

        Checksum cs = new CRC32();
        
        int bytesRead = -1;
        byte[] buffer = new byte[8*1024];
        
        while ((bytesRead = inputStream.read(buffer)) != -1) {
        	
            outputStream.write(buffer, 0, bytesRead);
            cs.update(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();
 
        
        try (PrintWriter out = new PrintWriter(versionFolder.getAbsolutePath()+"/"+filename.substring(0, filename.lastIndexOf("."))+".crc32")) {
            out.println(cs.getValue());
        }
        
        
	}
	
	private static void printUsageAndExit() {
		printUsageAndExit(null);
	}
	
	private static void printUsageAndExit(Throwable e) {
		CommandLine.usage(new CliOptions(), System.out);
		if(e == null)
			System.exit(0);
		e.printStackTrace();
		System.exit(-1);
	}
}