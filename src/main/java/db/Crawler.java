package db;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import extractor.PolicyExtractor;

public class Crawler {

	public static void main(String args[]) throws IOException {

		// Configure mybatis from mybatis-config.xml
		SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
		System.out.println(Resources.getResourceURL("mybatis"));
		SqlSessionFactory factory = builder.build(Crawler.class.getResourceAsStream("/mybatis/mybatis-config.xml"));// rely
																													// on
																													// mybatis

		// Executor executor = Executors.newFixedThreadPool(20);
		ExecutorService executor = Executors.newFixedThreadPool(20);
		List<Future<?>> future =new ArrayList<Future<?>>();
		List<Policy> list;
		try (SqlSession session = factory.openSession()) {
			list = session.selectList("Policy.getFailed", 1644);
			session.commit();
		}
		int c = 0;
		for (Policy p : list) {
			System.out.println(c++ + "\t" + p.getId() + "\t" + p.getMainsite() + "\t" + p.getConnectingUrl() + "\t"
					+ p.getPolicyUrl());
			Runnable task = () -> {
				PolicyExtractor extractor = new PolicyExtractor(true);
				extractor.extract(p.getConnectingUrl());
				p.setPolicyUrl(extractor.getPolicyUrl());
				p.setPolicy(extractor.getPolicy());
				extractor.quit();
				try (SqlSession session = factory.openSession()) {
					session.update("Policy.updatePolicyUrlById", p);
					session.commit();
				}
				

			};
			// executor.execute(task);
			//callables.add(task);
			future.add(executor.submit(task));
		}
		for(Future<?> f:future){
			try {
				f.get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();}
		}
		try {
		    System.out.println("attempt to shutdown executor");
		    executor.shutdown();
		    executor.awaitTermination(5, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
		    System.err.println("tasks interrupted");
		}
		finally {
		    if (!executor.isTerminated()) {
		        System.err.println("cancel non-finished tasks");
		    }
		    executor.shutdownNow();
		    System.out.println("shutdown finished");
		}
		c = 0;

		/*
		 * try (BufferedWriter bw =
		 * Files.newBufferedWriter(Paths.get("test.txt"),
		 * StandardOpenOption.APPEND)) { PolicyExtractor extractor = new
		 * PolicyExtractor(true); for (Policy p : list) {
		 * 
		 * extractor.extract(p.getConnectingUrl()); bw.write(c++ + "\t" +
		 * p.getId() + "\t" + p.getMainsite() + "\t" + p.getConnectingUrl() +
		 * "\t" + extractor.getPolicyUrl()); bw.newLine();
		 * System.out.println(c++ + "\t" + p.getId() + "\t" + p.getMainsite() +
		 * "\t" + p.getConnectingUrl() + "\t" + extractor.getPolicyUrl()); }
		 * extractor.quit(); }
		 */
		Policy p = new Policy("", "", "fds","", 0, "", "");
		p.setId(1804);
		// System.out.println(session.update("Policy.updatePolicyUrlByID", p));
		// session.commit();
		// session.close();

	}
}
