package com.trichyestates.estatehub.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable identifier of the original demo listings (a1..f10); null for listings created later. */
    @Column(name = "seed_code", unique = true, length = 20, updatable = false)
    private String seedCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PropertyCategory category;

    @Column(nullable = false, length = 150)
    private String location;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false)
    private Long price;

    @Column(name = "price_label", nullable = false, length = 50)
    private String priceLabel;

    @Column(nullable = false)
    private Integer bedrooms;

    @Column(nullable = false)
    private Integer bathrooms;

    @Column(nullable = false)
    private Integer area;

    @Column(nullable = false, length = 2000)
    private String description;

    /** Remote URL today; the column can later hold a storage/CDN URL when uploads are added. */
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false, length = 10)
    private ListingType listingType = ListingType.SALE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User agent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public String getSeedCode() { return seedCode; }
    public void setSeedCode(String seedCode) { this.seedCode = seedCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public PropertyCategory getCategory() { return category; }
    public void setCategory(PropertyCategory category) { this.category = category; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
    public String getPriceLabel() { return priceLabel; }
    public void setPriceLabel(String priceLabel) { this.priceLabel = priceLabel; }
    public Integer getBedrooms() { return bedrooms; }
    public void setBedrooms(Integer bedrooms) { this.bedrooms = bedrooms; }
    public Integer getBathrooms() { return bathrooms; }
    public void setBathrooms(Integer bathrooms) { this.bathrooms = bathrooms; }
    public Integer getArea() { return area; }
    public void setArea(Integer area) { this.area = area; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public ListingType getListingType() { return listingType; }
    public void setListingType(ListingType listingType) { this.listingType = listingType; }
    public User getAgent() { return agent; }
    public void setAgent(User agent) { this.agent = agent; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
