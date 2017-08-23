/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.jmap.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.mail.Flags;

import org.apache.james.mailbox.FlagsBuilder;

import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class Keywords {
    public static final Keywords DEFAULT_VALUE = factory().fromSet(ImmutableSet.of());

    public interface KeywordsValidator {
        void validate(Set<Keyword> keywords);
    }

    private FlagsBuilder combiner(FlagsBuilder firstBuilder, FlagsBuilder secondBuilder) {
        return firstBuilder.add(secondBuilder.build());
    }

    private FlagsBuilder accumulator(FlagsBuilder accumulator, Keyword keyword) {
        return accumulator.add(keyword.asFlags());
    }

    public static class KeywordsFactory {
        private Optional<KeywordsValidator> validator;
        private Optional<Predicate<Keyword>> filter;

        private KeywordsFactory() {
            validator = Optional.empty();
            filter = Optional.empty();
        }

        public KeywordsFactory throwOnImapNonExposedKeywords() {
            validator = Optional.of(keywords -> Preconditions.checkArgument(
                keywords.stream().allMatch(Keyword::isNotNonExposedImapKeyword), "Does not allow to update 'Deleted' or 'Recent' flag"));
            return this;
        }

        public KeywordsFactory filterImapNonExposedKeywords() {
            filter = Optional.of(keyword -> keyword.isNotNonExposedImapKeyword());
            return this;
        }

        public Keywords fromSet(Set<Keyword> setKeywords) {
            validator.orElse(keywords -> {})
                .validate(setKeywords);

            return new Keywords(setKeywords.stream()
                .filter(filter.orElse(keyword -> true))
                .collect(Guavate.toImmutableSet()));
        }

        public Keywords fromList(List<String> keywords) {
            return fromSet(keywords.stream()
                .map(Keyword::new)
                .collect(Guavate.toImmutableSet()));
        }

        @VisibleForTesting
        Keywords fromMap(Map<String, Boolean> mapKeywords) {
            Preconditions.checkArgument(mapKeywords.values()
                .stream()
                .noneMatch(keywordValue -> keywordValue == false), "Keyword must be true");
            Set<Keyword> setKeywords = mapKeywords.keySet()
                .stream()
                .map(Keyword::new)
                .collect(Guavate.toImmutableSet());

            return fromSet(setKeywords);
        }

        @VisibleForTesting
        Keywords fromOldKeyword(OldKeyword oldKeyword) {
            ImmutableSet.Builder<Keyword> builder = ImmutableSet.builder();
            if (oldKeyword.isAnswered().orElse(false)) {
                builder.add(Keyword.ANSWERED);
            }
            if (oldKeyword.isDraft().orElse(false)) {
                builder.add(Keyword.DRAFT);
            }
            if (oldKeyword.isFlagged().orElse(false)) {
                builder.add(Keyword.FLAGGED);
            }
            if (oldKeyword.isUnread().isPresent() && oldKeyword.isUnread().get() == false) {
                builder.add(Keyword.SEEN);
            }
            return fromSet(builder.build());
        }

        public Keywords fromFlags(Flags flags) {
            return fromSet(Stream.concat(
                    Stream.of(flags.getUserFlags())
                        .map(Keyword::new),
                    Stream.of(flags.getSystemFlags())
                        .map(Keyword::fromFlag))
                .collect(Guavate.toImmutableSet()));
        }

        public Optional<Keywords> fromMapOrOldKeyword(Optional<Map<String, Boolean>> mapKeyword, Optional<OldKeyword> oldKeyword) {
            Preconditions.checkArgument(!(mapKeyword.isPresent() && oldKeyword.isPresent()), "Does not support keyword and is* at the same time");

            Keywords keywords = mapKeyword.map(this::fromMap)
                .orElse(oldKeyword.map(this::fromOldKeyword)
                    .orElse(null));
            return Optional.ofNullable(keywords);
        }
    }

    public static KeywordsFactory factory() {
        return new KeywordsFactory();
    }

    private final ImmutableSet<Keyword> keywords;

    private Keywords(ImmutableSet<Keyword> keywords) {
        this.keywords = keywords;
    }

    public Flags asFlags() {
        return keywords.stream()
            .reduce(FlagsBuilder.builder(), this::accumulator, this::combiner)
            .build();
    }

    public Flags asFlagsWithRecentAndDeletedFrom(Flags originFlags) {
        Flags flags = asFlags();
        if (originFlags.contains(Flags.Flag.DELETED)) {
            flags.add(Flags.Flag.DELETED);
        }
        if (originFlags.contains(Flags.Flag.RECENT)) {
            flags.add(Flags.Flag.RECENT);
        }

        return flags;
    }

    public ImmutableMap<String, Boolean> asMap() {
        return keywords.stream()
            .collect(Guavate.toImmutableMap(Keyword::getFlagName, keyword -> Keyword.FLAG_VALUE));
    }

    public ImmutableSet<Keyword> getKeywords() {
        return keywords;
    }

    public boolean contains(Keyword keyword) {
        return keywords.contains(keyword);
    }

    @Override
    public final boolean equals(Object other) {
        if (other instanceof Keywords) {
            Keywords otherKeyword = (Keywords) other;
            return Objects.equal(keywords, otherKeyword.keywords);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(keywords);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("keywords", keywords)
            .toString();
    }

}
