package edu.udel.campusmarketplace.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void acknowledgesPostedCredentials() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"cpalm21@udel.edu","password":"hunter2"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("received"))
				.andExpect(jsonPath("$.email").value("cpalm21@udel.edu"));
	}

}
