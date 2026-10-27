package com.aditisingh.Quicklink;

import com.aditisingh.Quicklink.support.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: the whole application context starts, Flyway migrates a real Postgres,
 * and Hibernate's ddl-auto=validate confirms the entities match the migrated schema.
 */
@SpringBootTest
class QuicklinkApplicationTests extends AbstractPostgresTest {

	@Test
	void contextLoads() {
	}

}
