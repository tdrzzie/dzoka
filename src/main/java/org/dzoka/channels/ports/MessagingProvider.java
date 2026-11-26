package org.dzoka.channels.ports;

import java.util.UUID;

public interface MessagingProvider {
    enum Template { HELP, REPORT_SAVED, RELAY_UPDATE, RECOVERY_UPDATE, UNSUPPORTED, MATCH_ALERT, SESSION_REPLY }
    enum Result { SENT, FAILED, UNKNOWN, RETRY }
    record Submission(Result result,String providerId) {}
    Submission submit(UUID operation,String address,Template template,String reply);
    byte[] download(String id,String type,String sha256);
}
