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

// Test chay tren H2 in-memory, khong can SQL Server bat san.
// Hibernate tao bang tu entity (create-drop), sau do data-h2.sql do du lieu mau vao.
// defer-datasource-initialization bat buoc phai bat, neu khong script chay truoc khi co bang.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:funews-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.defer-datasource-initialization=true",
        "spring.sql.init.platform=h2"
})
@AutoConfigureMockMvc
@Transactional
abstract class BackendIntegrationSupport {
    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper mapper;

    protected MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return session(result);
    }

    protected long currentUserId(MockHttpSession session) throws Exception {
        return body(mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk()).andReturn()).path("id").asLong();
    }

    protected long createCategory(MockHttpSession admin, String name, long parentId) throws Exception {
        return body(mvc.perform(post("/api/categories").session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(category(name, parentId))))
                .andExpect(status().isCreated()).andReturn()).path("id").asLong();
    }

    protected Map<String, Object> account(String name, String email, String password, int role) {
        return Map.of("name", name, "email", email, "password", password, "role", role);
    }

    protected Map<String, Object> category(String name, long parentId) {
        return Map.of("name", name, "description", "Integration test category", "parentId", parentId, "isActive", 1);
    }

    protected Map<String, Object> news(String title, long categoryId, long forgedAuthorId, int status) {
        return Map.of("title", title, "headline", "Fixture headline", "content", "Fixture article content",
                "source", "Integration test", "categoryId", categoryId, "tags", "fixture, integration",
                "status", status, "createdById", forgedAuthorId, "createdDate", "2000-01-01");
    }

    protected JsonNode findById(JsonNode values, long id) {
        for (JsonNode value : values) {
            if (value.path("id").asLong() == id) {
                return value;
            }
        }
        return null;
    }

    protected MockHttpSession session(MvcResult result) {
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    protected JsonNode body(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    protected String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }
}
