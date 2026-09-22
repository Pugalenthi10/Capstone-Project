package com.trichyestates.estatehub;

import com.trichyestates.estatehub.entity.PasswordResetToken;
import com.trichyestates.estatehub.entity.Role;
import com.trichyestates.estatehub.entity.User;
import com.trichyestates.estatehub.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthApiTest extends AbstractApiTest {

    @Autowired PasswordResetTokenRepository tokenRepository;

    private String registerBody(String email, String mobile) throws Exception {
        return json("name", "Divya R", "email", email, "mobile", mobile, "password", PASSWORD);
    }

    @Test
    void healthCheckIsPublic() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Trichy Estates backend is running"));
    }

    @Test
    void registerCreatesUserWithHashedPasswordAndReturnsToken() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("Divya@Example.com", "9876543210")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.user.email").value("divya@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.password").doesNotExist());

        User saved = userRepository.findByEmail("divya@example.com").orElseThrow();
        assertNotEquals(PASSWORD, saved.getPassword());
        assertTrue(saved.getPassword().startsWith("$2"), "password must be a BCrypt hash");
        assertTrue(passwordEncoder.matches(PASSWORD, saved.getPassword()));
    }

    @Test
    void registerCannotSelfAssignAdminRole() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Eve", "email", "eve@example.com", "mobile", "9876500001",
                                "password", PASSWORD, "role", "ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        createUser("dup@example.com", "9876500002", Role.USER);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("dup@example.com", "9876500003")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void registerRejectsDuplicateMobile() throws Exception {
        createUser("first@example.com", "9876500004", Role.USER);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("second@example.com", "9876500004")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.mobile").exists());
    }

    @Test
    void registerValidatesFields() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "", "email", "not-an-email", "mobile", "12345", "password", "abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.mobile").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void loginReturnsTokenAndUserWithoutPassword() throws Exception {
        createUser("login@example.com", "9876500005", Role.USER);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "LOGIN@example.com", "password", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.user.email").value("login@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void loginRejectsWrongPasswordAndUnknownEmailIdentically() throws Exception {
        createUser("real@example.com", "9876500006", Role.USER);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "real@example.com", "password", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "ghost@example.com", "password", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointRejectsMissingAndInvalidTokens() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPasswordGivesSameAnswerForKnownAndUnknownEmail() throws Exception {
        createUser("known@example.com", "9876500007", Role.USER);
        String known = mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "known@example.com")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String unknown = mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "nobody@example.com")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(known, unknown);
    }

    @Test
    void resetPasswordWorksOnceAndTokenExpires() throws Exception {
        User user = createUser("reset@example.com", "9876500008", Role.USER);
        tokenRepository.saveAndFlush(new PasswordResetToken(user, sha256("good-token"),
                Instant.now().plus(10, ChronoUnit.MINUTES)));
        tokenRepository.saveAndFlush(new PasswordResetToken(user, sha256("old-token"),
                Instant.now().minus(1, ChronoUnit.MINUTES)));

        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json("token", "old-token", "newPassword", "BrandNew1")))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json("token", "good-token", "newPassword", "BrandNew1")))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "reset@example.com", "password", "BrandNew1")))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json("token", "good-token", "newPassword", "AnotherOne1")))
                .andExpect(status().isBadRequest());
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
