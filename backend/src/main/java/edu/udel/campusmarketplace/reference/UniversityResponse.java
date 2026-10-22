package edu.udel.campusmarketplace.reference;

import java.util.UUID;

public record UniversityResponse(UUID universityId, String universityName, String city, String stateInitials) {
}
