package org.dzoka.intake.application;
import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.domain.*;
import org.dzoka.intake.ports.Reports;
import org.springframework.stereotype.Service;
@Service
public final class IntakeService {
    private final Reports reports;
    public IntakeService(Reports reports){this.reports=reports;}
    public ReportReceipt submit(VerifiedIdentity actor,UUID tenant,UUID report,Integer expectedRevision,ReportDraft draft,byte[] raw,String key){
        if(key==null || !key.matches("[A-Za-z0-9._:-]{8,128}") || raw.length>16384
                || (report!=null && (expectedRevision==null || expectedRevision<1)))throw new IllegalArgumentException("Invalid intake command");
        return reports.submit(actor,tenant,report,expectedRevision,draft,raw,key);
    }
    public ReportView get(VerifiedIdentity actor,UUID tenant,UUID report,Integer revision){return reports.get(actor,tenant,report,revision);}
    public IntakeReference reference(VerifiedIdentity actor,UUID tenant,UUID id,boolean caseLookup){return reports.reference(actor,tenant,id,caseLookup);}
    public void delete(VerifiedIdentity actor,UUID tenant,UUID report){reports.delete(actor,tenant,report);}
    public void hold(VerifiedIdentity actor,UUID tenant,UUID report,boolean held){reports.hold(actor,tenant,report,held);}
    public int expire(VerifiedIdentity actor,UUID tenant){return reports.expire(actor,tenant);}
}
