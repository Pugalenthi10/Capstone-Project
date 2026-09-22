package com.trichyestates.estatehub.controller;

import com.trichyestates.estatehub.config.AppProperties;
import com.trichyestates.estatehub.dto.ContactInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, non-secret settings the UI needs (currently just the property-desk contact numbers). */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final AppProperties props;

    public ConfigController(AppProperties props) {
        this.props = props;
    }

    @GetMapping("/contact")
    public ContactInfoResponse contact() {
        return new ContactInfoResponse(props.contact().phone(), props.contact().whatsapp());
    }
}
