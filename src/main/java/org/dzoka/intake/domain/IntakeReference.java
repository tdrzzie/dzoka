package org.dzoka.intake.domain;
import java.util.UUID;
public record IntakeReference(UUID caseId,UUID itemId,UUID reportId,String caseState,String identityState){}
