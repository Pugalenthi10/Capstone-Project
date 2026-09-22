-- Trichy Estates schema (MySQL 8+). Owned by Flyway; Hibernate never alters it.

CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    mobile      VARCHAR(15)  NOT NULL,
    password    VARCHAR(100) NOT NULL,          -- BCrypt hash only, never plaintext
    role        VARCHAR(20)  NOT NULL,          -- USER | AGENT | ADMIN
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email  UNIQUE (email),
    CONSTRAINT uk_users_mobile UNIQUE (mobile),
    CONSTRAINT ck_users_role   CHECK (role IN ('USER', 'AGENT', 'ADMIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE properties (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    seed_code    VARCHAR(20)   NULL,            -- stable id of the original demo data; makes seeding idempotent
    title        VARCHAR(200)  NOT NULL,
    category     VARCHAR(20)   NOT NULL,        -- APARTMENT | HOUSE | FLAT
    location     VARCHAR(150)  NOT NULL,        -- locality, e.g. "Thillai Nagar"
    city         VARCHAR(100)  NOT NULL,
    state        VARCHAR(100)  NOT NULL,
    price        BIGINT        NOT NULL,        -- rupees
    price_label  VARCHAR(50)   NOT NULL,        -- display text, e.g. "₹78 Lakhs"
    bedrooms     INT           NOT NULL,
    bathrooms    INT           NOT NULL,
    area         INT           NOT NULL,        -- square feet
    description  VARCHAR(2000) NOT NULL,
    image_url    VARCHAR(1000) NULL,
    listing_type VARCHAR(10)   NOT NULL,        -- SALE | RENT
    agent_id     BIGINT        NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_properties_seed_code UNIQUE (seed_code),
    CONSTRAINT fk_properties_agent FOREIGN KEY (agent_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_properties_category CHECK (category IN ('APARTMENT', 'HOUSE', 'FLAT')),
    CONSTRAINT ck_properties_listing  CHECK (listing_type IN ('SALE', 'RENT')),
    CONSTRAINT ck_properties_price    CHECK (price > 0),
    CONSTRAINT ck_properties_area     CHECK (area > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_properties_location     ON properties (location);
CREATE INDEX idx_properties_city         ON properties (city);
CREATE INDEX idx_properties_category     ON properties (category);
CREATE INDEX idx_properties_listing_type ON properties (listing_type);
CREATE INDEX idx_properties_price        ON properties (price);
CREATE INDEX idx_properties_agent        ON properties (agent_id);

CREATE TABLE favorites (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    property_id  BIGINT      NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_favorites_user_property UNIQUE (user_id, property_id),
    CONSTRAINT fk_favorites_user     FOREIGN KEY (user_id)     REFERENCES users (id)      ON DELETE CASCADE,
    CONSTRAINT fk_favorites_property FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE cart_items (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    property_id  BIGINT      NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_cart_user_property UNIQUE (user_id, property_id),
    CONSTRAINT fk_cart_user     FOREIGN KEY (user_id)     REFERENCES users (id)      ON DELETE CASCADE,
    CONSTRAINT fk_cart_property FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE inquiries (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      BIGINT        NOT NULL,
    property_id  BIGINT        NOT NULL,
    message      VARCHAR(1000) NOT NULL,
    status       VARCHAR(20)   NOT NULL,        -- NEW | CONTACTED | CLOSED
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_inquiries_user     FOREIGN KEY (user_id)     REFERENCES users (id)      ON DELETE CASCADE,
    CONSTRAINT fk_inquiries_property FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE,
    CONSTRAINT ck_inquiries_status   CHECK (status IN ('NEW', 'CONTACTED', 'CLOSED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_inquiries_user     ON inquiries (user_id);
CREATE INDEX idx_inquiries_property ON inquiries (property_id);
CREATE INDEX idx_inquiries_status   ON inquiries (status);

CREATE TABLE password_reset_tokens (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,           -- SHA-256 of the emailed token; the raw token is never stored
    expires_at  DATETIME(6) NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
