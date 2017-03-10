package org.apache.james.webadmin.dto;

import java.util.Collection;

/**
 * Created by sangnd on 3/10/17.
 */
public class DomainMappingResponse {
    private final String email;
    private final Collection<String> mappings;

    public DomainMappingResponse(String email, Collection<String> mappings) {
        this.email = email;
        this.mappings = mappings;
    }

    public String getEmail() {
        return email;
    }

    public Collection<String> getMappings() {
        return mappings;
    }
}
