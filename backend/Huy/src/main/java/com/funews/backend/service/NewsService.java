package com.funews.backend.service;

import com.funews.backend.dto.ArticleAuditLogResponse;
import com.funews.backend.dto.DashboardResponse;
import com.funews.backend.dto.NewsRequest;
import com.funews.backend.dto.NewsResponse;
import com.funews.backend.dto.NotificationResponse;
import com.funews.backend.dto.ReviewRequest;
import com.funews.backend.entity.Account;
import com.funews.backend.entity.ArticleAuditLog;
import com.funews.backend.entity.ArticleStatus;
import com.funews.backend.entity.Category;
import com.funews.backend.entity.NewsArticle;
import com.funews.backend.entity.Notification;
import com.funews.backend.entity.NotificationType;
import com.funews.backend.entity.Tag;
import com.funews.backend.exception.ApiException;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.repository.ArticleAuditLogRepository;
import com.funews.backend.repository.CategoryRepository;
import com.funews.backend.repository.NewsArticleRepository;
import com.funews.backend.repository.NotificationRepository;
import com.funews.backend.repository.TagRepository;
import com.funews.backend.security.AccountPrincipal;
import com.funews.backend.util.Slugs;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Luong bien tap cua Module 1.
 *
 * MOI LAN DOI TRANG THAI DEU PHAI QUA MOT HAM RIENG o day, va ham do tu ghi nhat
 * ky (FR-ED-004) cung thong bao cho tac gia (AC-ED-002). Khong co duong nao doi
 * trang thai ma khong de lai dau vet: updateNews tu choi moi request mang trang
 * thai khac trang thai hien tai.
 *
 * Phan quyen o day la lop thu hai. SecurityConfig da chan theo URL, nhung URL
 * khong biet ai la tac gia cua bai nao - viec do chi tang nay kiem tra duoc.
 *
 * Bai da xoa mem khong bao gio tra ra: moi truy van di qua NewsArticleRepository
 * deu loc deleted = false.
 */
@Service
@Transactional(readOnly = true)
public class NewsService {
    private static final Logger log = LoggerFactory.getLogger(NewsService.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** Tac vu hen gio dang bai dung ten tai khoan nay - PHAN 3.3 cua module1_schema.sql. */
    private static final String SYSTEM_EMAIL = "system@fu.edu.vn";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EDITOR = "EDITOR";

    private final AccountRepository accounts;
    private final CategoryRepository categories;
    private final NewsArticleRepository news;
    private final TagRepository tags;
    private final ArticleAuditLogRepository auditLogs;
    private final NotificationRepository notifications;
    private final EditorialRulesService rules;

    public NewsService(AccountRepository accounts, CategoryRepository categories, NewsArticleRepository news,
            TagRepository tags, ArticleAuditLogRepository auditLogs, NotificationRepository notifications,
            EditorialRulesService rules) {
        this.accounts = accounts;
        this.categories = categories;
        this.news = news;
        this.tags = tags;
        this.auditLogs = auditLogs;
        this.notifications = notifications;
        this.rules = rules;
    }

    /* =================================================================
       DOC - ai thay bai nao
       ================================================================= */

    public List<NewsResponse> listNews(AccountPrincipal viewer) {
        return visibleArticles(viewer).stream().map(this::newsResponse).toList();
    }

    public NewsResponse getNews(Long id, AccountPrincipal viewer) {
        NewsArticle article = requireNews(id);
        ensureCanRead(article, viewer);
        return newsResponse(article);
    }

    /** Hang doi duyet bai. SecurityConfig da gioi han cho EDITOR va ADMIN. */
    public List<NewsResponse> reviewQueue() {
        return news.findByStatusForQueue(ArticleStatus.PENDING_REVIEW).stream().map(this::newsResponse).toList();
    }

    public List<ArticleAuditLogResponse> auditTrail(Long id, AccountPrincipal viewer) {
        NewsArticle article = requireNews(id);
        ensureCanRead(article, viewer);
        return auditLogs.findByArticleIdOrderByCreatedAtDescIdDesc(article.getId()).stream()
                .map(row -> auditResponse(article, row)).toList();
    }

    public DashboardResponse dashboard(AccountPrincipal viewer) {
        List<NewsResponse> recent = visibleArticles(viewer).stream()
                .sorted(Comparator.comparing(NewsArticle::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(NewsArticle::getId, Comparator.reverseOrder()))
                .limit(5).map(this::newsResponse).toList();
        // Ca hai so deu phai bo qua dong da xoa mem - count() tran thi dem ca chung.
        return new DashboardResponse(accounts.findByDeletedFalseOrderByIdAsc().size(),
                categories.countByDeletedFalse(),
                news.countByDeletedFalse(), news.countByStatusAndDeletedFalse(ArticleStatus.PUBLISHED),
                news.countByStatusAndDeletedFalse(ArticleStatus.PENDING_REVIEW), recent);
    }

    /* =================================================================
       CRUD
       ================================================================= */

    @Transactional
    public NewsResponse createNews(NewsRequest request, AccountPrincipal author) {
        Account me = requireAccount(author);
        ArticleStatus status = statusForCreate(request.status());
        NewsArticle article = new NewsArticle();
        // Tac gia do server gan, khong nhan tu client.
        article.setAuthor(me);
        article.setStatus(status);
        apply(article, request, author);
        if (status == ArticleStatus.PENDING_REVIEW) {
            ensureSummaryLongEnough(article);
            article.setSubmittedAt(LocalDateTime.now(ZONE));
        }
        NewsArticle saved = news.saveAndFlush(article);
        if (status == ArticleStatus.PENDING_REVIEW) {
            writeLog(saved, ArticleStatus.DRAFT, ArticleStatus.PENDING_REVIEW, "SUBMIT", null, me);
        }
        return newsResponse(saved);
    }

    @Transactional
    public NewsResponse updateNews(Long id, NewsRequest request, AccountPrincipal actor) {
        NewsArticle article = requireNews(id);
        ensureCanEdit(article, actor);
        ensureVersionMatches(article, request.version());
        if (request.status() != article.getStatus()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Sua bai khong doi duoc trang thai."
                    + " Dung submit / request-changes / approve / publish / archive.");
        }
        apply(article, request, actor);
        return newsResponse(news.saveAndFlush(article));
    }

    /** XOA MEM. Khong co lenh DELETE nao trong ca service nay. */
    @Transactional
    public void deleteNews(Long id, AccountPrincipal actor) {
        NewsArticle article = requireNews(id);
        boolean owner = article.getAuthor().getId().equals(actor.id());
        if (!owner && !isAdmin(actor)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Chi tac gia hoac quan tri vien xoa duoc bai nay.");
        }
        if (article.getStatus() == ArticleStatus.PUBLISHED && !isAdmin(actor)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Bai dang cong khai: go bang archive truoc, hoac de quan tri vien xoa.");
        }
        article.setDeleted(true);
        article.setDeletedAt(LocalDateTime.now(ZONE));
        article.setDeletedBy(requireAccount(actor));
        news.saveAndFlush(article);
    }

    /* =================================================================
       LUONG DUYET BAI - FR-ED-001
       DRAFT / CHANGES_REQUESTED -> PENDING_REVIEW -> SCHEDULED -> PUBLISHED -> ARCHIVED
       ================================================================= */

    @Transactional
    public NewsResponse submit(Long id, AccountPrincipal actor) {
        NewsArticle article = requireNews(id);
        if (!article.getAuthor().getId().equals(actor.id()) && !isAdmin(actor)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Chi tac gia nop bai cua chinh minh.");
        }
        ArticleStatus from = article.getStatus();
        if (from != ArticleStatus.DRAFT && from != ArticleStatus.CHANGES_REQUESTED) {
            throw badTransition(from, ArticleStatus.PENDING_REVIEW);
        }
        ensureSummaryLongEnough(article);
        Account me = requireAccount(actor);
        article.setStatus(ArticleStatus.PENDING_REVIEW);
        article.setSubmittedAt(LocalDateTime.now(ZONE));
        writeLog(news.saveAndFlush(article), from, ArticleStatus.PENDING_REVIEW, "SUBMIT", null, me);
        return newsResponse(article);
    }

    /** FR-ED-002: tra bai lai BAT BUOC co ly do, va ly do do den tay tac gia. */
    @Transactional
    public NewsResponse requestChanges(Long id, ReviewRequest request, AccountPrincipal actor) {
        NewsArticle article = requirePendingReview(id, actor);
        String note = request == null ? null : trimToNull(request.note());
        if (note == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tra bai lai bat buoc co ly do - FR-ED-002.");
        }
        Account me = requireAccount(actor);
        markReviewed(article, me, ArticleStatus.CHANGES_REQUESTED);
        writeLog(news.saveAndFlush(article), ArticleStatus.PENDING_REVIEW, ArticleStatus.CHANGES_REQUESTED,
                "REQUEST_CHANGES", note, me);
        notifyAuthor(article, NotificationType.CHANGES_REQUESTED,
                "Bai " + article.getTitle() + " bi tra lai: " + note);
        return newsResponse(article);
    }

    /**
     * Duyet bai. Khong co gio hen, hoac gio hen da qua, thi dang ngay trong cung
     * mot transaction - nhat ky van ghi du hai buoc APPROVE va PUBLISH.
     */
    @Transactional
    public NewsResponse approve(Long id, ReviewRequest request, AccountPrincipal actor) {
        NewsArticle article = requirePendingReview(id, actor);
        String approveNote = request == null ? null : trimToNull(request.note());
        if (rules.current().isRequireNoteOnApprove() && approveNote == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Quy tac bien tap dang bat: duyet bai phai ghi chu.");
        }
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime at = request == null || request.scheduledPublishAt() == null
                ? now : request.scheduledPublishAt();
        Account me = requireAccount(actor);
        markReviewed(article, me, ArticleStatus.SCHEDULED);
        article.setScheduledPublishAt(at);
        boolean later = at.isAfter(now);
        writeLog(news.saveAndFlush(article), ArticleStatus.PENDING_REVIEW, ArticleStatus.SCHEDULED,
                later ? "SCHEDULE" : "APPROVE", approveNote, me);
        if (later) {
            notifyAuthor(article, NotificationType.SCHEDULED,
                    "Bai " + article.getTitle() + " da duoc duyet, se tu dang luc " + at + ".");
        } else {
            publishNow(article, me, now);
        }
        return newsResponse(article);
    }

    /** Dang som mot bai dang cho toi gio hen. */
    @Transactional
    public NewsResponse publish(Long id, AccountPrincipal actor) {
        ensureStaff(actor);
        NewsArticle article = requireNews(id);
        if (article.getStatus() != ArticleStatus.SCHEDULED) {
            throw badTransition(article.getStatus(), ArticleStatus.PUBLISHED);
        }
        publishNow(article, requireAccount(actor), LocalDateTime.now(ZONE));
        return newsResponse(article);
    }

    @Transactional
    public NewsResponse archive(Long id, AccountPrincipal actor) {
        ensureStaff(actor);
        NewsArticle article = requireNews(id);
        if (article.getStatus() != ArticleStatus.PUBLISHED) {
            throw badTransition(article.getStatus(), ArticleStatus.ARCHIVED);
        }
        Account me = requireAccount(actor);
        article.setStatus(ArticleStatus.ARCHIVED);
        writeLog(news.saveAndFlush(article), ArticleStatus.PUBLISHED, ArticleStatus.ARCHIVED, "ARCHIVE", null, me);
        notifyAuthor(article, NotificationType.ARCHIVED,
                "Bai " + article.getTitle() + " da duoc go khoi trang cong khai.");
        return newsResponse(article);
    }

    /**
     * AC-ED-003: toi gio hen thi bai TU len trang, khong ai bam.
     * Goi tu ScheduledPublisher. Tra ve so bai da dang de test kiem duoc.
     */
    @Transactional
    public int publishDueArticles() {
        if (!rules.current().isAutoPublishScheduled()) {
            // Admin da tat tu dong dang: bai hen gio nam yen cho bien tap bam Dang.
            return 0;
        }
        LocalDateTime now = LocalDateTime.now(ZONE);
        List<NewsArticle> due = news.findDueForPublish(ArticleStatus.SCHEDULED, now);
        if (due.isEmpty()) {
            return 0;
        }
        Account system = accounts.findByEmailIgnoreCase(SYSTEM_EMAIL).orElse(null);
        if (system == null) {
            // actor_id la NOT NULL nen khong co tai khoan nay thi khong ghi duoc nhat ky,
            // ma dang bai ma khong ghi nhat ky thi vi pham AC-ED-004 - tha khong dang.
            log.warn("Thieu tai khoan {}: {} bai den gio hen chua dang duoc."
                    + " Chay lai database/module1_schema.sql de tao tai khoan do.", SYSTEM_EMAIL, due.size());
            return 0;
        }
        due.forEach(article -> publishNow(article, system, now));
        return due.size();
    }

    /* =================================================================
       HOP THU - AC-ED-002
       ================================================================= */

    public List<NotificationResponse> inbox(AccountPrincipal viewer) {
        return notifications.findByAccountIdAndDeletedFalseOrderByCreatedAtDescIdDesc(requireId(viewer)).stream()
                .map(this::notificationResponse).toList();
    }

    public long unreadCount(AccountPrincipal viewer) {
        return notifications.countByAccountIdAndReadFalseAndDeletedFalse(requireId(viewer));
    }

    @Transactional
    public NotificationResponse markNotificationRead(Long id, AccountPrincipal viewer) {
        Notification notification = notifications.findByIdAndAccountIdAndDeletedFalse(id, requireId(viewer))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Khong tim thay thong bao."));
        if (!notification.isRead()) {
            notification.markRead(LocalDateTime.now(ZONE));
            notifications.save(notification);
        }
        return notificationResponse(notification);
    }

    /* =================================================================
       Phan quyen va kiem tra
       ================================================================= */

    private List<NewsArticle> visibleArticles(AccountPrincipal viewer) {
        if (isStaff(viewer)) {
            return news.findAllVisible();
        }
        if (viewer == null) {
            return news.findPublished(ArticleStatus.PUBLISHED);
        }
        // CONTRIBUTOR thay bai cua chinh minh o moi trang thai. READER khong co bai
        // nao nen cung truy van nay tra ve dung cac bai da xuat ban.
        return news.findVisibleForAuthor(ArticleStatus.PUBLISHED, viewer.id());
    }

    /** Tra 404 chu khong 403: khong tiet lo rang bai do co ton tai. */
    private void ensureCanRead(NewsArticle article, AccountPrincipal viewer) {
        if (article.getStatus() == ArticleStatus.PUBLISHED || isStaff(viewer)) {
            return;
        }
        if (viewer != null && article.getAuthor().getId().equals(viewer.id())) {
            return;
        }
        throw new ApiException(HttpStatus.NOT_FOUND, "Khong tim thay bai viet.");
    }

    private void ensureCanEdit(NewsArticle article, AccountPrincipal actor) {
        boolean owner = article.getAuthor().getId().equals(actor.id());
        if (!owner && !isStaff(actor)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Khong tim thay bai viet.");
        }
        // AC-ED-001: nop roi la khoa sua. ADMIN co quyen ghi de moi luong.
        if (article.getStatus() == ArticleStatus.PENDING_REVIEW && !isAdmin(actor)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Bai dang cho duyet nen bi khoa sua. Cho bien tap tra lai thi sua tiep.");
        }
        boolean afterReview = article.getStatus() == ArticleStatus.SCHEDULED
                || article.getStatus() == ArticleStatus.PUBLISHED
                || article.getStatus() == ArticleStatus.ARCHIVED;
        if (afterReview && !isStaff(actor)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Bai da qua vong duyet, chi bien tap vien hoac quan tri vien sua duoc.");
        }
    }

    private NewsArticle requirePendingReview(Long id, AccountPrincipal actor) {
        ensureStaff(actor);
        NewsArticle article = requireNews(id);
        if (article.getStatus() != ArticleStatus.PENDING_REVIEW) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Bai nay khong o trang thai PENDING_REVIEW nen khong duyet duoc.");
        }
        return article;
    }

    private void ensureStaff(AccountPrincipal actor) {
        if (!isStaff(actor)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Chi bien tap vien hoac quan tri vien lam duoc viec nay - NFR-ED-003.");
        }
    }

    private boolean isStaff(AccountPrincipal principal) {
        return isAdmin(principal) || (principal != null && ROLE_EDITOR.equalsIgnoreCase(principal.roleCode()));
    }

    private boolean isAdmin(AccountPrincipal principal) {
        return principal != null && ROLE_ADMIN.equalsIgnoreCase(principal.roleCode());
    }

    /** Quy tac bien tap: tom tat phai du dai moi duoc nop - SRS muc 3.2. */
    private void ensureSummaryLongEnough(NewsArticle article) {
        int min = rules.current().getMinSummaryLength();
        if (min > 0 && article.getSummary().trim().length() < min) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Quy tac bien tap: tom tat phai co it nhat " + min + " ky tu moi nop duoc.");
        }
    }

    private void ensureVersionMatches(NewsArticle article, Long expected) {
        if (expected != null && expected != article.getVersion()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Bai nay vua co nguoi khac sua. Tai lai bai roi sua tiep.");
        }
    }

    /* =================================================================
       Dung chung
       ================================================================= */

    private void apply(NewsArticle article, NewsRequest request, AccountPrincipal actor) {
        article.setTitle(request.title().trim());
        article.setSlug(uniqueSlug(request.slug(), request.title(), article.getId()));
        article.setSummary(textOrEmpty(request.summary()));
        article.setContent(textOrEmpty(request.content()));
        article.setThumbnailUrl(trimToNull(request.thumbnailUrl()));
        article.setSource(textOrEmpty(request.source()));
        article.setCategory(requireCategory(request.categoryId()));
        // Bai noi bat la quyen cua bien tap, khong phai tac gia tu danh dau.
        if (isStaff(actor)) {
            article.setFeatured(Boolean.TRUE.equals(request.isFeatured()));
        }
        article.getTags().clear();
        article.getTags().addAll(resolveTags(request.tags()));
    }

    private Set<Tag> resolveTags(List<String> names) {
        Set<Tag> resolved = new LinkedHashSet<>();
        if (names == null) {
            return resolved;
        }
        for (String name : names) {
            String trimmed = trimToNull(name);
            if (trimmed == null) {
                continue;
            }
            String slug = Slugs.of(trimmed, "the");
            resolved.add(tags.findBySlugAndDeletedFalse(slug).orElseGet(() -> {
                Tag created = new Tag();
                created.setName(trimmed);
                created.setSlug(slug);
                return tags.save(created);
            }));
        }
        return resolved;
    }

    private String uniqueSlug(String requested, String title, Long currentId) {
        String base = Slugs.of(requested == null || requested.isBlank() ? title : requested, "bai-viet");
        String candidate = base;
        for (int suffix = 2; isSlugTaken(candidate, currentId); suffix++) {
            candidate = base + "-" + suffix;
        }
        return candidate;
    }

    private boolean isSlugTaken(String slug, Long currentId) {
        return news.findBySlugAndDeletedFalse(slug)
                .filter(other -> !other.getId().equals(currentId)).isPresent();
    }

    private void markReviewed(NewsArticle article, Account reviewer, ArticleStatus to) {
        article.setReviewer(reviewer);
        article.setReviewedAt(LocalDateTime.now(ZONE));
        article.setStatus(to);
    }

    private void publishNow(NewsArticle article, Account actor, LocalDateTime now) {
        ArticleStatus from = article.getStatus();
        article.setStatus(ArticleStatus.PUBLISHED);
        article.setPublishedAt(now);
        writeLog(news.saveAndFlush(article), from, ArticleStatus.PUBLISHED, "PUBLISH", null, actor);
        notifyAuthor(article, NotificationType.PUBLISHED,
                "Bai " + article.getTitle() + " da len trang cong khai.");
    }

    private void writeLog(NewsArticle article, ArticleStatus from, ArticleStatus to, String action,
            String note, Account actor) {
        ArticleAuditLog row = new ArticleAuditLog();
        row.setArticle(article);
        row.setFromStatus(from);
        row.setToStatus(to);
        row.setAction(action);
        row.setNote(note);
        row.setActor(actor);
        auditLogs.save(row);
    }

    private void notifyAuthor(NewsArticle article, NotificationType type, String message) {
        Notification notification = new Notification();
        notification.setAccount(article.getAuthor());
        notification.setArticle(article);
        notification.setType(type);
        notification.setMessage(message.length() > 1000 ? message.substring(0, 1000) : message);
        notifications.save(notification);
    }

    private Account requireAccount(AccountPrincipal principal) {
        return accounts.findById(requireId(principal))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap lai."));
    }

    private Long requireId(AccountPrincipal principal) {
        if (principal == null || principal.id() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap.");
        }
        return principal.id();
    }

    private Category requireCategory(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Khong tim thay danh muc."));
    }

    private NewsArticle requireNews(Long id) {
        return news.findById(id).filter(article -> !article.isDeleted())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Khong tim thay bai viet."));
    }

    private ArticleStatus statusForCreate(ArticleStatus status) {
        if (status == ArticleStatus.DRAFT || status == ArticleStatus.PENDING_REVIEW) {
            return status;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Tao bai chi duoc DRAFT hoac PENDING_REVIEW."
                + " Cac trang thai sau do do luong duyet bai quyet dinh - FR-ED-001.");
    }

    private ApiException badTransition(ArticleStatus from, ArticleStatus to) {
        return new ApiException(HttpStatus.CONFLICT,
                "Khong chuyen duoc tu " + from + " sang " + to + " - FR-ED-001.");
    }

    /*
       Moi cho ghi deu dung saveAndFlush chu khong save: cot version (@Version) va
       updatedAt chi duoc Hibernate dien luc flush, ma flush mac dinh xay ra sau khi
       ham tra ve. Dung save thi response mang version CU, client gui lai con so do
       o lan sua tiep theo va an 409 oan.
    */
    private NewsResponse newsResponse(NewsArticle article) {
        return new NewsResponse(article.getId(), article.getTitle(), article.getSlug(), article.getSummary(),
                article.getContent(), article.getThumbnailUrl(), article.getSource(),
                article.getCategory().getId(), article.getStatus(), article.getAuthor().getId(),
                article.getReviewer() == null ? null : article.getReviewer().getId(),
                article.getSubmittedAt(), article.getReviewedAt(), article.getScheduledPublishAt(),
                article.getPublishedAt(), article.getViewCount(), article.getRatingCount(),
                article.getRatingAvg(), article.isFeatured(),
                article.getTags().stream().map(Tag::getName).toList(),
                article.getCreatedAt(), article.getUpdatedAt(), article.getVersion());
    }

    private ArticleAuditLogResponse auditResponse(NewsArticle article, ArticleAuditLog row) {
        return new ArticleAuditLogResponse(row.getId(), article.getId(), row.getFromStatus(), row.getToStatus(),
                row.getAction(), row.getNote(), row.getActor().getId(), row.getCreatedAt());
    }

    private NotificationResponse notificationResponse(Notification notification) {
        return new NotificationResponse(notification.getId(),
                notification.getArticle() == null ? null : notification.getArticle().getId(),
                notification.getType(), notification.getMessage(), notification.isRead(),
                notification.getReadAt(), notification.getCreatedAt());
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
