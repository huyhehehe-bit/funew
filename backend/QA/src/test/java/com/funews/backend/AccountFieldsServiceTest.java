package com.funews.backend;

import com.funews.backend.dto.AccountRequest;
import com.funews.backend.entity.Account;
import com.funews.backend.entity.Role;
import com.funews.backend.exception.ApiException;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.repository.RoleRepository;
import com.funews.backend.security.AccountPrincipal;
import com.funews.backend.service.AccountService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountFieldsServiceTest {
    private AccountRepository accounts;
    private RoleRepository roles;
    private AccountService service;
    private Role editor;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountRepository.class);
        roles = mock(RoleRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        service = new AccountService(accounts, encoder, roles);
        editor = role(2L, "EDITOR");
        when(roles.findById(2L)).thenReturn(Optional.of(editor));
        when(encoder.encode("password")).thenReturn("encoded-password");
        when(accounts.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            if (account.getId() == null) {
                account.setId(10L);
            }
            return account;
        });
    }

    @Test
    void createPersistsUsernameBirthdayAndInactiveStatus() {
        LocalDate birthday = LocalDate.of(2001, 2, 3);

        var response = service.createAccount(new AccountRequest("New User", "new@fu.edu.vn", "password",
                2L, null, "  new.user  ", birthday, 0));

        assertThat(response.username()).isEqualTo("new.user");
        assertThat(response.dateOfBirth()).isEqualTo(birthday);
        assertThat(response.status()).isZero();
        assertThat(response.isActive()).isFalse();
        assertThat(response.roleCode()).isEqualTo("EDITOR");
        verify(accounts).save(argThat(account -> "new.user".equals(account.getUsername())
                && birthday.equals(account.getDateOfBirth()) && !account.isActive()
                && "encoded-password".equals(account.getPasswordHash())));
    }

    @Test
    void createDefaultsUsernameToNormalizedEmailAndStatusToActive() {
        var response = service.createAccount(new AccountRequest("New User", "  NEW@fu.edu.vn  ",
                "password", 2L, null));

        assertThat(response.username()).isEqualTo("new@fu.edu.vn");
        assertThat(response.email()).isEqualTo("new@fu.edu.vn");
        assertThat(response.dateOfBirth()).isNull();
        assertThat(response.status()).isEqualTo(1);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void updatePreservesOmittedUsernameBirthdayStatusAndPassword() {
        Account existing = account(editor);
        existing.setUsername("retained-name");
        existing.setDateOfBirth(LocalDate.of(1998, 7, 6));
        existing.setStatus(0);
        when(accounts.findById(10L)).thenReturn(Optional.of(existing));

        var response = service.updateAccount(10L,
                new AccountRequest("Updated Name", "changed@fu.edu.vn", null, 2L, null));

        assertThat(response.fullName()).isEqualTo("Updated Name");
        assertThat(response.email()).isEqualTo("changed@fu.edu.vn");
        assertThat(response.username()).isEqualTo("retained-name");
        assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1998, 7, 6));
        assertThat(response.status()).isZero();
        assertThat(existing.getPasswordHash()).isEqualTo("existing-hash");
    }

    @Test
    void createRejectsUsernameFoundByCaseInsensitiveLookup() {
        Account existing = account(editor);
        existing.setUsername("taken");
        when(accounts.findByUsernameIgnoreCaseAndDeletedFalse(argThat(
                username -> "taken".equalsIgnoreCase(username)))).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createAccount(new AccountRequest("New User", "new@fu.edu.vn",
                "password", 2L, null, " TAKEN ", null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(accounts).findByUsernameIgnoreCaseAndDeletedFalse("TAKEN");
        verify(accounts, never()).save(any(Account.class));
    }

    @Test
    void createRejectsConflictingOrInvalidStatusBeforeSaving() {
        assertThatThrownBy(() -> service.createAccount(new AccountRequest("New User", "new@fu.edu.vn",
                "password", 2L, true, "new.user", null, 0)))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.createAccount(new AccountRequest("New User", "new@fu.edu.vn",
                "password", 2L, null, "new.user", null, 2)))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(accounts, never()).save(any(Account.class));
    }

    @Test
    void updateCannotDisableLastActiveAdministrator() {
        Role admin = role(1L, "ADMIN");
        when(roles.findById(1L)).thenReturn(Optional.of(admin));
        when(accounts.findById(10L)).thenReturn(Optional.of(account(admin)));
        when(accounts.countByRole_CodeAndActiveTrueAndDeletedFalse("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> service.updateAccount(10L, new AccountRequest("Admin", "admin@fu.edu.vn",
                null, 1L, null, null, null, 0)))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(accounts, never()).save(any(Account.class));
    }

    @Test
    void deleteRetainsAccountReferencedByAuthoredArticles() {
        Account existing = account(editor);
        when(accounts.findById(10L)).thenReturn(Optional.of(existing));
        when(accounts.countArticlesByAuthorId(10L)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteAccount(10L, "admin@fu.edu.vn"))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        assertThat(existing.isDeleted()).isFalse();
        var order = inOrder(roles, accounts);
        order.verify(roles).lockAdministratorRole();
        order.verify(accounts).findById(10L);
        order.verify(accounts).countArticlesByAuthorId(10L);
        verify(accounts, never()).save(any(Account.class));
    }

    @Test
    void deleteWithoutArticlesStillRecordsSoftDeletionAndActor() {
        Account existing = account(editor);
        Account actor = account(role(1L, "ADMIN"));
        actor.setId(1L);
        actor.setEmail("admin@fu.edu.vn");
        when(accounts.findById(10L)).thenReturn(Optional.of(existing));
        when(accounts.findByEmailIgnoreCase(actor.getEmail())).thenReturn(Optional.of(actor));

        service.deleteAccount(10L, actor.getEmail());

        assertThat(existing.isDeleted()).isTrue();
        assertThat(existing.isActive()).isFalse();
        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getDeletedBy()).isSameAs(actor);
        verify(accounts).save(existing);
    }

    @Test
    void inactiveAndDeletedPrincipalsCannotAuthenticate() {
        Account existing = account(editor);
        assertThat(AccountPrincipal.from(existing).isEnabled()).isTrue();

        existing.setStatus(0);
        assertThat(AccountPrincipal.from(existing).isEnabled()).isFalse();

        existing.setStatus(1);
        existing.setDeleted(true);
        assertThat(AccountPrincipal.from(existing).isEnabled()).isFalse();
    }

    private Role role(Long id, String code) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        role.setName(code);
        return role;
    }

    private Account account(Role role) {
        Account account = new Account();
        account.setId(10L);
        account.setFullName("Existing User");
        account.setEmail("existing@fu.edu.vn");
        account.setUsername("existing.user");
        account.setPasswordHash("existing-hash");
        account.setRole(role);
        return account;
    }
}
