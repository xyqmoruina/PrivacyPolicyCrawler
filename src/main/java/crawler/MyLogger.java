package crawler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MyLogger {
	private String path;
	private String harPath;
	//private Path dir;

	/**
	 * Create logging directory specified by path
	 * @param path
	 */
	public MyLogger(String path) {
		//this.path=path;
		//dir=null;
		this.path=path+"+"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss"));//add timestamp
		this.harPath=this.path+"/har";
		createDir(this.path); //initial logging directory
		createDir(this.harPath); //default direcotory for har file
	}
	/**
	 * create new directory, delete before create if already exists
	 * @param path
	 */
	public void createDir(String path) {
		//Path dir;
		// initial logging directory
		try {
			//dir = Files.createDirectory(Paths.get(path));
			Files.createDirectory(Paths.get(path));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//dir = Paths.get(path);
			//System.out.println("Clean " + dir.getFileName());
			cleanDir(path);
		}
	}
	
	/**
	 * clean given directory
	 * @param dir
	 */
	public void cleanDir(String path){
		Path dir=Paths.get(path);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path file : stream) {
				// TODO: recursively
				System.out.println("Delete: " + dir.getFileName() + "/" + file.getFileName());
				Files.delete(file);
			}
		} catch (IOException | DirectoryIteratorException x) {
			// IOException can only be thrown by newDirectoryStream.
			System.err.println(x);
		}
	}
	/**
	 * Write to file under logging directory, clean content if file already
	 * exist
	 *
	 * @param fileName
	 *            File name
	 * @param text
	 *            Content to write
	 */
	public void toFile(String fileName, String text) {
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(path, fileName))) {
			bw.write(text);
			bw.newLine();
		} catch (IOException e) {
			System.err.println(e);
		}
	}

	/**
	 * Write to log file under logging directory
	 * 
	 * @param logFile
	 *            File name
	 * @param text
	 *            Content to log
	 * 
	 */
	public void toLog(String logFile, String text) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(path + "/" + logFile, true))) {
			bw.write(text);
			bw.newLine();
		} catch (IOException e) {
			System.err.println(e);
		}
	}
	
	public String getDir(){
		return path;
	}
	
	public String getHarDir(){
		return harPath;
	}
	public static void main(String[] args) throws IOException {
		// for(File file: dir.listFiles()) file.delete();

		MyLogger logger = new MyLogger("links/test");
		logger.toFile("1.txt", "F321321\n");
		logger.toFile("1.txt", "fdsafdsa\n");
		logger.toLog("log.txt", "213213\n");
		logger.toLog("log.txt", "uuuun");

	}
}
