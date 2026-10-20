package edu.udel.campusmarketplace.reference;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only lookup data for marketplace forms. */
@RestController
@RequestMapping("/api")
public class ReferenceDataController {

	private final ReferenceDataRepository repository;

	ReferenceDataController(ReferenceDataRepository repository) {
		this.repository = repository;
	}

	@GetMapping("/categories")
	public List<CategoryResponse> categories() {
		return repository.findCategories();
	}

	@GetMapping("/universities")
	public List<UniversityResponse> universities() {
		return repository.findUniversities();
	}

	@ExceptionHandler(DataAccessException.class)
	ResponseEntity<Map<String, String>> databaseUnavailable() {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(Map.of("message", "Reference data is temporarily unavailable."));
	}
}
