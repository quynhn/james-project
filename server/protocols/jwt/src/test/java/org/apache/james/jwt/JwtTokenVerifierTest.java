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
package org.apache.james.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.Security;
import java.util.Optional;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.jsonwebtoken.ClaimJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;

public class JwtTokenVerifierTest {

    private static final String PUBLIC_PEM_KEY = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtlChO/nlVP27MpdkG0Bh\n" +
            "16XrMRf6M4NeyGa7j5+1UKm42IKUf3lM28oe82MqIIRyvskPc11NuzSor8HmvH8H\n" +
            "lhDs5DyJtx2qp35AT0zCqfwlaDnlDc/QDlZv1CoRZGpQk1Inyh6SbZwYpxxwh0fi\n" +
            "+d/4RpE3LBVo8wgOaXPylOlHxsDizfkL8QwXItyakBfMO6jWQRrj7/9WDhGf4Hi+\n" +
            "GQur1tPGZDl9mvCoRHjFrD5M/yypIPlfMGWFVEvV5jClNMLAQ9bYFuOc7H1fEWw6\n" +
            "U1LZUUbJW9/CH45YXz82CYqkrfbnQxqRb2iVbVjs/sHopHd1NTiCfUtwvcYJiBVj\n" +
            "kwIDAQAB\n" +
            "-----END PUBLIC KEY-----";
    
    private static final String VALID_TOKEN_WITHOUT_ADMIN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIn0.T04BTk" +
            "LXkJj24coSZkK13RfG25lpvmSl2MJ7N10KpBk9_-95EGYZdog-BDAn3PJzqVw52z-Bwjh4VOj1-j7cURu0cT4jXehhUrlCxS4n7QHZD" +
            "N_bsEYGu7KzjWTpTsUiHe-rN7izXVFxDGG1TGwlmBCBnPW-EFCf9ylUsJi0r2BKNdaaPRfMIrHptH1zJBkkUziWpBN1RNLjmvlAUf49" +
            "t1Tbv21ZqYM5Ht2vrhJWczFbuC-TD-8zJkXhjTmA1GVgomIX5dx1cH-dZX1wANNmshUJGHgepWlPU-5VIYxPEhb219RMLJIELMY2qN" +
            "OR8Q31ydinyqzXvCSzVJOf6T60-w";

    private static final String VALID_TOKEN_ADMIN_TRUE = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbkBvcGVuL" +
        "XBhYXMub3JnIiwiYWRtaW4iOnRydWUsImlhdCI6MTQ4OTAzODQzOH0.rgxCkdWEa-92a4R-72a9Z49k4LRvQDShgci5Y7qWRUP9IGJCK-lMkrHF" +
        "4H0a6L87BYppxVW701zaZ6dNxRMvHnjLBBWnPsC2B0rkkr2hEL2zfz7sb-iNGV-J4ICx97t8-TfQ5rz3VOX0FwdusPL_rJtmlGEGRivPkR6_aBe1" +
        "kQnvMlwpqF_3ox58EUqYJk6lK_6rjKEV3Xfre31IMpuQUy6c7TKc95sL2-13cknelTierBEmZ00RzTtv9SHIEfzZTfaUK2Wm0PvnQjmU2nIdEvU" +
        "EqE-jrM3yYXcQzoO-YTQnEhdl-iqbCfmEpYkl2Bx3eIq7gRxxnr7BPsX6HrCB0w";
    private static final String VALID_TOKEN_ADMIN_FALSE = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbkBvcGVu" +
        "LXBhYXMub3JnIiwiYWRtaW4iOmZhbHNlLCJpYXQiOjE0ODkwNDA4Njd9.reQc3DiVvbQHF08oW1qOUyDJyv3tfzDNk8jhVZequiCdOI9vXnRlOe" +
        "-yDYktd4WT8MYhqY7MgS-wR0vO9jZFv8ZCgd_MkKCvCO0HmMjP5iQPZ0kqGkgWUH7X123tfR38MfbCVAdPDba-K3MfkogV1xvDhlkPScFr_6MxE" +
        "xtedOK2JnQZn7t9sUzSrcyjWverm7gZkPptkIVoS8TsEeMMME5vFXe_nqkEG69q3kuBUm_33tbR5oNS0ZGZKlG9r41lHBjyf9J1xN4UYV8n866d" +
        "a7RPPCzshIWUtO0q9T2umWTnp-6OnOdBCkndrZmRR6pPxsD5YL0_77Wq8KT_5__fGA";
    private JwtTokenVerifier sut;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Before
    public void setup() {
        PublicKeyProvider pubKeyProvider = new PublicKeyProvider(getJWTConfiguration(), new PublicKeyReader());
        sut = new JwtTokenVerifier(pubKeyProvider);
    }

    private JwtConfiguration getJWTConfiguration() {
        return new JwtConfiguration(Optional.of(PUBLIC_PEM_KEY));
    }

    @Test
    public void shouldReturnTrueOnValidSignature() {
        assertThat(sut.verify(VALID_TOKEN_WITHOUT_ADMIN)).isTrue();
    }

    @Test
    public void shouldThrowOnMismatchingSigningKey() {
        String invalidToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIn0.Pd6t82" +
                "tPL3EZdkeYxw_DV2KimE1U2FvuLHmfR_mimJ5US3JFU4J2Gd94O7rwpSTGN1B9h-_lsTebo4ua4xHsTtmczZ9xa8a_kWKaSkqFjNFa" +
                "Fp6zcoD6ivCu03SlRqsQzSRHXo6TKbnqOt9D6Y2rNa3C4igSwoS0jUE4BgpXbc0";

        assertThatThrownBy(() -> sut.verify(invalidToken))
            .isInstanceOf(SignatureException.class);
    }

    @Test
    public void verifyShouldThrowWhenSubjectIsNull() {
        String tokenWithNullSubject = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOm51bGwsIm5hbWUiOiJKb2huIERvZSJ9.EB" +
                "_1grWDy_kFelXs3AQeiP13ay4eG_134dWB9XPRSeWsuPs8Mz2UY-VHDxLGD-fAqv-xKXr4QFEnS7iZkdpe0tPLNSwIjqeqkC6KqQln" +
                "oC1okqWVWBDOcf7Acp1Jzp_cFTUhL5LkHvZDsyCdq5T9OOVVkzO4A9RrzIUsTrYPtRCBuYJ3ggR33cKpw191PulPGNH70rZqpUfDXe" +
                "VPY3q15vWzZH9O9IJzB2KdHRMPxl2luRjzDbh4DLp56NhZuLX_2a9UAlmbV8MQX4Z_04ybhAYrcBfxR3MgJyr0jlxSibqSbXrkXuo-" +
                "PyybfZCIhK_qXUlO5OS6sO7AQhKZO9p0MQ";

        assertThatThrownBy(() -> sut.verify(tokenWithNullSubject))
            .isInstanceOf(MalformedJwtException.class)
            .hasMessage("'subject' field in token is mandatory");
    }
    
    @Test
    public void verifyShouldThrowWhenEmptySubject() {
        String tokenWithEmptySubject = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIiLCJuYW1lIjoiSm9obiBEb2UifQ.UdY" +
                "s2PPzFCegUYspoDCnlJR_bJm8_z1InOv4v3tq8SJETQUarOXlhb_n6y6ujVvmJiSx9dI24Hc3Czi3RGUOXbnBDj1WPfd0aVSiUSqZr" +
                "MCHBt5vjCYqAseDaP3w4aiiFb6EV3tteJFeBLZx8XlKPYxlzRLLUADDyDSQvrFBBPxfsvCETZovKdo9ofIN64o-yx23ss63yE6oIOd" +
                "zJZ1Id40KSR2d7l3kIQJPLKUWJDnro5RAh4DOGOWNSq0JSbMhk7Zn3cXIBUpv3R8p79tui1UQpzwHMC0e6OSuWEDNQHtq-Cz85u8GG" +
                "sUSbogmgObA_BimNtUq_Q1p0SGtIYBXmQ";

        assertThatThrownBy(() -> sut.verify(tokenWithEmptySubject))
            .isInstanceOf(MalformedJwtException.class)
            .hasMessage("'subject' field in token is mandatory");
    }

    @Test
    public void shouldReturnUserLoginFromValidToken() {
        assertThat(sut.extractLogin(VALID_TOKEN_WITHOUT_ADMIN)).isEqualTo("1234567890");
    }

    @Test
    public void hasAttributeShouldNotThrowIfClaimValid() throws Exception {
        sut.hasAttribute("admin", true, VALID_TOKEN_ADMIN_TRUE);
    }

    @Test
    public void extractLoginShouldWorkWithAdminClaim() {
        assertThat(sut.extractLogin(VALID_TOKEN_ADMIN_TRUE)).isEqualTo("admin@open-paas.org");
    }

    @Test
    public void hasAttributeShouldThrowIfClaimInvalid() throws Exception {
        expectedException.expect(ClaimJwtException.class);

        sut.hasAttribute("admin", true, VALID_TOKEN_ADMIN_FALSE);
    }

}
