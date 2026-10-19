package com.funews.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManagementIntegrationTest extends BackendIntegrationSupport {
    @Test
    void adminCanCreateUpdateAndDeleteAccountsWithoutExposingPasswords() throws Exception {
        MockHttpSession admin = login("a@fu.edu.vn", "123");
        MvcResult created = mvc.perform(post("/api/accounts").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Fixture User", "fixture-user@fu.edu.vn", "initial-password", 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        long id = body(created).path("id").asLong();

        mvc.perform(put("/api/accounts/{id}", id).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Updated User", "updated-user@fu.edu.vn", "changed-password", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated User"))
                .andExpect(jsonPath("$.email").value("updated-user@fu.edu.vn"))
                .andExpect(jsonPath("$.password").doesNotExist());
        assertThat(currentUserId(login("updated-user@fu.edu.vn", "changed-password"))).isEqualTo(id);
        JsonNode accounts = body(mvc.perform(get("/api/accounts").session(admin))
                .andExpect(status().isOk()).andReturn());
        assertThat(findById(accounts, id).path("name").asText()).isEqualTo("Updated User");
        accounts.forEach(value -> {
            assertThat(value.has("password")).isFalse();
            assertThat(value.has("passwordHash")).isFalse();
        });

        mvc.perform(delete("/api/accounts/{id}", id).session(admin))
                .andExpect(status().isNoContent());
        assertThat(findById(body(mvc.perform(get("/api/accounts").session(admin))
                .andExpect(status().isOk()).andReturn()), id)).isNull();
    }

    @Test
    void duplicateEmailsAreRejectedCaseInsensitively() throws Exception {
        MockHttpSession admin = login("a@fu.edu.vn", "123");
        mvc.perform(post("/api/accounts").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Duplicate", "A@FU.EDU.VN", "123", 2))))
                .andExpect(status().isConflict());
    }

    @Test
    void categoryCrudPersistsAndParentCannotBeDeletedWhileItHasChildren() throws Exception {
        MockHttpSession admin = login("a@fu.edu.vn", "123");
        long parentId = createCategory(admin, "Fixture parent", 0);
        long childId = createCategory(admin, "Fixture child", parentId);

        mvc.perform(put("/api/categories/{id}", childId).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Updated child", "description", "Updated description",
                                "parentId", parentId, "isActive", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated child"))
                .andExpect(jsonPath("$.isActive").value(0));
        JsonNode persisted = findById(body(mvc.perform(get("/api/categories").session(admin))
                .andExpect(status().isOk()).andReturn()), childId);
        assertThat(persisted.path("parentId").asLong()).isEqualTo(parentId);
        assertThat(persisted.path("description").asText()).isEqualTo("Updated description");

        mvc.perform(delete("/api/categories/{id}", parentId).session(admin))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/categories/{id}", childId).session(admin))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/categories/{id}", parentId).session(admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void profileChangesKeepSessionWorkingAndIgnorePrivilegeEscalation() throws Exception {
        MockHttpSession staff = login("b@fu.edu.vn", "123");
        long id = currentUserId(staff);
        mvc.perform(put("/api/profile").session(staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("id", 999999, "name", "Updated profile", "email", "profile-change@fu.edu.vn",
                                "role", 1, "password", "unrequested-password-change"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) id))
                .andExpect(jsonPath("$.role").value(2))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/auth/me").session(staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile-change@fu.edu.vn"))
                .andExpect(jsonPath("$.name").value("Updated profile"))
                .andExpect(jsonPath("$.role").value(2));
        mvc.perform(get("/api/accounts").session(staff)).andExpect(status().isForbidden());
        assertThat(currentUserId(login("profile-change@fu.edu.vn", "123"))).isEqualTo(id);
    }

    @Test
    void accountDemotionIsEffectiveForExistingSessions() throws Exception {
        MockHttpSession seedAdmin = login("a@fu.edu.vn", "123");
        MvcResult created = mvc.perform(post("/api/accounts").session(seedAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Temporary admin", "temporary-admin@fu.edu.vn", "123", 1))))
                .andExpect(status().isCreated()).andReturn();
        long id = body(created).path("id").asLong();
        MockHttpSession temporaryAdmin = login("temporary-admin@fu.edu.vn", "123");
        mvc.perform(get("/api/accounts").session(temporaryAdmin)).andExpect(status().isOk());

        mvc.perform(put("/api/accounts/{id}", id).session(seedAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Temporary admin", "temporary-admin@fu.edu.vn", "", 2))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/accounts").session(temporaryAdmin)).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").session(temporaryAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(2));
    }

    @Test
    void invalidRequiredFieldsAreRejectedWithoutSaving() throws Exception {
        MockHttpSession admin = login("a@fu.edu.vn", "123");
        mvc.perform(post("/api/categories").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(category("   ", 0))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/accounts").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Invalid email", "not-an-email", "123", 2))))
                .andExpect(status().isBadRequest());
    }

}
