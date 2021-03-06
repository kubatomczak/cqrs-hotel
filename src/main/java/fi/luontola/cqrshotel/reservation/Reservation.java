// Copyright © 2016-2018 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.luontola.cqrshotel.reservation;

import fi.luontola.cqrshotel.framework.AggregateRoot;
import fi.luontola.cqrshotel.framework.eventstore.EventStore;
import fi.luontola.cqrshotel.framework.util.EventListener;
import fi.luontola.cqrshotel.hotel.Hotel;
import fi.luontola.cqrshotel.pricing.PricingEngine;
import fi.luontola.cqrshotel.reservation.events.ContactInformationUpdated;
import fi.luontola.cqrshotel.reservation.events.CustomerDiscovered;
import fi.luontola.cqrshotel.reservation.events.LineItemCreated;
import fi.luontola.cqrshotel.reservation.events.PriceOffered;
import fi.luontola.cqrshotel.reservation.events.ReservationInitiated;
import fi.luontola.cqrshotel.reservation.events.RoomAssigned;
import fi.luontola.cqrshotel.reservation.events.SearchedForAccommodation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static fi.luontola.cqrshotel.reservation.Reservation.State.INITIATED;
import static fi.luontola.cqrshotel.reservation.Reservation.State.PROSPECT;

public class Reservation extends AggregateRoot {

    public static final Duration PRICE_VALIDITY_DURATION = Duration.ofMinutes(30);

    enum State {PROSPECT, INITIATED}

    private State state;
    private final Map<LocalDate, PriceOffered> priceOffersByDate = new HashMap<>();
    private int lineItems = 0;

    @EventListener
    private void apply(CustomerDiscovered event) {
        state = PROSPECT;
    }

    @EventListener
    private void apply(PriceOffered event) {
        priceOffersByDate.put(event.date, event);
    }

    @EventListener
    private void apply(ReservationInitiated event) {
        state = INITIATED;
    }

    @EventListener
    private void apply(LineItemCreated event) {
        lineItems++;
    }

    public void discoverCustomer() {
        if (getVersion() == EventStore.BEGINNING) {
            publish(new CustomerDiscovered(getId()));
        }
    }

    public void searchForAccommodation(LocalDate arrival, LocalDate departure, PricingEngine pricing, Clock clock) {
        publish(new SearchedForAccommodation(getId(), arrival, departure));
        for (LocalDate date = arrival; date.isBefore(departure); date = date.plusDays(1)) {
            makePriceOffer(date, pricing, clock);
        }
    }

    private void makePriceOffer(LocalDate date, PricingEngine pricing, Clock clock) {
        if (hasValidPriceOffer(date, clock)) {
            return;
        }
        pricing.getAccommodationPrice(date).ifPresent(price -> {
            Instant expires = clock.instant().plus(PRICE_VALIDITY_DURATION);
            publish(new PriceOffered(getId(), date, price, expires));
        });
    }

    private boolean hasValidPriceOffer(LocalDate date, Clock clock) {
        PriceOffered offer = priceOffersByDate.get(date);
        return offer != null && offer.isStillValid(clock);
    }

    public void updateContactInformation(String name, String email) {
        publish(new ContactInformationUpdated(getId(), name, email));
    }

    public void makeReservation(LocalDate arrival, LocalDate departure, Clock clock) {
        checkStateIs(PROSPECT);
        publish(new ReservationInitiated(getId(), arrival, departure, Hotel.checkInTime(arrival), Hotel.checkOutTime(departure)));

        for (LocalDate date = arrival; date.isBefore(departure); date = date.plusDays(1)) {
            PriceOffered offer = getValidPriceOffer(date, clock);
            publish(new LineItemCreated(getId(), lineItems + 1, offer.date, offer.price));
        }
    }

    private PriceOffered getValidPriceOffer(LocalDate date, Clock clock) {
        PriceOffered offer = priceOffersByDate.get(date);
        if (offer == null) {
            throw new IllegalStateException("no price offer for date " + date);
        }
        if (offer.hasExpired(clock)) {
            throw new IllegalStateException("price offer for date " + date + " has expired");
        }
        return offer;
    }

    public void assignRoom(UUID roomId, String roomNumber) {
        checkStateIs(INITIATED);
        publish(new RoomAssigned(getId(), roomId, roomNumber));
    }

    private void checkStateIs(State expected) {
        if (state != expected) {
            throw new IllegalStateException("unexpected state: " + state);
        }
    }
}
