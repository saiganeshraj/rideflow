package com.rideflow.dispatch;

import com.rideflow.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing endpoints for ride dispatching and driver ride offer lifecycle.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Dispatch", description = "Ride dispatch and driver offer operations")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    /**
     * Dispatches a ride to eligible nearby drivers.
     * Only the customer who created the ride can dispatch it.
     *
     * @param rideId    the ID of the ride
     * @param principal authenticated user principal
     * @param request   optional dispatch parameters
     * @return dispatch response detailing offer generation results
     */
    @PostMapping("/rides/{rideId}/dispatch")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Dispatch ride",
            description = "Dispatches a ride to eligible nearby drivers within radius",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public RideDispatchResponse dispatchRide(
            @PathVariable("rideId") UUID rideId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) RideDispatchRequest request) {
        return dispatchService.dispatchRide(rideId, principal, request);
    }

    /**
     * Retrieves all pending ride offers addressed to the authenticated driver.
     *
     * @param principal authenticated driver principal
     * @return list of pending ride offers
     */
    @GetMapping({"/drivers/me/offers", "/offers/my-offers"})
    @Operation(
            summary = "List pending driver offers",
            description = "Returns active, non-expired pending ride offers addressed to the authenticated driver",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<RideOfferResponse> getMyOffers(@AuthenticationPrincipal UserPrincipal principal) {
        return dispatchService.getDriverPendingOffers(principal);
    }

    /**
     * Accepts a pending ride offer.
     * Only the driver addressed by the offer can accept it.
     *
     * @param offerId   the ID of the offer
     * @param principal authenticated driver principal
     * @return accepted offer response
     */
    @PostMapping({"/offers/{offerId}/accept", "/drivers/me/offers/{offerId}/accept"})
    @Operation(
            summary = "Accept ride offer",
            description = "Accepts a pending ride offer, assigns driver to the ride, and cancels other pending offers",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public RideOfferResponse acceptOffer(
            @PathVariable("offerId") UUID offerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return dispatchService.acceptOffer(offerId, principal);
    }

    /**
     * Rejects a pending ride offer.
     * Only the driver addressed by the offer can reject it.
     *
     * @param offerId   the ID of the offer
     * @param principal authenticated driver principal
     * @param request   optional rejection reason
     * @return rejected offer response
     */
    @PostMapping({"/offers/{offerId}/reject", "/drivers/me/offers/{offerId}/reject"})
    @Operation(
            summary = "Reject ride offer",
            description = "Rejects a pending ride offer addressed to the authenticated driver",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public RideOfferResponse rejectOffer(
            @PathVariable("offerId") UUID offerId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) RideOfferRejectRequest request) {
        return dispatchService.rejectOffer(offerId, principal, request);
    }
}
