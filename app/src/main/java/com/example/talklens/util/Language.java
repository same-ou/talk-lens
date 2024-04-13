package com.example.talklens.util;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Locale;

/**
 * Holds the language code (i.e. "en") and the corresponding localized full language name (i.e.
 * "English")
 */
public class Language implements Serializable,Comparable<Language> {
    private final String code;

    public Language(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return new Locale(code).getDisplayName();
    }

    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Language)) {
            return false;
        }

        Language otherLang = (Language) o;
        return otherLang.code.equals(code);
    }

    @NonNull
    @Override
    public String toString() {
        return  getDisplayName();
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public int compareTo(@NonNull Language o) {
        return this.getDisplayName().compareTo(o.getDisplayName());
    }
}