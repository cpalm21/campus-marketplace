package edu.udel.campusmarketplace.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import edu.udel.campusmarketplace.auth.VerifiedUser;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DATABASE_URL", matches = ".+")
class UserRegistrationIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private UserService service;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void concurrentFirstRegistrationsConvergeOnOneProfile() throws Exception {
        String uid = "registration-" + UUID.randomUUID();
        String email = uid + "@udel.edu";
        UUID university = users.findUniversityIdByEmailDomain("UDEL.EDU").orElseThrow();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var register = (java.util.concurrent.Callable<User>) () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                return users.insert(new NewUser(uid, email, university));
            };
            var first = executor.submit(register);
            var second = executor.submit(register);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS).userId()).isEqualTo(second.get(15, TimeUnit.SECONDS).userId());
            assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE firebase_uid = ?", Integer.class, uid)).isEqualTo(1);
            assertThat(service.register(new VerifiedUser(uid, email, false)).email()).isEqualTo(email);
        } finally {
            start.countDown();
            jdbc.update("DELETE FROM users WHERE firebase_uid = ?", uid);
        }
    }
}
