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

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;
import javax.mail.Flags;

import org.apache.james.mailbox.FlagsBuilder;

import org.apache.commons.lang3.StringUtils;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class KeywordTest {
    private final static String FORWARDED = "forwarded";
    private final static int FLAG_NAME_MAX_LENTH = 255;
    private final static String ANY_KEYWORD = "AnyKeyword";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldRespectBeanContract() {
        EqualsVerifier.forClass(Keyword.class).verify();
    }

    @Test
    public void keywordShouldThrowWhenFlagNameLengthLessThenMinLength() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("");
    }

    @Test
    public void keywordShouldThrowWhenFlagNameLengthMoreThenMaxLength() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword(StringUtils.repeat("a", FLAG_NAME_MAX_LENTH + 1));
    }

    @Test
    public void keywordShouldCreateNewOneWhenFlagNameLengthEqualMaxLength() throws Exception {
        assertThat(new Keyword(StringUtils.repeat("a", FLAG_NAME_MAX_LENTH))).isNotNull();
    }

    @Test
    public void keywordShouldCreateNewOneWhenFlagNameLengthEqualMinLength() throws Exception {
        assertThat(new Keyword("a")).isNotNull();
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainPercentageCharacter() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a%");
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainLeftBracket() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a[");
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainRightBracket() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a]");
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainLeftBrace() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a{");
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainSlash() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a\\");
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainStar() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a*");
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainQuote() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a\"");
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainOpeningParenthesis() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a(");
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainClosingParenthesis() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a)");
    }

    @Test
    public void keywordShouldThrowWhenFlagNameContainSpaceCharacter() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        new Keyword("a b");
    }

    @Test
    public void isSupportedShouldReturnFalseWhenDeleted() throws Exception {
        assertThat(Keyword.DELETED.isNonExposedImapKeyword()).isFalse();
    }

    @Test
    public void isSupportedShouldReturnFalseWhenRecent() throws Exception {
        assertThat(Keyword.RECENT.isNonExposedImapKeyword()).isFalse();
    }

    @Test
    public void isSupportedShouldReturnTrueWhenOtherSystemFlag() throws Exception {
        assertThat(Keyword.DRAFT.isNonExposedImapKeyword()).isTrue();
    }

    @Test
    public void isSupportedShouldReturnTrueWhenAnyUserFlag() throws Exception {
        Keyword keyword = new Keyword(ANY_KEYWORD);
        assertThat(keyword.isNonExposedImapKeyword()).isTrue();
    }

    @Test
    public void isDraftShouldReturnTrueWhenDraft() throws Exception {
        assertThat(Keyword.DRAFT.isDraft()).isTrue();
    }

    @Test
    public void isDraftShouldReturnFalseWhenNonDraft() throws Exception {
        assertThat(Keyword.DELETED.isDraft()).isFalse();
    }

    @Test
    public void asSystemFlagShouldReturnSystemFlag() throws Exception {
        assertThat(new Keyword("$Draft").asSystemFlag())
            .isEqualTo(Optional.of(Flags.Flag.DRAFT));
    }

    @Test
    public void asSystemFlagShouldReturnEmptyWhenNonSystemFlag() throws Exception {
        assertThat(new Keyword(ANY_KEYWORD).asSystemFlag().isPresent())
            .isFalse();
    }

    @Test
    public void asFlagsShouldReturnFlagsWhenSystemFlag() throws Exception {
        assertThat(Keyword.DELETED.asFlags())
            .isEqualTo(new Flags(Flags.Flag.DELETED));
    }

    @Test
    public void asFlagsShouldReturnFlagsWhenUserFlag() throws Exception {
        Keyword keyword = new Keyword(ANY_KEYWORD);
        assertThat(keyword.asFlags())
            .isEqualTo(new Flags(ANY_KEYWORD));
    }
}