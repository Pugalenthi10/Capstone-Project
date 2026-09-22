package com.trichyestates.estatehub;

import com.trichyestates.estatehub.config.DataSeeder;
import com.trichyestates.estatehub.entity.Role;
import com.trichyestates.estatehub.repository.PropertyRepository;
import com.trichyestates.estatehub.repository.UserRepository;
import com.trichyestates.estatehub.util.PriceFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Uses its own H2 database so committed seed data cannot leak into the other (rolled-back) tests. */
@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "app.seed.admin-password=Admin-Dev-Pass1",
        "app.seed.agent-password=Agent-Dev-Pass1",
        "spring.datasource.url=jdbc:h2:mem:seeder_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class DataSeederTest {

    @Autowired DataSeeder seeder;
    @Autowired PropertyRepository propertyRepository;
    @Autowired UserRepository userRepository;

    @Test
    void seedsThirtyPropertiesAndTwoAccountsExactlyOnce() throws Exception {
        assertEquals(30, propertyRepository.count());
        assertEquals(Role.ADMIN, userRepository.findByEmail("admin@trichyestates.local").orElseThrow().getRole());
        assertEquals(Role.AGENT, userRepository.findByEmail("agent@trichyestates.local").orElseThrow().getRole());

        seeder.run(null);   // simulate an application restart
        seeder.run(null);

        assertEquals(30, propertyRepository.count(), "seeding must not duplicate properties");
        assertEquals(2, userRepository.count(), "seeding must not duplicate accounts");
        assertNotEquals("Admin-Dev-Pass1",
                userRepository.findByEmail("admin@trichyestates.local").orElseThrow().getPassword());
    }

    @Test
    void seededListingsKeepTheOriginalCategoriesAndLabels() {
        List<com.trichyestates.estatehub.entity.Property> all = propertyRepository.findAll();
        assertEquals(10, all.stream().filter(p -> p.getCategory().name().equals("APARTMENT")).count());
        assertEquals(10, all.stream().filter(p -> p.getCategory().name().equals("HOUSE")).count());
        assertEquals(10, all.stream().filter(p -> p.getCategory().name().equals("FLAT")).count());
        // The generated label logic reproduces every original hard-coded label exactly.
        all.forEach(p -> assertEquals(p.getPriceLabel(), PriceFormatter.label(p.getPrice()), p.getTitle()));
    }
}
