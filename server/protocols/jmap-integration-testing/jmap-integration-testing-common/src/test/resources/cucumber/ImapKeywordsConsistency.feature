Feature: Impact of IMAP on JMAP keywords consistency

  Background:
    Given a domain named "domain.tld"
    And a connected user "username@domain.tld"
    And "username@domain.tld" has a mailbox "source"
    And "username@domain.tld" has a mailbox "mailbox"
    And "username@domain.tld" has a mailbox "trash"

  Scenario Outline: GetMessages should union keywords when an inconsistency was created via IMAP
    Given the user has a message "m1" in "source" mailbox with subject "My awesome subject", content "This is the content"
    Given the user copy "m1" from mailbox "source" to mailbox "<mailbox>"
    Given the user has an open IMAP connection with mailbox "<mailbox>" selected
    Given the user set flags via IMAP to "(\Flagged)" for all messages in mailbox "<mailbox>"
    When the user ask for messages "m1"
    Then no error is returned
    And the list should contain 1 message
    And the id of the message is "m1"
    And the keywords of the message is <keyword>

  Examples:
  |keyword                 | mailbox |
  |$Flagged                | mailbox |
  |$Flagged                | source  |
