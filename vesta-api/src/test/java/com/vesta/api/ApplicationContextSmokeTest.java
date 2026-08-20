package com.vesta.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class ApplicationContextSmokeTest {

    @Test
    void contextLoadsAndRepositoryQueriesAreValid() {
    }
}

