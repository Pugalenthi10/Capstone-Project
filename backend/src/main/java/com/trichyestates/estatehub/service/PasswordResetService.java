package com.trichyestates.estatehub.service;

import com.trichyestates.estatehub.config.AppProperties;
import com.trichyestates.estatehub.entity.PasswordResetToken;
import com.trichyestates.estatehub.entity.User;
import com.trichyestates.estatehub.exception.BadRequestException;
import com.trichyestates.estatehub.repository.PasswordResetTokenRepository;
import com.trichyestates.estatehub.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String INVALID = "This reset link is invalid or has expired";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AppProperties props;

    public PasswordResetService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder, MailService mailService, AppProperties props) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.props = props;
    }

    /**
     * Always completes silently, whether or not the e-mail belongs to an account, so the API cannot be
     * used to discover which addresses are registered.
     */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(AuthService.normalizeEmail(email)).ifPresent(user -> {
            tokenRepository.invalidateAllForUser(user.getId());   // only the newest link works

            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            Instant expiresAt = Instant.now().plus(Duration.ofMinutes(props.passwordReset().expirationMinutes()));
            tokenRepository.save(new PasswordResetToken(user, sha256(rawToken), expiresAt));

            String base = props.frontendUrl().replaceAll("/+$", "");
            mailService.sendPasswordReset(user.getEmail(), user.getName(), base + "/?reset=" + rawToken);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(sha256(rawToken.trim()))
                .orElseThrow(() -> new BadRequestException(INVALID));
        if (!token.isUsable(Instant.now())) {
            throw new BadRequestException(INVALID);
        }
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        token.setUsed(true);                                   // one-time use
        tokenRepository.invalidateAllForUser(user.getId());    // and kill any other outstanding links
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
