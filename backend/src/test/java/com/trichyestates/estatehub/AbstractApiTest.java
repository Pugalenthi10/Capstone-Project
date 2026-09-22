package com.trichyestates.estatehub;

import com.trichyestates.estatehub.entity.*;
import com.trichyestates.estatehub.repository.PropertyRepository;
import com.trichyestates.estatehub.repository.UserRepository;
import com.trichyestates.estatehub.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** Every test runs in a transaction that is rolled back, so tests never leak data into each other. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractApiTest {

    protected static final String PASSWORD = "Passw0rd!";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper mapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PropertyRepository propertyRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;

    protected User createUser(String email, String mobile, Role role) {
        User u = new User();
        u.setName("Test " + role);
        u.setEmail(email);
        u.setMobile(mobile);
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setRole(role);
        return userRepository.saveAndFlush(u);
    }

    protected String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    protected Property createProperty(String title, PropertyCategory category, String location,
                                      long price, int beds, int baths, int area, User agent) {
        Property p = new Property();
        p.setTitle(title);
        p.setCategory(category);
        p.setLocation(location);
        p.setCity("Trichy");
        p.setState("Tamil Nadu");
        p.setPrice(price);
        p.setPriceLabel("label");
        p.setBedrooms(beds);
        p.setBathrooms(baths);
        p.setArea(area);
        p.setDescription("desc " + title);
        p.setListingType(ListingType.SALE);
        p.setAgent(agent);
        return propertyRepository.saveAndFlush(p);
    }

    /** json("a", 1, "b", "x") -> {"a":1,"b":"x"} */
    protected String json(Object... keyValues) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return mapper.writeValueAsString(map);
    }
}
