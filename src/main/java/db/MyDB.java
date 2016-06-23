package db;

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
}
