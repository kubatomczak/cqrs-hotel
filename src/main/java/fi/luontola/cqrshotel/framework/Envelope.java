// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.framework;

import fi.luontola.cqrshotel.util.Struct;

import java.util.UUID;

public class Envelope<M extends Message> extends Struct {

    public final UUID messageId;
    public final UUID correlationId;
    public final UUID causationId;
    public final M payload;

    public static <M extends Message> Envelope<M> newMessage(M payload) {
        // TODO: detect correlation and causation IDs
        return original(payload);
    }

    private static <M extends Message> Envelope<M> original(M payload) {
        return new Envelope<>(UUID.randomUUID(), UUID.randomUUID(), null, payload);
    }

    private static <M extends Message> Envelope<M> caused(M payload, Envelope<?> cause) {
        return new Envelope<>(UUID.randomUUID(), cause.correlationId, cause.messageId, payload);
    }

    public Envelope(UUID messageId, UUID correlationId, UUID causationId, M payload) {
        this.messageId = messageId;
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.payload = payload;
    }
}
