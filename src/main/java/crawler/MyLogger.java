package crawler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MyLogger {
	private String path;
	private Path dir;
	
	/**
	 * Create logging directory specified by path
	 * @param path
	 * @throws IOException
	 */
	public MyLogger(String path) {
		this.path=path;
		dir=null;
		//initial logging directory
		try {
			dir = Files.createDirectory(Paths.get(path));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			dir=Paths.get(path);
			System.out.println("Clean "+dir.getFileName());
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			    for (Path file: stream) {
			    	//TODO: recursively
			        System.out.println("Delete: "+dir.getFileName()+"/"+file.getFileName());
			        Files.delete(file);
			    }
			} catch (IOException | DirectoryIteratorException x) {
			    // IOException can only be thrown by newDirectoryStream.
			    System.err.println(x);
			}
		}
	}
	
	/**
	 * Write to file under logging directory, clean content if file already exist
	 *
	 * @param fileName	File name  
	 * @param text	Content to write
	 * @throws IOException
	 */
	public void toFile(String fileName, String text) {
		try(BufferedWriter bw=Files.newBufferedWriter(Paths.get(dir.toString(), fileName))){
			bw.write(text);
			bw.newLine();
		} catch(IOException e){
			System.err.println(e);
		}
	}
	
	/**
	 * Write to log file under logging directory
	 * @param logFile File name
	 * @param text Content to log
	 * 
	 */
	public void toLog(String logFile, String text) {
		try(BufferedWriter bw=new BufferedWriter(new FileWriter(dir.toString()+"/"+logFile,true))){
			bw.write(text);
			bw.newLine();
		} catch(IOException e){
			System.err.println(e);
		}
	}
	public static void main(String[] args) throws IOException{
		//for(File file: dir.listFiles()) file.delete();
		
		MyLogger logger=new MyLogger("dir/dir");
		logger.toFile("1.txt", "F321321\n");
		logger.toFile("1.txt", "fdsafdsa\n");
		logger.toLog("log.txt", "213213\n");
		logger.toLog("log.txt", "uuuun");
	
	}
}
