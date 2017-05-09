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

package org.apache.james.mailbox.inmemory;

import static org.assertj.core.api.Assertions.assertThat;
import org.apache.james.mailbox.extractor.TextExtractor;

import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

public class JsoupTextExtractorTest {
    private TextExtractor textExtractor;

    @Before
    public void setUp() {
        textExtractor = new JsoupTextExtractor();
    }

    @Test
    public void HtmlTextTest() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("documents/html.txt");
        assertThat(inputStream).isNotNull();
        assertThat(textExtractor.extractContent(inputStream, "text/html", null).getTextualContent())
                .isEqualTo("Afficher l'email dans le navigateur. Hackathon sur les technologies Open Source avec Passerelles Numériques à Danang LINAGORA est fière d'être partenaire de Passerelles Numériques pour animer un Hackathon avec des jeunes étudiants dynamiques et prometteurs. Du lundi 20 mars au 31 mars, l'équipe LINAGORA Vietnam, et en particulier notre expert OpenPaaS Benoît Tellier, effectue un Hackathon sur les technologies Open Source et l'environnement Linux avec une vingtaine d'étudiants vietnamiens évoluant au sein de l'ONG d'éducation Passerelles Numérique à Danang. Cette intervention se fait dans le cadre de la coopération stratégique et technologique entre la ville de Danang et LINAGORA Vietnam. Depuis 2015, les deux entités coopèrent et collaborent pour le développement de solutions d'e-Gouvernance basées sur des technologies Open Source. Fin 2016, les logiciels OBM (messagerie collaborative) et LinShare (partage de fichiers) de LINAGORA ont été déployé pour 20 000 administrés de la ville Danang. Ce Hackathon unique en son genre s'est donné trois objectifs : Présenter d'une part le logiciel phare que nous développons : OpenPaaS (plate-forme de travail collaboratif de nouvelle génération), Présenter d'autre part le projet James (mail serveur) de la fondation Apache, pour lequel nous contribuons et faire contribuer les étudiants sur ces deux logiciels. Enfin, faire découvrir le métier de développeur dans une entreprise internationale. Ce programme s'organise autour de diverses formations, de travaux pratiques (10 heures par semaine) autour des thèmes de James et d'OpenPaaS : écrire des tests, écrire du code lisible, utiliser Java 8, utiliser JPA, créer une interface REST, etc. La «méthode agile» est utilisé et mise en avant pour structurer le travail, pratiquer du «pair programming» ainsi que de la revue de code, installer la plate-forme OpenPaaS sur leurs ordinateurs, etc. À travers cette expérience, les étudiants peuvent découvrir le monde Open Source et les méthodes et exigences de développeurs professionnels.   Benoît Tellier, expert OpenPaaS et des étudiants suivant la formation. Au terme de ces 15 jours de formation, LINAGORA est à la fois fière d'être partenaire de Passerelles Numériques et surtout d'animer ce Hackathon avec des étudiants si dynamiques et prometteurs. Passerelles Numériques est une formidable association qui fait un excellent travail auprès des plus jeunes. Elle a un vrai impact sur la société grâce à son travail et ses valeurs. Ce partenariat et cette initiative vont dans le sens de notre objectif principal de LINAGORA Vietnam : contribuer à l'indépendance numérique du Vietnam via la fourniture des solutions d'e-Gouvernance Open Source aux administrations vietnamiennes et le développement des programmes de formation de qualité sur l'informatique et les technologies Open Source. Share Tweet Share En savoir plus sur Passerelles Numériques : Passerelles Numériques est une ONG française. Ils opèrent au Cambodge, aux Philippines et au Vietnam. Leur mission est de permettre à des jeunes très défavorisés d’accéder à une éducation et à une formation technique et professionnelle, dans le secteur du numérique. Cela leur permettra d’échapper durablement à la pauvreté, et de contribuer au développement socio-économique de leur pays. Contact presse : Robin Ledru : communication@linagora.com et 06 33 87 88 64 Suivez nous sur nos différents réseaux sociaux Copyright © 2017 *|LIST:COMPANY|*, All rights reserved. *|LIST:DESCRIPTION|* Our mailing address is: *|HTML:LIST_ADDRESS_HTML|* Want to change how you receive these emails? You can update your preferences or unsubscribe from this list");
    }

}