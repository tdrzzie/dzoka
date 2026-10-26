package org.dzoka.intake.domain;
import java.util.UUID;
public record ReportReceipt(UUID caseId, UUID itemId, UUID reportId, int revision) {}
