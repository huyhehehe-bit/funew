package com.funews.backend.service;

import com.funews.backend.dto.EditorialRulesRequest;
import com.funews.backend.dto.EditorialRulesResponse;
import com.funews.backend.entity.Account;
import com.funews.backend.entity.EditorialRules;
import com.funews.backend.exception.ApiException;
import com.funews.backend.repository.AccountRepository;
import com.funews.backend.repository.EditorialRulesRepository;
import com.funews.backend.security.AccountPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quy tac bien tap - SRS muc 3.2, Administrator cau hinh.
 *
 * current() khong bao gio nem loi: thieu dong cau hinh thi tra ve mot
 * EditorialRules moi voi gia tri mac dinh. Nho the database chua chay script
 * nang cap van khong lam chet luong duyet bai.
 */
@Service
@Transactional(readOnly = true)
public class EditorialRulesService {
    private final EditorialRulesRepository rules;
    private final AccountRepository accounts;

    public EditorialRulesService(EditorialRulesRepository rules, AccountRepository accounts) {
        this.rules = rules;
        this.accounts = accounts;
    }

    /** Dung cho tang service khac doc quy tac. */
    public EditorialRules current() {
        return rules.findById(EditorialRules.SINGLE_ROW_ID).orElseGet(EditorialRules::new);
    }

    public EditorialRulesResponse get() {
        return response(current());
    }

    @Transactional
    public EditorialRulesResponse update(EditorialRulesRequest request, AccountPrincipal actor) {
        EditorialRules current = current();
        current.setId(EditorialRules.SINGLE_ROW_ID);
        current.setAutoPublishScheduled(request.autoPublishScheduled());
        current.setRequireNoteOnApprove(request.requireNoteOnApprove());
        current.setMinSummaryLength(request.minSummaryLength());
        current.setUpdatedBy(actorAccount(actor));
        // saveAndFlush chu khong save: @UpdateTimestamp chi duoc dien luc flush,
        // ma flush mac dinh xay ra sau khi ham nay tra ve - khi do response da
        // mang theo moc thoi gian cu.
        return response(rules.saveAndFlush(current));
    }

    private Account actorAccount(AccountPrincipal actor) {
        if (actor == null || actor.id() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap.");
        }
        return accounts.findById(actor.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Vui long dang nhap lai."));
    }

    private EditorialRulesResponse response(EditorialRules entity) {
        return new EditorialRulesResponse(entity.isAutoPublishScheduled(), entity.isRequireNoteOnApprove(),
                entity.getMinSummaryLength(), entity.getUpdatedAt(),
                entity.getUpdatedBy() == null ? null : entity.getUpdatedBy().getId());
    }
}
