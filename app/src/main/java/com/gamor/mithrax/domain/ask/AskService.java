package com.gamor.mithrax.domain.ask;

import androidx.annotation.NonNull;

/**
 * Ask MithraX using only on-device context. Implementations must not call a network.
 */
public interface AskService {

    @NonNull
    AskResult ask(@NonNull String question);
}
