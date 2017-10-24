#***************************************************************
# Licensed to the Apache Software Foundation (ASF) under one   *
# or more contributor license agreements.  See the NOTICE file *
# distributed with this work for additional information        *
# regarding copyright ownership.  The ASF licenses this file   *
# to you under the Apache License, Version 2.0 (the            *
# "License"); you may not use this file except in compliance   *
# with the License.  You may obtain a copy of the License at   *
#                                                              *
#   http://www.apache.org/licenses/LICENSE-2.0                 *
#                                                              *
# Unless required by applicable law or agreed to in writing,   *
# software distributed under the License is distributed on an  *
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
# KIND, either express or implied.  See the License for the    *
# specific language governing permissions and limitations      *
# under the License.                                           *
# **************************************************************/

Feature: Keywords consistency on delegation mailbox

  Background:
    Given a domain named "domain.tld"
    And a user "alice@domain.tld"
    And a user "bob@domain.tld"
    And "alice@domain.tld" has a mailbox "notShared"
    And "alice@domain.tld" has a mailbox "shared"
    And "alice@domain.tld" shares its mailbox "shared" with rights "lrw" with "bob@domain.tld"
    And "alice@domain.tld" is connected
    And "alice@domain.tld" has a message "m1" in "notShared" mailbox with subject "My awesome subject", content "This is the content"
    And the user copy "m1" from mailbox "notShared" to mailbox "shared"

  Scenario: getMessageList filtered by flag should combine flag when delegation mailbox
    Given "bob@domain.tld" is connected
    And the user set flags on "m1" to "$Flagged"
    When "alice@domain.tld" is connected
    And the user asks for message list in mailboxes "shared,notShared" with flag "$Flagged"
    Then the message list contains "m1"

  Scenario: getMessageList filtered by flag should keep flag on delegation mailbox
    Given "bob@domain.tld" is connected
    And the user set flags on "m1" to "$Flagged"
    When "alice@domain.tld" is connected
    And the user asks for message list in mailboxes "notShared" with flag "$Flagged"
    Then the message list is empty

  Scenario: getMessageList filtered by flag should keep flag on non-shared mailbox
    Given "bob@domain.tld" is connected
    And the user set flags on "m1" to "$Flagged"
    When "alice@domain.tld" is connected
    And the user asks for message list in mailboxes "shared" with flag "$Flagged"
    Then the message list contains "m1"

  # It does not work for now such as we search by flag first the we filter by inMailbox.
  # We do not know where can we combine/intersect flag
  @Ignore
  Scenario: Get message list should insection Draft flag on all mailbox
    Given "bob@domain.tld" is connected
    And message "m1" has flags $Draft in mailbox "shared" of user "alice@domain.tld"
    When "alice@domain.tld" is connected
    And the user asks for message list in mailboxes "shared,notShared" with flag "$Draft"
    Then the message list is empty

  Scenario: Get message list should keep flags on non-shared mailbox
    Given "bob@domain.tld" is connected
    And message "m1" has flags $Draft in mailbox "shared" of user "alice@domain.tld"
    When "alice@domain.tld" is connected
    And the user asks for message list in mailbox "notShared" with flag "$Draft"
    Then the message list is empty

  Scenario: Get message list should keep flags on shared mailbox
    Given "bob@domain.tld" is connected
    And message "m1" has flags $Draft in mailbox "shared" of user "alice@domain.tld"
    When "alice@domain.tld" is connected
    And the user asks for message list in mailbox "shared" with flag "$Draft"
    Then the message list contains "m1"

  Scenario: getMessage with shared user should return message with combine flag when delegation mailbox
    Given "alice@domain.tld" is connected
    And message "m1" has flags $Flagged in mailbox "shared" of user "alice@domain.tld"
    When "bob@domain.tld" is connected
    And "bob@domain.tld" ask for messages "m1"
    Then no error is returned
    And the keywords of the message is $Flagged

  Scenario: getMessage of owner mailbox should return message with combine flag when delegation mailbox
    Given "alice@domain.tld" is connected
    And message "m1" has flags $Flagged in mailbox "shared" of user "alice@domain.tld"
    When "alice@domain.tld" ask for messages "m1"
    Then no error is returned
    And the keywords of the message is $Flagged

  Scenario: getMessage should keep origin message status when cut the sharing
    Given "alice@domain.tld" is connected
    And message "m1" has flags $Flagged in mailbox "shared" of user "alice@domain.tld"
    And "alice@domain.tld" shares its mailbox "shared" with rights "" with "bob@domain.tld"
    When "bob@domain.tld" is connected
    And "bob@domain.tld" ask for messages "m1"
    Then no error is returned
    And the message has no keywords

  Scenario: message should update message status based on delegation mailbox
    Given "alice@domain.tld" is connected
    And the user set flags on "m1" to "$Flagged,$Seen"
    And "bob@domain.tld" is connected
    And the user set flags on "m1" to "$Seen"
    When "alice@domain.tld" is connected
    And "alice@domain.tld" ask for messages "m1"
    Then no error is returned
    And the message has IMAP flag "\Flagged \Seen" in mailbox "notShared"
    And the message has IMAP flag "\Seen" in mailbox "shared"

  Scenario: message should keep origin message status when cut the sharing
    And "bob@domain.tld" is connected
    And the user set flags on "m1" to "$Flagged"
    And "alice@domain.tld" shares its mailbox "shared" with rights "" with "bob@domain.tld"
    When "alice@domain.tld" is connected
    And "alice@domain.tld" ask for messages "m1"
    Then no error is returned
    And the message has IMAP flag "\Flagged" in mailbox "shared"
    And the message has IMAP flag "" in mailbox "notShared"

  Scenario: getMessage should keep origin message status when delegation mailbox
    Given "alice@domain.tld" is connected
    And message "m1" has flags $Flagged in mailbox "notShared" of user "alice@domain.tld"
    And "bob@domain.tld" is connected
    And message "m1" has flags $Seen in mailbox "shared" of user "alice@domain.tld"
    And "alice@domain.tld" shares its mailbox "shared" with rights "" with "bob@domain.tld"
    When "alice@domain.tld" is connected
    And "alice@domain.tld" ask for messages "m1"
    Then no error is returned
    And the keywords of the message is $Flagged,$Seen

  Scenario: getMessage on mailbox should keep its flag as it is when owner
      And "alice@domain.tld" is connected
      And the user set flags on "m1" to "$Flagged"
      When "alice@domain.tld" ask for messages "m1"
      Then no error is returned
      And the message has IMAP flag "\Flagged" in mailbox "shared"
      And the message has IMAP flag "\Flagged" in mailbox "notShared"

  Scenario: messages should keep Draft flag as it is when onwer
    Given "bob@domain.tld" is connected
    And message "m1" has flags $Draft in mailbox "shared" of user "alice@domain.tld"
    When "bob@domain.tld" is connected
    And "bob@domain.tld" ask for messages "m1"
    Then no error is returned
    And the keywords of the message is $Draft

  Scenario: message should intesect flag when Draft
    Given "bob@domain.tld" is connected
    And message "m1" has flags $Draft in mailbox "shared" of user "alice@domain.tld"
    When "alice@domain.tld" is connected
    And "alice@domain.tld" ask for messages "m1"
    Then no error is returned
    And the message has no keywords

  Scenario: message should intesect flag when Draft after cut sharing
    Given "bob@domain.tld" is connected
    And message "m1" has flags $Draft in mailbox "shared" of user "alice@domain.tld"
    And "alice@domain.tld" shares its mailbox "shared" with rights "" with "bob@domain.tld"
    When "alice@domain.tld" is connected
    And "alice@domain.tld" ask for messages "m1"
    Then no error is returned
    And the message has no keywords

  Scenario: message should combine flag if not Draft
    Given "alice@domain.tld" is connected
    And the user has an open IMAP connection with mailbox "shared" selected
    And the user set flags via IMAP to "\FLAGGED" for all messages in mailbox "shared"
    When "alice@domain.tld" ask for messages "m1"
    Then no error is returned
    And the keywords of the message is $Flagged

  Scenario: message should combine flag if not Draft on all mailboxes
    Given "alice@domain.tld" is connected
    And the user set flags on "m1" to "$Flagged"
    When "alice@domain.tld" ask for messages "m1"
    Then no error is returned
    And the keywords of the message is $Flagged

  Scenario: message should intesect Draft flag with onwer mailbox
    Given "alice@domain.tld" is connected
    And the user has an open IMAP connection with mailbox "shared" selected
    And the user set flags via IMAP to "\DRAFT" for all messages in mailbox "shared"
    When "alice@domain.tld" ask for messages "m1"
    Then no error is returned
    And the message has no keywords
