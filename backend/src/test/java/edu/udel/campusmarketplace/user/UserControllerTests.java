package edu.udel.campusmarketplace.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTests {

	@Autowired
	private MockMvc mockMvc;

	/**
	 * The route is mapped and the body parses; the service has no persistence
	 * behind it yet. Flip this to expect 201 once registration is implemented.
	 */
	@Test
	void registerIsRoutedButNotImplemented() throws Exception {
		mockMvc.perform(post("/api/users/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"firstName":"Christian","lastName":"Palmer"}
						"""))
				.andExpect(status().isNotImplemented());
	}

}
