package com.vesta.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHashTest {

    @Test
    void hashesTokensDeterministicallyWithoutPersistingRawValue() {
        String token = TokenHash.randomToken();

        assertThat(token).hasSize(64);
        assertThat(TokenHash.sha256(token)).hasSize(64).isNotEqualTo(token);
        assertThat(TokenHash.sha256(token)).isEqualTo(TokenHash.sha256(token));
    }
}

