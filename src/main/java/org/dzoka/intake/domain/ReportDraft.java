package org.dzoka.intake.domain;

import java.text.Normalizer;
import java.time.*;

public record ReportDraft(Kind kind, String rawText, String language, String timezone,
        String rawLocation, String rawTime, Double latitude, Double longitude, Double uncertaintyMeters,
        Instant occurredAfter, Instant occurredBefore, String category, java.util.UUID placeId, Long placeVersion) {
    public enum Kind { LOST, FOUND }
    public ReportDraft(Kind kind,String rawText,String language,String timezone,String rawLocation,String rawTime,
                       Double latitude,Double longitude,Double uncertaintyMeters,Instant occurredAfter,Instant occurredBefore,String category) {
        this(kind,rawText,language,timezone,rawLocation,rawTime,latitude,longitude,uncertaintyMeters,occurredAfter,occurredBefore,category,null,null);
    }
    public ReportDraft(Kind kind,String rawText,String language,String timezone,String rawLocation,String rawTime,
                       Double latitude,Double longitude,Double uncertaintyMeters,Instant occurredAfter,Instant occurredBefore) {
        this(kind,rawText,language,timezone,rawLocation,rawTime,latitude,longitude,uncertaintyMeters,occurredAfter,occurredBefore,null);
    }
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
        if(category!=null && !category.matches("[A-Z][A-Z0-9_]{0,47}")) throw new IllegalArgumentException("Invalid category code");
        if((placeId==null)!=(placeVersion==null) || placeVersion!=null&&placeVersion<1)throw new IllegalArgumentException("Incomplete place reference");
    }
    public String normalizedText() {
        return rawText==null?null:Normalizer.normalize(rawText,Normalizer.Form.NFC).replaceAll("(?U)\\s+"," ").strip();
    }
}
