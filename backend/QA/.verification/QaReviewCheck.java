package com.funews.backend;

import com.funews.backend.entity.NewsArticle;
import java.util.Map;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public final class QaReviewCheck extends JwtIntegrationSupport {
    public static void main(String[] args) throws Exception {
        QaReviewCheck check = new QaReviewCheck();
        check.startQaWebContext();
        try {
            for (String login : new String[]{check.account.getUsername(), check.account.getEmail()}) {
                int status = check.mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(check.mapper.writeValueAsString(Map.of("username", login, "password", "123"))))
                    .andReturn().getResponse().getStatus();
                System.out.println("LOGIN " + (login.contains("@") ? "email" : "custom username") + " HTTP " + status);
            }
            try {
                PropertyPath.from("createdById", NewsArticle.class);
                System.out.println("NEWS PATH createdById: valid");
            } catch (org.springframework.data.mapping.PropertyReferenceException exception) {
                System.out.println("NEWS PATH createdById: " + exception.getMessage());
            }
        } finally {
            check.stopQaWebContext();
        }
    }
}