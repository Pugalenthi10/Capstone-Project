package com.trichyestates.estatehub;

import com.trichyestates.estatehub.entity.Property;
import com.trichyestates.estatehub.entity.PropertyCategory;
import com.trichyestates.estatehub.entity.Role;
import com.trichyestates.estatehub.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Favorites, cart, enquiries, profile and admin user management. */
class UserFeaturesApiTest extends AbstractApiTest {

    User alice, bob, agent, otherAgent, admin;
    Property p1, p2, otherAgentsProperty;

    @BeforeEach
    void data() {
        alice = createUser("alice@example.com", "9100000001", Role.USER);
        bob = createUser("bob@example.com", "9100000002", Role.USER);
        agent = createUser("agent@example.com", "9100000003", Role.AGENT);
        otherAgent = createUser("agent2@example.com", "9100000004", Role.AGENT);
        admin = createUser("admin@example.com", "9100000005", Role.ADMIN);
        p1 = createProperty("Alpha", PropertyCategory.FLAT, "Puthur", 5_000_000, 2, 2, 1000, agent);
        p2 = createProperty("Beta", PropertyCategory.HOUSE, "Kattur", 8_000_000, 3, 3, 2000, agent);
        otherAgentsProperty = createProperty("Gamma", PropertyCategory.APARTMENT, "Woraiyur", 6_000_000, 2, 2, 1200, otherAgent);
    }

    // ---------------------------------------------------------------- favorites

    @Test
    void favoritesRequireLogin() throws Exception {
        mvc.perform(get("/api/favorites")).andExpect(status().isUnauthorized());
    }

    @Test
    void favoritesAreUserSpecificAndIdempotent() throws Exception {
        mvc.perform(post("/api/favorites/" + p1.getId()).header("Authorization", bearer(alice)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/favorites/" + p1.getId()).header("Authorization", bearer(alice)))
                .andExpect(status().isOk());   // already there: no duplicate row, no error

        mvc.perform(get("/api/favorites").header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Alpha"));
        mvc.perform(get("/api/favorites").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(delete("/api/favorites/" + p1.getId()).header("Authorization", bearer(alice)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/favorites").header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(post("/api/favorites/999999").header("Authorization", bearer(alice)))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- cart

    @Test
    void cartAddListRemoveClearAndIsolation() throws Exception {
        mvc.perform(post("/api/cart/" + p1.getId()).header("Authorization", bearer(alice))).andExpect(status().isCreated());
        mvc.perform(post("/api/cart/" + p2.getId()).header("Authorization", bearer(alice))).andExpect(status().isCreated());
        mvc.perform(post("/api/cart/" + p2.getId()).header("Authorization", bearer(alice))).andExpect(status().isOk());

        mvc.perform(get("/api/cart").header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/cart").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(delete("/api/cart/" + p1.getId()).header("Authorization", bearer(alice)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/cart").header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Beta"));

        mvc.perform(delete("/api/cart").header("Authorization", bearer(alice)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/cart").header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ---------------------------------------------------------------- inquiries

    @Test
    void inquiryAlwaysBelongsToAuthenticatedUser() throws Exception {
        // A forged userId in the body must be ignored.
        mvc.perform(post("/api/inquiries").header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("propertyId", p1.getId(), "message", "Is it available?", "userId", bob.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.buyerId").value(alice.getId()))
                .andExpect(jsonPath("$.propertyTitle").value("Alpha"));

        mvc.perform(get("/api/inquiries/my").header("Authorization", bearer(alice)))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/inquiries/my").header("Authorization", bearer(bob)))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void inquiryValidationAndNotFound() throws Exception {
        mvc.perform(post("/api/inquiries").header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON).content(json("message", "hi")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.propertyId").exists());
        mvc.perform(post("/api/inquiries").header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON).content(json("propertyId", 999999)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/inquiries").contentType(MediaType.APPLICATION_JSON)
                        .content(json("propertyId", p1.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bulkInquiryIsAllOrNothing() throws Exception {
        mvc.perform(post("/api/inquiries/bulk").header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("propertyIds", java.util.List.of(p1.getId(), 999999))))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/inquiries/bulk").header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("propertyIds", java.util.List.of(p1.getId(), p2.getId(), p1.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)));   // duplicate id collapsed
    }

    @Test
    void agentSeesOnlyOwnEnquiriesAdminSeesAll() throws Exception {
        mvc.perform(post("/api/inquiries").header("Authorization", bearer(alice))
                .contentType(MediaType.APPLICATION_JSON).content(json("propertyId", p1.getId())));
        mvc.perform(post("/api/inquiries").header("Authorization", bearer(alice))
                .contentType(MediaType.APPLICATION_JSON).content(json("propertyId", otherAgentsProperty.getId())));

        mvc.perform(get("/api/inquiries/agent").header("Authorization", bearer(agent)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].propertyTitle").value("Alpha"))
                .andExpect(jsonPath("$[0].buyerEmail").value("alice@example.com"));
        mvc.perform(get("/api/inquiries/agent").header("Authorization", bearer(alice)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/inquiries").header("Authorization", bearer(agent)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/inquiries").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void onlyOwningAgentOrAdminCanUpdateEnquiryStatus() throws Exception {
        String created = mvc.perform(post("/api/inquiries").header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON).content(json("propertyId", p1.getId())))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).get("id").asLong();

        mvc.perform(patch("/api/inquiries/" + id + "/status").header("Authorization", bearer(otherAgent))
                        .contentType(MediaType.APPLICATION_JSON).content(json("status", "CONTACTED")))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/inquiries/" + id + "/status").header("Authorization", bearer(agent))
                        .contentType(MediaType.APPLICATION_JSON).content(json("status", "CONTACTED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTACTED"));
    }

    // ---------------------------------------------------------------- profile

    @Test
    void profileCanBeReadAndUpdatedButNotItsRole() throws Exception {
        mvc.perform(get("/api/users/me").header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mvc.perform(put("/api/users/me").header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Alice Renamed", "mobile", "9876512345", "role", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Renamed"))
                .andExpect(jsonPath("$.mobile").value("9876512345"))
                .andExpect(jsonPath("$.role").value("USER"));   // role in the body is ignored
    }

    @Test
    void profileRejectsMobileOfAnotherUser() throws Exception {
        mvc.perform(put("/api/users/me").header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Alice", "mobile", bob.getMobile())))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- admin

    @Test
    void onlyAdminCanChangeRoles() throws Exception {
        mvc.perform(patch("/api/admin/users/" + alice.getId() + "/role").header("Authorization", bearer(agent))
                        .contentType(MediaType.APPLICATION_JSON).content(json("role", "AGENT")))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/users/" + alice.getId() + "/role").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json("role", "AGENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("AGENT"));
        mvc.perform(patch("/api/admin/users/" + admin.getId() + "/role").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json("role", "USER")))
                .andExpect(status().isBadRequest());   // cannot demote yourself
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
    }
}
