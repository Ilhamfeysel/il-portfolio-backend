package com.ilham.portfolio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest (properties = "gemini.api.key=test-key")
class IlPortfolioBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
