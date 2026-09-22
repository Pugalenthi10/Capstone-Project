package com.trichyestates.estatehub.config;

import com.trichyestates.estatehub.entity.ListingType;
import com.trichyestates.estatehub.entity.Property;
import com.trichyestates.estatehub.entity.PropertyCategory;
import com.trichyestates.estatehub.entity.Role;
import com.trichyestates.estatehub.entity.User;
import com.trichyestates.estatehub.repository.PropertyRepository;
import com.trichyestates.estatehub.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.List;

/**
 * Seeds one ADMIN, one AGENT and the 30 original listings (resources/seed/properties.json).
 * Idempotent: users are matched by e-mail and properties by seed_code, so restarts never duplicate anything.
 * Disable in production with SEED_ENABLED=false.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String ALPHABET = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public record SeedProperty(String seedCode, PropertyCategory category, String title, String location,
                               String city, Long price, String priceLabel, Integer bedrooms, Integer bathrooms,
                               Integer area, String description, String imageUrl, ListingType listingType) { }

    private final AppProperties props;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper mapper;

    public DataSeeder(AppProperties props, UserRepository userRepository, PropertyRepository propertyRepository,
                      PasswordEncoder passwordEncoder, ObjectMapper mapper) {
        this.props = props;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        AppProperties.Seed seed = props.seed();
        if (!seed.enabled()) {
            log.info("Data seeding is disabled (SEED_ENABLED=false)");
            return;
        }

        ensureUser("Trichy Estates Admin", seed.adminEmail(), seed.adminMobile(), seed.adminPassword(), Role.ADMIN);
        User agent = ensureUser("Trichy Estates Agent", seed.agentEmail(), seed.agentMobile(),
                seed.agentPassword(), Role.AGENT);

        int inserted = 0;
        try (InputStream in = new ClassPathResource("seed/properties.json").getInputStream()) {
            List<SeedProperty> items = mapper.readValue(in, new TypeReference<List<SeedProperty>>() { });
            for (SeedProperty s : items) {
                if (propertyRepository.existsBySeedCode(s.seedCode())) {
                    continue;
                }
                Property p = new Property();
                p.setSeedCode(s.seedCode());
                p.setTitle(s.title());
                p.setCategory(s.category());
                p.setLocation(s.location());
                p.setCity(s.city());
                p.setState("Tamil Nadu");
                p.setPrice(s.price());
                p.setPriceLabel(s.priceLabel());
                p.setBedrooms(s.bedrooms());
                p.setBathrooms(s.bathrooms());
                p.setArea(s.area());
                p.setDescription(s.description());
                p.setImageUrl(s.imageUrl());
                p.setListingType(s.listingType() == null ? ListingType.SALE : s.listingType());
                p.setAgent(agent);
                propertyRepository.save(p);
                inserted++;
            }
        }
        log.info("Seeding complete: {} new properties inserted ({} total in database)",
                inserted, propertyRepository.count());
    }

    private User ensureUser(String name, String email, String mobile, String configuredPassword, Role role) {
        return userRepository.findByEmail(email.toLowerCase()).orElseGet(() -> {
            boolean generated = configuredPassword == null || configuredPassword.isBlank();
            String password = generated ? randomPassword() : configuredPassword;

            User user = new User();
            user.setName(name);
            user.setEmail(email.toLowerCase());
            user.setMobile(mobile);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role);
            User saved = userRepository.save(user);

            if (generated) {
                log.warn("DEVELOPMENT ONLY - created {} account {} with generated password: {}  "
                        + "(shown once; set SEED_{}_PASSWORD to choose your own, or SEED_ENABLED=false in production)",
                        role, email, password, role);
            } else {
                log.info("Created {} account {} using the password from the environment", role, email);
            }
            return saved;
        });
    }

    private static String randomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
