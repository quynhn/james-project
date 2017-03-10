package org.apache.james.webadmin.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.List;


public class DomainMappingRequest {

    private final String realDomain;
    private final List<String> aliases;

    @JsonCreator
    public DomainMappingRequest(@JsonProperty("realDomain") String realDomain, @JsonProperty("aliases") List<String> mappings) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(realDomain));

        Preconditions.checkNotNull(mappings);
        Preconditions.checkArgument(mappings.size() > 0);

        this.realDomain = realDomain;
        this.aliases = mappings;
    }

    public String getRealDomain() {
        return realDomain;
    }

    public List<String> getAliases() {
        return aliases;
    }
}
