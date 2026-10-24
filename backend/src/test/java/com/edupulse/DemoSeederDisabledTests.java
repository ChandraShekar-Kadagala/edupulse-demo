package com.edupulse;

import com.edupulse.repository.UserRepository;
import com.edupulse.seeder.DataSeeder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.seeder.enabled=false"
})
public class DemoSeederDisabledTests {

    @Autowired(required = false)
    private DataSeeder dataSeeder;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("When app.seeder.enabled=false, DataSeeder bean is not registered and no accounts are created")
    public void testSeederDisabled_NoBeanAndNoAccountsCreated() {
        assertNull(dataSeeder, "DataSeeder bean must NOT be instantiated when app.seeder.enabled=false");
        assertEquals(0, userRepository.count(), "No accounts should be created when seeding is disabled");
    }
}
