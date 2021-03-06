package db;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

/**
 * Data accessing object(DAO)
 * @author jero
 *
 */
public class MyDB {
	private SqlSessionFactoryBuilder builder;
	private SqlSessionFactory factory;

	public MyDB() {
		// Configure mybatis from mybatis-config.xml
		builder = new SqlSessionFactoryBuilder();
		factory = builder.build(Crawler.class.getResourceAsStream("/mybatis/mybatis-config.xml"));
	}

	public int insert(Policy policy) {
		int affectedRow = 0;

		try (SqlSession session = factory.openSession()) {
			affectedRow = session.insert("Policy.insert", policy);
			session.commit();
		}
		
		return affectedRow;
	}
	
	public List<Policy> selectById(int id){
		List<Policy> list=new ArrayList<Policy>();

		try (SqlSession session = factory.openSession()) {
			list = session.selectList("Policy.selectById",id);
			session.commit();
		}
		
		return list;
	}
	
	public List<DistinctPolicy> getAllDistinctPolicy(){
		List<DistinctPolicy> list =new ArrayList<DistinctPolicy>();
		
		try (SqlSession session = factory.openSession()) {
			list = session.selectList("Policy.getAllDistinctPolicy");
			session.commit();
		}
		
		return list;
	}
}
