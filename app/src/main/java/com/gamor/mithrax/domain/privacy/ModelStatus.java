package com.gamor.mithrax.domain.privacy;

import androidx.annotation.NonNull;

/**
 * What this build actually uses for speech and answers. Must not claim a cloud model.
 */
public final class ModelStatus {

    @NonNull
    public final String speechTitle;
    @NonNull
    public final String speechDetail;
    @NonNull
    public final String languageTitle;
    @NonNull
    public final String languageDetail;
    public final boolean speechModelBundled;
    public final boolean onDeviceLanguageModelInstalled;

    public ModelStatus(@NonNull String speechTitle,
                       @NonNull String speechDetail,
                       @NonNull String languageTitle,
                       @NonNull String languageDetail,
                       boolean speechModelBundled,
                       boolean onDeviceLanguageModelInstalled) {
        this.speechTitle = speechTitle;
        this.speechDetail = speechDetail;
        this.languageTitle = languageTitle;
        this.languageDetail = languageDetail;
        this.speechModelBundled = speechModelBundled;
        this.onDeviceLanguageModelInstalled = onDeviceLanguageModelInstalled;
    }
}
