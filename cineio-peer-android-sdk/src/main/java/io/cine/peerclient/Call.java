package io.cine.peerclient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by thomas on 1/13/15.
 */
public class Call {

    private final HashMap<String, Participant> participants;
    private final SignalingConnection signalingConnection;
    private final String room;
    private STATE state;

    public static enum STATE {INITIATED, IN_CALL, state, ENDED};

    public Call(String room, SignalingConnection signalingConnection, boolean initiated){
        this.room = room;
        this.signalingConnection = signalingConnection;
        if (initiated){
            this.state = STATE.IN_CALL;
        } else{
            this.state = STATE.INITIATED;
        }
        this.participants = new HashMap<String, Participant>();
    }

    public boolean isInCall(){
        return this.state == STATE.IN_CALL;
    }

    public boolean isEnded(){
        return this.state == STATE.ENDED;
    }

    public void answer(){
        this.state = STATE.IN_CALL;
        signalingConnection.joinRoom(this.room);
    }

    public void reject(){
        this.state = STATE.ENDED;
        signalingConnection.rejectCall(this.room);
    }

    public void hangup(){
        this.state = STATE.ENDED;
        signalingConnection.leaveRoom(this.room);
        cancelOutgoingCalls();
    }

    private void cancelOutgoingCalls() {
        Iterator it = participants.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            Participant p = (Participant) pairs.getValue();
            p.cancel();
        }
    }

    public void invite(String otherIdentity){
        Participant participant = this.createOrFindParticipant(otherIdentity);
        participant.call();

    }

    public void cancel(String otherIdentity){
        Participant participant = this.participants.get(otherIdentity);
        if(participant != null){
            participant.cancel();
        }
    }

    public void left(String otherIdentity){
        Participant participant = this.participants.get(otherIdentity);
        if(participant != null){
            participant.left();
        }
    }

    public void joined(String otherIdentity){
        Participant participant = this.createOrFindParticipant(otherIdentity);
        participant.joined();
    }

    private Participant createOrFindParticipant(String otherIdentity) {
        Participant participant = this.participants.get(otherIdentity);
        if(participant != null){return participant;}
        participant = new Participant(otherIdentity, this.room, this.signalingConnection);
        this.participants.put(otherIdentity, participant);
        return participant;
    }

    private class Participant{
        private final String otherIdentity;
        private final String room;
        private final SignalingConnection signalingConnection;
        private STATE state;

        public Participant(String otherIdentity, String room, SignalingConnection signalingConnection){
            this.otherIdentity = otherIdentity;
            this.room = room;
            this.state = STATE.INITIATED;
            this.signalingConnection = signalingConnection;
        }

        public void call(){
            this.state = STATE.IN_CALL;
            signalingConnection.call(this.otherIdentity, this.room);
        }

        public void cancel(){
            this.state = STATE.ENDED;
            signalingConnection.callCancel(this.otherIdentity, this.room);
        }

        public void left(){
            this.state = STATE.ENDED;
        }
        public void joined(){
            this.state = STATE.IN_CALL;
        }
    }

}
