package org.dzoka.intake.domain;

import java.text.Normalizer;
import java.time.*;

public record ReportDraft(Kind kind, String rawText, String language, String timezone,
        String rawLocation, String rawTime, Double latitude, Double longitude, Double uncertaintyMeters,
        Instant occurredAfter, Instant occurredBefore) {
    public enum Kind { LOST, FOUND }
    public ReportDraft {
        if(kind==null || language==null || !language.matches("[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*")
                || timezone==null) throw new IllegalArgumentException("Invalid report context");
        ZoneId.of(timezone);
        if(rawText!=null && (rawText.length()>8000 || rawText.indexOf('\0')>=0)) throw new IllegalArgumentException("Invalid text");
        for(String value:new String[]{rawLocation,rawTime})
            if(value!=null && (value.length()>1000 || value.indexOf('\0')>=0)) throw new IllegalArgumentException("Invalid raw context");
        if((latitude==null || longitude==null || uncertaintyMeters==null) && !(latitude==null && longitude==null && uncertaintyMeters==null))
            throw new IllegalArgumentException("Incomplete position");
        if(latitude!=null && (!Double.isFinite(latitude) || !Double.isFinite(longitude) || !Double.isFinite(uncertaintyMeters)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180 || uncertaintyMeters < 0 || uncertaintyMeters > 200000))
            throw new IllegalArgumentException("Invalid position");
        if(occurredAfter!=null && occurredBefore!=null && occurredAfter.isAfter(occurredBefore))
            throw new IllegalArgumentException("Invalid time interval");
    }
    public String normalizedText() {
        return rawText==null?null:Normalizer.normalize(rawText,Normalizer.Form.NFC).replaceAll("(?U)\\s+"," ").strip();
    }
}
