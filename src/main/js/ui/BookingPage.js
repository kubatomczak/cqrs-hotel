// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";
import {connect} from "react-redux";
import {Layout} from "./Layout";
import {apiFetch} from "../util";
import {AccommodationSearchForm} from "./AccommodationSearchForm";
import {reservationMade} from "../reservationActions";

let BookingPage = ({reservationOffer, makeReservation}) => (
  <Layout>
    <AccommodationSearchForm/>
    {reservationOffer && <p>
      <b>Check in:</b> {reservationOffer.startDate}<br/>
      <b>Check out:</b> {reservationOffer.endDate}<br/>
      <b>Cost:</b> {reservationOffer.totalPrice}<br/>
      <button type="button" onClick={() => makeReservation(reservationOffer)}>Make Reservation</button>
    </p>
    }
  </Layout>
);

function mapStateToProps(state) {
  return {
    reservationOffer: state.reservation.offer,
  }
}

function mapDispatchToProps(dispatch) {
  return {
    makeReservation: (reservationOffer) => {
      apiFetch(
        '/api/make-reservation', {
          method: 'post',
          body: {
            ...reservationOffer,
            name: "John Doe",
            email: "john@example.com",
          },
        })
        .then(result => {
          dispatch(reservationMade(result));
        })
        .catch(ex => console.log("makeReservation failed", ex));
    },
  }
}

BookingPage = connect(mapStateToProps, mapDispatchToProps)(BookingPage);

export {BookingPage};