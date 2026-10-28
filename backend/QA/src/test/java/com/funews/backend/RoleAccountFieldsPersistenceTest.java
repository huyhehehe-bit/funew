package com.funews.backend;

import com.funews.backend.dto.AccountRequest;
import com.funews.backend.entity.Account;
import com.funews.backend.entity.Category;
import com.funews.backend.entity.NewsArticle;
import com.funews.backend.entity.Role;
import com.funews.backend.entity.Tag;
import com.funews.backend.exception.ApiException;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.repository.RoleRepository;
import com.funews.backend.service.AccountService;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/** Verify the account schema without starting unrelated web modules. */
class RoleAccountFieldsPersistenceTest {
    private static StandardServiceRegistry registry;
    private static SessionFactory sessions;

    @BeforeAll
    static void startDatabase() {
        registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.connection.url", "jdbc:h2:mem:role-account-fields;DB_CLOSE_DELAY=-1")
                .applySetting("hibernate.connection.username", "sa")
                .applySetting("hibernate.connection.password", "")
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .build();
        sessions = new MetadataSources(registry)
                .addAnnotatedClass(Role.class)
                .addAnnotatedClass(Account.class)
                .addAnnotatedClass(Category.class)
                .addAnnotatedClass(NewsArticle.class)
                .addAnnotatedClass(Tag.class)
                .buildMetadata().buildSessionFactory();
    }

    @AfterAll
    static void stopDatabase() {
        if (sessions != null) {
            sessions.close();
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    void newFieldsRoundTripAndCreationTimesSurviveUpdates() {
        try (var session = sessions.openSession()) {
            var transaction = session.beginTransaction();
            try {
                Role role = role("FIELD_READER");
                session.persist(role);
                Account account = account(role, "reader.fields");
                account.setDateOfBirth(LocalDate.of(2000, 2, 29));
                account.setStatus(0);
                session.persist(account);
                session.flush();
                Long accountId = account.getId();
                Long roleId = role.getId();
                session.clear();

                Account saved = session.find(Account.class, accountId);
                Role savedRole = session.find(Role.class, roleId);
                assertEquals("reader.fields", saved.getUsername());
                assertEquals(LocalDate.of(2000, 2, 29), saved.getDateOfBirth());
                assertEquals(0, saved.getStatus());
                assertFalse(saved.isActive());
                assertFalse(session.createNativeQuery("select status from accounts where id = :id", Boolean.class)
                        .setParameter("id", accountId).getSingleResult());
                assertNotNull(savedRole.getCreatedAt());
                assertNotNull(saved.getCreatedAt());
                var roleCreatedAt = savedRole.getCreatedAt();
                var accountCreatedAt = saved.getCreatedAt();

                savedRole.setDescription("Updated description");
                saved.setUsername("updated.reader");
                saved.setDateOfBirth(LocalDate.of(2001, 3, 1));
                saved.setStatus(1);
                session.flush();
                session.clear();

                Account updated = session.find(Account.class, accountId);
                assertEquals("updated.reader", updated.getUsername());
                assertEquals(LocalDate.of(2001, 3, 1), updated.getDateOfBirth());
                assertEquals(1, updated.getStatus());
                assertTrue(updated.isActive());
                assertEquals(accountCreatedAt, updated.getCreatedAt());
                assertEquals(roleCreatedAt, session.find(Role.class, roleId).getCreatedAt());
            } finally {
                transaction.rollback();
            }
        }
    }

    @Test
    void unknownBirthDateCanRemainNull() {
        try (var session = sessions.openSession()) {
            var transaction = session.beginTransaction();
            try {
                Role role = role("FIELD_EDITOR");
                session.persist(role);
                Account account = account(role, "no.birth.date");
                session.persist(account);
                session.flush();
                Long accountId = account.getId();
                session.clear();

                Account saved = session.find(Account.class, accountId);
                assertNull(saved.getDateOfBirth());
                assertEquals(1, saved.getStatus());
                assertNotNull(saved.getRole().getCreatedAt());
            } finally {
                transaction.rollback();
            }
        }
    }

    @Test
    void articleOwnershipQueryCountsAuthorsRatherThanReviewers() {
        try (var session = sessions.openSession()) {
            var transaction = session.beginTransaction();
            try {
                Role role = role("ARTICLE_OWNER_READER");
                session.persist(role);
                Account author = account(role, "article.author");
                Account reviewer = account(role, "article.reviewer");
                session.persist(author);
                session.persist(reviewer);
                Category category = new Category();
                category.setName("Ownership test");
                category.setSlug("ownership-test");
                session.persist(category);
                NewsArticle article = new NewsArticle();
                article.setTitle("Ownership test article");
                article.setSlug("ownership-test-article");
                article.setAuthor(author);
                article.setReviewer(reviewer);
                article.setCategory(category);
                session.persist(article);
                session.flush();

                AccountRepository accounts = new JpaRepositoryFactory(session).getRepository(AccountRepository.class);
                assertEquals(1L, accounts.countArticlesByAuthorId(author.getId()));
                assertEquals(0L, accounts.countArticlesByAuthorId(reviewer.getId()));

                article.setDeleted(true);
                session.flush();
                assertEquals(1L, accounts.countArticlesByAuthorId(author.getId()));
            } finally {
                transaction.rollback();
            }
        }
    }

    @Test
    void concurrentAdministratorDemotionsLeaveOneActiveAdministrator() throws Exception {
        var transactions = new TransactionTemplate(new JpaTransactionManager(sessions));
        var entityManager = SharedEntityManagerCreator.createSharedEntityManager(sessions);
        var repositories = new JpaRepositoryFactory(entityManager);
        AccountRepository accounts = repositories.getRepository(AccountRepository.class);
        RoleRepository storedRoles = repositories.getRepository(RoleRepository.class);
        CountDownLatch lockAttempts = new CountDownLatch(2);
        RoleRepository roles = mock(RoleRepository.class, delegatesTo(storedRoles));
        doAnswer(invocation -> {
            lockAttempts.countDown();
            return storedRoles.lockAdministratorRole();
        }).when(roles).lockAdministratorRole();
        AccountService service = new AccountService(accounts, mock(PasswordEncoder.class), roles);

        long[] ids = transactions.execute(status -> {
            Role administrator = role("ADMIN");
            Role reader = role("CONCURRENT_READER");
            entityManager.persist(administrator);
            entityManager.persist(reader);
            Account first = account(administrator, "concurrent.admin.first");
            Account second = account(administrator, "concurrent.admin.second");
            entityManager.persist(first);
            entityManager.persist(second);
            entityManager.flush();
            return new long[] { first.getId(), second.getId(), administrator.getId(), reader.getId() };
        });
        CountDownLatch firstUpdated = new CountDownLatch(1);
        CountDownLatch commitFirst = new CountDownLatch(1);
        var workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> transactions.execute(status -> {
                service.updateAccount(ids[0], new AccountRequest("First admin",
                        "concurrent.admin.first@example.com", null, ids[3], true));
                firstUpdated.countDown();
                await(commitFirst);
                return HttpStatus.OK;
            }));
            assertTrue(firstUpdated.await(10, TimeUnit.SECONDS), "The first update did not finish.");

            var second = workers.submit(() -> {
                try {
                    return transactions.execute(status -> {
                        service.updateAccount(ids[1], new AccountRequest("Second admin",
                                "concurrent.admin.second@example.com", null, ids[3], true));
                        return HttpStatus.OK;
                    });
                } catch (ApiException exception) {
                    return exception.getStatus();
                }
            });
            assertTrue(lockAttempts.await(10, TimeUnit.SECONDS), "Both updates must acquire the admin role lock.");
            // The first transaction has changed its account but has not committed. The
            // second must wait on the real database lock before reading the admin count.
            assertThrows(TimeoutException.class, () -> second.get(300, TimeUnit.MILLISECONDS));
            commitFirst.countDown();

            assertEquals(HttpStatus.OK, first.get(10, TimeUnit.SECONDS));
            assertEquals(HttpStatus.CONFLICT, second.get(10, TimeUnit.SECONDS));
            long remainingAdministrators = transactions.execute(status ->
                    accounts.countByRole_CodeAndActiveTrueAndDeletedFalse("ADMIN"));
            assertEquals(1L, remainingAdministrators);
        } finally {
            commitFirst.countDown();
            workers.shutdown();
            assertTrue(workers.awaitTermination(15, TimeUnit.SECONDS), "Account update threads did not stop.");
            transactions.executeWithoutResult(status -> {
                entityManager.createQuery("delete from Account a where a.id in :ids")
                        .setParameter("ids", List.of(ids[0], ids[1])).executeUpdate();
                entityManager.createQuery("delete from Role r where r.id in :ids")
                        .setParameter("ids", List.of(ids[2], ids[3])).executeUpdate();
            });
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(30, TimeUnit.SECONDS), "Timed out waiting to commit the first update.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static Role role(String code) {
        Role role = new Role();
        role.setCode(code);
        role.setName("Field test role");
        return role;
    }

    private static Account account(Role role, String username) {
        Account account = new Account();
        account.setUsername(username);
        account.setFullName("Field Test User");
        account.setEmail(username + "@example.com");
        account.setPasswordHash("test-only-hash");
        account.setRole(role);
        return account;
    }
}
