package org.dzoka.channels.adapters;

import java.util.List;
import java.util.UUID;
import org.dzoka.foundation.application.Failures;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.recovery.domain.Recovery.Capacity;
import org.dzoka.recovery.ports.RecoveryOperations;
import org.springframework.stereotype.Component;

@Component
final class ChannelConversation {
    private final ChannelMatching matches;
    private final RecoveryOperations recovery;

    ChannelConversation(ChannelMatching matches,RecoveryOperations recovery) {
        this.matches=matches;
        this.recovery=recovery;
    }

    boolean handles(String text) {
        return text!=null && List.of("SEARCH","REVIEW","CLAIM","CHALLENGE","ANSWER","PICKUP","CONFIRM")
                .contains(text.split("\\s+",2)[0]);
    }

    String reply(VerifiedIdentity actor,UUID tenant,UUID contact,UUID report,UUID operation,String quoteKey,String text) {
        String[] args=text.split("\\s+",5);
        return switch(args[0]) {
            case "SEARCH" -> search(actor,tenant,contact,report,operation,quoteKey);
            case "REVIEW" -> {
                selected(actor,tenant,contact,report,args);
                yield "Candidate "+args[2]+" has a similar description from the recovery desk."
                        +" Reply to your report with CLAIM "+args[1]+" "+args[2]+" to request an ownership check.";
            }
            case "CLAIM" -> {
                var candidate=selected(actor,tenant,contact,report,args);
                var intake=candidate.intake();
                var claim=recovery.submitMatch(actor,tenant,intake,operation,Capacity.OWNER,candidate.report(),candidate.revision());
                yield "Claim "+claim.state()+". Send CHALLENGE "+intake+" "+claim.id()+".";
            }
            case "CHALLENGE" -> {
                requireLength(args,3);
                var challenge=recovery.challenge(actor,tenant,id(args[1]),id(args[2]),operation);
                yield challenge.id()==null?"No ownership question is available. Contact the recovery desk."
                        :challenge.prompt()+" Send ANSWER "+args[1]+" "+args[2]+" "+challenge.id()
                        +" followed by your answer. Never send passwords, PINs or financial details.";
            }
            case "ANSWER" -> {
                requireLength(args,5);
                recovery.answer(actor,tenant,id(args[1]),id(args[2]),id(args[3]),args[4]);
                yield "Your answer was recorded for independent review. Send PICKUP "+args[1]+" "+args[2]+" for status.";
            }
            case "PICKUP","CONFIRM" -> {
                requireLength(args,3);
                String status=recovery.pickupMessage(actor,tenant,id(args[1]),id(args[2]),args[0].equals("CONFIRM"));
                yield switch(status) {
                    case "RETURNED" -> "The recovery desk confirmed this return.";
                    case "ACKNOWLEDGED" -> "Your confirmation of the recorded return was saved.";
                    case "CONTACT_DESK" -> "Your pickup request was recorded. Staff will check your authorization in person.";
                    case "PENDING" -> "Your claim is waiting for review.";
                    default -> "Pickup is unavailable for this claim.";
                };
            }
            default -> throw new IllegalArgumentException("Unknown command");
        };
    }

    private String search(VerifiedIdentity actor,UUID tenant,UUID contact,UUID report,UUID operation,String quoteKey) {
        if(report==null) return "Reply to your original lost or found report with SEARCH.";
        var result=matches.record(actor,tenant,contact,report,operation,quoteKey);
        int count=result.candidates().size();
        if(count==0) return "No candidate with comparable evidence is available.";
        return count+" possible "+(count==1?"candidate":"candidates")
                +". Reply to your original report with REVIEW "+operation+" 1. Candidate numbers run from 1 to "+count+".";
    }

    private ChannelMatching.Candidate selected(VerifiedIdentity actor,UUID tenant,UUID contact,UUID report,String[] args) {
        requireLength(args,3);
        if(report==null || !args[2].matches("[1-5]")) throw new IllegalArgumentException("Invalid candidate");
        int index=Integer.parseInt(args[2])-1;
        return matches.choose(actor,tenant,contact,report,id(args[1]),index);
    }

    private static UUID id(String value) {
        if(!value.matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException("Invalid conversation reference");
        return UUID.fromString(value);
    }

    private static void requireLength(String[] args,int expected) {
        if(args.length!=expected) throw new IllegalArgumentException("Invalid command format");
    }

}
