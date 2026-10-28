package com.funews.backend.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Sinh slug tu tieu de tieng Viet: "Thong bao tuyen sinh 2026" -> "thong-bao-tuyen-sinh-2026".
 *
 * Normalizer NFD tach chu va dau thanh hai ky tu roi bo phan dau di, nen "e" co
 * dau thanh "e". Rieng chu d gach ngang khong phai la d cong dau nen NFD khong
 * tach duoc, phai thay tay.
 */
public final class Slugs {
    private static final int MAX_LENGTH = 180;

    private Slugs() {
    }

    public static String of(String text, String fallback) {
        String slug = slugify(text);
        return slug.isEmpty() ? slugify(fallback) : slug;
    }

    private static String slugify(String text) {
        if (text == null) {
            return "";
        }
        String ascii = Normalizer.normalize(text.replace('đ', 'd').replace('Đ', 'D'),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String slug = ascii.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.length() <= MAX_LENGTH ? slug
                : slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
    }
}
