package kz.iitu.springlab.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/** Lab 10: the access matrix of task 3.4, the database login of task 2 and the CSRF check of task 4. */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    // ---- task 2: users come from app_user, passwords are BCrypt hashes ----

    @Test
    void passwordsAreStoredOnlyAsBcryptHashes() {
        List<Map<String, Object>> rows = jdbc.queryForList("select username, password_hash from app_user order by id");
        rows.forEach(row -> System.out.println(">>> " + row));
        assertThat(rows).allSatisfy(row -> {
            String hash = (String) row.get("password_hash");
            assertThat(hash).startsWith("$2a$10$").hasSize(60).doesNotContain("123");
        });
        String userHash = (String) rows.get(0).get("password_hash");
        String readerHash = (String) rows.get(2).get("password_hash");
        assertThat(userHash).isNotEqualTo(readerHash);            // the same password user123 ...
        assertThat(encoder.matches("user123", userHash)).isTrue(); // ... and both hashes accept it
        assertThat(encoder.matches("user123", readerHash)).isTrue();
    }

    @Test
    void databaseUsersCanLogIn() throws Exception {
        mvc.perform(formLogin("/login").user("user").password("user123"))
                .andExpect(authenticated().withUsername("user").withRoles("USER"));
        mvc.perform(formLogin("/login").user("admin").password("admin123"))
                .andExpect(authenticated().withUsername("admin").withRoles("ADMIN", "USER"));
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameAnswer() throws Exception {
        mvc.perform(formLogin("/login").user("user").password("wrong"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
        mvc.perform(formLogin("/login").user("nobody").password("wrong"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @DirtiesContext // changes a row of app_user
    void disabledUserCannotLogIn() throws Exception {
        jdbc.update("update app_user set enabled = false where username = 'reader'");
        mvc.perform(formLogin("/login").user("reader").password("user123"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }

    // ---- task 3.4: the access matrix ----

    @Test
    void anonymous() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/books")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/books").with(csrf()).param("title", "X").param("author", "Y").param("year", "2020"))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/admin")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/nowhere")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
    }

    @Test
    void userRole() throws Exception {
        var user = user("user").roles("USER");
        mvc.perform(get("/").with(user)).andExpect(status().isOk());
        mvc.perform(get("/books").with(user)).andExpect(status().isOk());
        mvc.perform(post("/books").with(user).with(csrf()).param("title", "X").param("author", "Y").param("year", "2020"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(user)).andExpect(status().isForbidden());
        mvc.perform(get("/nowhere").with(user)).andExpect(status().isForbidden());
    }

    @Test
    void adminRole() throws Exception {
        var admin = user("admin").roles("ADMIN", "USER");
        mvc.perform(get("/").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/books").with(admin)).andExpect(status().isOk());
        mvc.perform(post("/books").with(admin).with(csrf()).param("title", "X").param("author", "Y").param("year", "2020"))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/books"));
        mvc.perform(get("/admin").with(admin)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin")));
        mvc.perform(get("/nowhere").with(admin)).andExpect(status().isForbidden()); // denyAll() is for everybody
    }

    // ---- task 3.2: the URL rule lets USER into the API, @PreAuthorize stops the delete ----

    @Test
    void preAuthorizeStopsUserEvenWhenTheUrlRuleLetsHimIn() throws Exception {
        mvc.perform(get("/api/books/1").with(user("user").roles("USER"))).andExpect(status().isOk());
        mvc.perform(delete("/api/books/1").with(user("user").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ---- task 4: CSRF ----

    @Test
    void postWithoutTokenIsRejected() throws Exception {
        var admin = user("admin").roles("ADMIN", "USER");
        mvc.perform(post("/books").with(admin).param("title", "Forged").param("author", "M").param("year", "2020"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/books").with(admin).with(csrf().useInvalidToken())
                        .param("title", "Forged").param("author", "M").param("year", "2020"))
                .andExpect(status().isForbidden());
    }

    @Test
    void everyPostFormHasTheHiddenCsrfField() throws Exception {
        mvc.perform(get("/login")).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mvc.perform(get("/books").with(user("admin").roles("ADMIN", "USER")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
    }

    // ---- variant 1: own login page with error and logout messages ----

    @Test
    void ownLoginPageShowsErrorAndLogoutMessages() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<h1>Sign in</h1>")));
        mvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid username or password.")));
        mvc.perform(get("/login").param("logout", ""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("You have been signed out.")));
        mvc.perform(post("/logout").with(user("user").roles("USER")).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
    }
}
