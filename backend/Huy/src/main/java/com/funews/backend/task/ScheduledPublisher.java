package com.funews.backend.task;

import com.funews.backend.service.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AC-ED-003: bai da duyet va co gio hen thi tu chuyen sang PUBLISHED khi den gio.
 *
 * Chay moi phut. Chu y hai dieu:
 *   - Nuot loi tai day. Mot lan chay that bai khong duoc lam chet lich: lan sau
 *     quet lai la bai do van con trong hang doi.
 *   - Neu chay nhieu ban ung dung cung luc, cot articles.version (@Version) lo
 *     phan con lai: ban nao ghi sau se that bai thay vi dang bai hai lan.
 */
@Component
public class ScheduledPublisher {
    private static final Logger log = LoggerFactory.getLogger(ScheduledPublisher.class);

    private final NewsService news;

    public ScheduledPublisher(NewsService news) {
        this.news = news;
    }

    @Scheduled(fixedDelayString = "${app.publisher.delay-ms:60000}",
            initialDelayString = "${app.publisher.initial-delay-ms:15000}")
    public void publishDueArticles() {
        try {
            int published = news.publishDueArticles();
            if (published > 0) {
                log.info("Da tu dang {} bai den gio hen.", published);
            }
        } catch (RuntimeException exception) {
            log.warn("Lan quet bai den gio hen that bai, se thu lai o lan sau.", exception);
        }
    }
}
