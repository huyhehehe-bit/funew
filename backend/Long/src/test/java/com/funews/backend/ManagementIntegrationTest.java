package com.funews.backend;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Long - quan ly tai khoan va danh muc.
 *
 * Xac thuc bang JWT Bearer, khong con phien. roleId theo bang roles:
 * 1 ADMIN, 2 EDITOR, 3 CONTRIBUTOR, 4 READER - nen a@ gio la EDITOR chu khong
 * phai admin nua, moi thao tac quan tri phai dung admin@.
 */
class ManagementIntegrationTest extends BackendIntegrationSupport {
    private static final int ADMIN = 1;
    private static final int CONTRIBUTOR = 3;

    @Test
    void adminCanCreateUpdateAndDeleteAccountsWithoutExposingPasswords() throws Exception {
        String admin = token("admin@fu.edu.vn", "Admin");
        MvcResult created = mvc.perform(post("/api/accounts").with(bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Fixture User", "fixture-user@fu.edu.vn", "initial-password", CONTRIBUTOR))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        long id = body(created).path("id").asLong();

        mvc.perform(put("/api/accounts/{id}", id).with(bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Updated User", "updated-user@fu.edu.vn", "changed-password", CONTRIBUTOR))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated User"))
                .andExpect(jsonPath("$.email").value("updated-user@fu.edu.vn"))
                .andExpect(jsonPath("$.roleCode").value("CONTRIBUTOR"))
                .andExpect(jsonPath("$.password").doesNotExist());
        // Doi mat khau xong thi dang nhap bang mat khau moi phai ra dung tai khoan do.
        assertThat(currentUserId(token("updated-user@fu.edu.vn", "changed-password"))).isEqualTo(id);

        JsonNode accounts = body(mvc.perform(get("/api/accounts").with(bearer(admin)))
                .andExpect(status().isOk()).andReturn());
        assertThat(findById(accounts, id).path("fullName").asText()).isEqualTo("Updated User");
        accounts.forEach(value -> {
            assertThat(value.has("password")).isFalse();
            assertThat(value.has("passwordHash")).isFalse();
        });

        mvc.perform(delete("/api/accounts/{id}", id).with(bearer(admin)))
                .andExpect(status().isNoContent());
        assertThat(findById(body(mvc.perform(get("/api/accounts").with(bearer(admin)))
                .andExpect(status().isOk()).andReturn()), id)).isNull();
    }

    @Test
    void duplicateEmailsAreRejectedCaseInsensitively() throws Exception {
        String admin = token("admin@fu.edu.vn", "Admin");
        mvc.perform(post("/api/accounts").with(bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Duplicate", "A@FU.EDU.VN", "123", CONTRIBUTOR))))
                .andExpect(status().isConflict());
    }

    @Test
    void categoryCrudPersistsAndParentCannotBeDeletedWhileItHasChildren() throws Exception {
        String admin = token("admin@fu.edu.vn", "Admin");
        long parentId = createCategory(admin, "Fixture parent", 0);
        long childId = createCategory(admin, "Fixture child", parentId);

        // isActive da la boolean chu khong con 0/1.
        mvc.perform(put("/api/categories/{id}", childId).with(bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Updated child", "description", "Updated description",
                                "parentId", parentId, "isActive", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated child"))
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.slug").value("updated-child"));
        JsonNode persisted = findById(body(mvc.perform(get("/api/categories").with(bearer(admin)))
                .andExpect(status().isOk()).andReturn()), childId);
        assertThat(persisted.path("parentId").asLong()).isEqualTo(parentId);
        assertThat(persisted.path("description").asText()).isEqualTo("Updated description");

        mvc.perform(delete("/api/categories/{id}", parentId).with(bearer(admin)))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/categories/{id}", childId).with(bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/categories/{id}", parentId).with(bearer(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void profileChangesKeepTokenWorkingAndIgnorePrivilegeEscalation() throws Exception {
        String contributor = token("b@fu.edu.vn", "123");
        long id = currentUserId(contributor);
        // Gui kem id, roleId va password gia mao - ProfileRequest khong co nhung
        // truong do nen chung bi bo qua, khong phai bi tu choi.
        mvc.perform(put("/api/profile").with(bearer(contributor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("id", 999999, "fullName", "Updated profile",
                                "email", "profile-change@fu.edu.vn", "roleId", ADMIN,
                                "password", "unrequested-password-change"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) id))
                .andExpect(jsonPath("$.roleCode").value("CONTRIBUTOR"))
                .andExpect(jsonPath("$.password").doesNotExist());

        // Token cu van dung duoc sau khi doi email, vi mat khau khong doi.
        mvc.perform(get("/api/auth/me").with(bearer(contributor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile-change@fu.edu.vn"))
                .andExpect(jsonPath("$.fullName").value("Updated profile"))
                .andExpect(jsonPath("$.roleCode").value("CONTRIBUTOR"));
        mvc.perform(get("/api/accounts").with(bearer(contributor))).andExpect(status().isForbidden());
        assertThat(currentUserId(token("profile-change@fu.edu.vn", "123"))).isEqualTo(id);
    }

    @Test
    void accountDemotionIsEffectiveForTokensAlreadyIssued() throws Exception {
        String seedAdmin = token("admin@fu.edu.vn", "Admin");
        MvcResult created = mvc.perform(post("/api/accounts").with(bearer(seedAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Temporary admin", "temporary-admin@fu.edu.vn", "123", ADMIN))))
                .andExpect(status().isCreated()).andReturn();
        long id = body(created).path("id").asLong();
        String temporaryAdmin = token("temporary-admin@fu.edu.vn", "123");
        mvc.perform(get("/api/accounts").with(bearer(temporaryAdmin))).andExpect(status().isOk());

        mvc.perform(put("/api/accounts/{id}", id).with(bearer(seedAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Temporary admin", "temporary-admin@fu.edu.vn", "", CONTRIBUTOR))))
                .andExpect(status().isOk());

        // Day la diem quan trong cua thiet ke: token KHONG mang theo vai tro.
        // JwtAuthenticationFilter doc lai tai khoan tu database moi request, nen
        // ha quyen co hieu luc ngay voi token da phat, khong phai doi het han.
        mvc.perform(get("/api/accounts").with(bearer(temporaryAdmin))).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").with(bearer(temporaryAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleCode").value("CONTRIBUTOR"));
    }

    @Test
    void invalidRequiredFieldsAreRejectedWithoutSaving() throws Exception {
        String admin = token("admin@fu.edu.vn", "Admin");
        mvc.perform(post("/api/categories").with(bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(category("   ", 0))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/accounts").with(bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Invalid email", "not-an-email", "123", CONTRIBUTOR))))
                .andExpect(status().isBadRequest());
    }
}
