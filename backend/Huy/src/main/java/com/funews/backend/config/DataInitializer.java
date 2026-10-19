package com.funews.backend.config;

import com.funews.backend.entity.Account;
import com.funews.backend.entity.Category;
import com.funews.backend.entity.NewsArticle;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.repository.CategoryRepository;
import com.funews.backend.repository.NewsArticleRepository;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Huy - tao du lieu mau cho database trong. Tat bang app.seed-data=false. */
@Component
public class DataInitializer implements CommandLineRunner {
    private final AccountRepository accounts;
    private final CategoryRepository categories;
    private final NewsArticleRepository news;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedData;

    public DataInitializer(AccountRepository accounts, CategoryRepository categories,
                           NewsArticleRepository news, PasswordEncoder passwordEncoder,
                           @Value("${app.seed-data:true}") boolean seedData) {
        this.accounts = accounts;
        this.categories = categories;
        this.news = news;
        this.passwordEncoder = passwordEncoder;
        this.seedData = seedData;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Chi tao khi database con trong; khoi dong lai phai giu nguyen thay doi cua nguoi dung.
        if (!seedData || accounts.count() != 0 || categories.count() != 0 || news.count() != 0) {
            return;
        }

        createAccount("Nguyen Van A", "a@fu.edu.vn", "123", 1);
        Account staffB = createAccount("Tran Thi B", "b@fu.edu.vn", "123", 2);
        Account staffC = createAccount("Le Van C", "c@fu.edu.vn", "123", 2);
        createAccount("Admin", "admin@fu.edu.vn", "Admin", 1);

        Category admissions = createCategory("Tuyen sinh", "Tin tuc tuyen sinh", null, 1);
        Category events = createCategory("Su kien", "Su kien cua truong", null, 1);
        createCategory("Hoc bong", "Thong tin hoc bong", admissions, 0);

        createNews("Thong bao tuyen sinh 2026", "Truong mo dot tuyen sinh moi",
                "Noi dung chi tiet ve dot tuyen sinh nam 2026.", "Phong tuyen sinh",
                admissions, "tuyensinh, 2026", 1, staffB, LocalDate.of(2026, 9, 1));
        createNews("Ngay hoi viec lam", "Hon 50 doanh nghiep tham gia",
                "Ngay hoi viec lam duoc to chuc tai san truong.", "Phong CTSV",
                events, "sukien, vieclam", 0, staffC, LocalDate.of(2026, 9, 15));
    }

    private Account createAccount(String name, String email, String password, int role) {
        Account account = new Account();
        account.setName(name);
        account.setEmail(email);
        // Luu hash BCrypt, khong bao gio luu mat khau dang thuong.
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole(role);
        return accounts.save(account);
    }

    private Category createCategory(String name, String description, Category parent, int active) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setParent(parent);
        category.setIsActive(active);
        return categories.save(category);
    }

    private void createNews(String title, String headline, String content, String source, Category category,
                            String tags, int status, Account author, LocalDate date) {
        NewsArticle article = new NewsArticle();
        article.setTitle(title);
        article.setHeadline(headline);
        article.setContent(content);
        article.setSource(source);
        article.setCategory(category);
        article.setTags(tags);
        article.setStatus(status);
        article.setCreatedBy(author);
        article.setCreatedDate(date);
        news.save(article);
    }
}
