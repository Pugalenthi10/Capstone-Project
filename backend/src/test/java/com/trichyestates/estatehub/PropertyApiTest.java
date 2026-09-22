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

class PropertyApiTest extends AbstractApiTest {

    User agentA, agentB, admin, buyer;
    Property flat, apartment, house;

    @BeforeEach
    void data() {
        agentA = createUser("agenta@example.com", "9000000011", Role.AGENT);
        agentB = createUser("agentb@example.com", "9000000012", Role.AGENT);
        admin = createUser("admin@example.com", "9000000013", Role.ADMIN);
        buyer = createUser("buyer@example.com", "9000000014", Role.USER);
        flat = createProperty("Metro Square Flat", PropertyCategory.FLAT, "Thillai Nagar", 5_900_000, 2, 2, 1160, agentA);
        apartment = createProperty("Emerald Heights", PropertyCategory.APARTMENT, "Srirangam", 7_800_000, 3, 2, 1540, agentA);
        house = createProperty("Oak Grove Residence", PropertyCategory.HOUSE, "KK Nagar", 11_200_000, 3, 3, 2250, agentB);
    }

    private String body(String title) throws Exception {
        return json("title", title, "category", "HOUSE", "location", "Cantonment", "price", 9_200_000,
                "bedrooms", 3, "bathrooms", 2, "area", 1800, "description", "A lovely home",
                "imageUrl", "https://example.com/a.jpg");
    }

    @Test
    void listIsPublicAndPaged() throws Exception {
        mvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].title").value("Metro Square Flat"))   // featured = insertion order
                .andExpect(jsonPath("$.content[0].agentId").value(agentA.getId()));
        mvc.perform(get("/api/properties").param("size", "2"))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void filterByCategoryIsCaseInsensitiveAndAllMeansNoFilter() throws Exception {
        mvc.perform(get("/api/properties").param("category", "apartment"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].category").value("APARTMENT"));
        mvc.perform(get("/api/properties").param("category", "all"))
                .andExpect(jsonPath("$.content", hasSize(3)));
        mvc.perform(get("/api/properties").param("category", "castle"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sortingMatchesTheFrontendDropdown() throws Exception {
        mvc.perform(get("/api/properties").param("sort", "low"))
                .andExpect(jsonPath("$.content[0].title").value("Metro Square Flat"));
        mvc.perform(get("/api/properties").param("sort", "high"))
                .andExpect(jsonPath("$.content[0].title").value("Oak Grove Residence"));
        mvc.perform(get("/api/properties").param("sort", "area"))
                .andExpect(jsonPath("$.content[0].title").value("Oak Grove Residence"));
        mvc.perform(get("/api/properties").param("sort", "nonsense"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchCombinesFilters() throws Exception {
        mvc.perform(get("/api/properties/search").param("location", "thillai").param("category", "FLAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Metro Square Flat"));
        mvc.perform(get("/api/properties/search").param("minPrice", "7000000").param("maxPrice", "8000000"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Emerald Heights"));
        mvc.perform(get("/api/properties/search").param("bedrooms", "3").param("bathrooms", "3"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Oak Grove Residence"));
        mvc.perform(get("/api/properties/search").param("location", "trichy"))    // matches the city too
                .andExpect(jsonPath("$.content", hasSize(3)));
        mvc.perform(get("/api/properties/search").param("location", "%"))         // wildcard is escaped, not a match-all
                .andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/api/properties/search").param("minPrice", "9").param("maxPrice", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdAndNotFound() throws Exception {
        mvc.perform(get("/api/properties/" + house.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Oak Grove Residence"))
                .andExpect(jsonPath("$.location").value("KK Nagar"));
        mvc.perform(get("/api/properties/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Property not found"));
    }

    @Test
    void anonymousAndNormalUsersCannotCreate() throws Exception {
        mvc.perform(post("/api/properties").contentType(MediaType.APPLICATION_JSON).content(body("Nope")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/properties").header("Authorization", bearer(buyer))
                        .contentType(MediaType.APPLICATION_JSON).content(body("Nope")))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCanCreateAndBecomesOwner() throws Exception {
        mvc.perform(post("/api/properties").header("Authorization", bearer(agentA))
                        .contentType(MediaType.APPLICATION_JSON).content(body("Cantonment Villa")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.agentId").value(agentA.getId()))
                .andExpect(jsonPath("$.priceLabel").value("₹92 Lakhs"))   // generated from price
                .andExpect(jsonPath("$.city").value("Trichy"));
    }

    @Test
    void createValidatesInput() throws Exception {
        mvc.perform(post("/api/properties").header("Authorization", bearer(agentA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", "", "price", -5, "imageUrl", "javascript:alert(1)")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.imageUrl").exists());
    }

    @Test
    void agentCannotModifyAnotherAgentsPropertyButOwnerAndAdminCan() throws Exception {
        // house belongs to agentB
        mvc.perform(put("/api/properties/" + house.getId()).header("Authorization", bearer(agentA))
                        .contentType(MediaType.APPLICATION_JSON).content(body("Hijacked")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/properties/" + house.getId()).header("Authorization", bearer(agentA)))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/properties/" + house.getId()).header("Authorization", bearer(agentB))
                        .contentType(MediaType.APPLICATION_JSON).content(body("Renamed by owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed by owner"));
        mvc.perform(put("/api/properties/" + house.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body("Renamed by admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed by admin"));

        mvc.perform(delete("/api/properties/" + house.getId()).header("Authorization", bearer(agentB)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/properties/" + house.getId())).andExpect(status().isNotFound());
    }
}
