package com.github.igor.ecommerce.support;

import com.github.igor.ecommerce.entity.User;
import com.github.igor.ecommerce.enums.Role;
import com.github.igor.ecommerce.repository.UserRepository;
import com.github.igor.ecommerce.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestSupport {

    protected static final String AUTHORIZATION_HEADER = HttpHeaders.AUTHORIZATION;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected String userToken;
    protected String adminToken;

    @BeforeEach
    void setUpSecurityTokens() {
        User admin = userRepository.findByEmail("admin@ecommerce.com").orElseThrow();
        adminToken = jwtService.generateToken(admin);

        User user = userRepository.save(new User(
                "Catalog User",
                "catalog-user@ecommerce.com",
                passwordEncoder.encode("password123"),
                Role.USER
        ));
        userToken = jwtService.generateToken(user);
    }

    protected RequestBuilder bearer(RequestBuilder builder, String token) {
        ((MockHttpServletRequestBuilder) builder).header(AUTHORIZATION_HEADER, "Bearer " + token);
        return builder;
    }
}
