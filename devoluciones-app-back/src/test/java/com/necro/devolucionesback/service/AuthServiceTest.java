package com.necro.devolucionesback.service;

import com.necro.devolucionesback.dto.AuthResponse;
import com.necro.devolucionesback.dto.LoginRequest;
import com.necro.devolucionesback.dto.RegisterRequest;
import com.necro.devolucionesback.exception.UserAlreadyExistsException;
import com.necro.devolucionesback.model.Role;
import com.necro.devolucionesback.model.User;
import com.necro.devolucionesback.repository.RoleRepository;
import com.necro.devolucionesback.repository.UserRepository;
import com.necro.devolucionesback.service.jwtUtils.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private Role analistaRole;
    private User savedUser;

    @BeforeEach
    void setUp() {
        analistaRole = Role.builder().id(1L).name("ANALISTA").build();
        savedUser = User.builder()
                .id(1L)
                .username("analista1")
                .email("analista@test.com")
                .password("$2a$encoded")
                .roles(Set.of(analistaRole))
                .build();
    }

    @Nested
    @DisplayName("login - autenticacion")
    class LoginTest {

        @Test
        @DisplayName("Credenciales validas -> retorna AuthResponse con token")
        void login_validCredentials_returnsAuthResponse() {
            LoginRequest request = new LoginRequest("analista@test.com", "password123");
            Authentication auth = mock(Authentication.class);
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    "analista1", "$2a$encoded",
                    java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ANALISTA")));

            given(userRepository.findByEmail("analista@test.com")).willReturn(Optional.of(savedUser));
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(auth);
            given(auth.getPrincipal()).willReturn(userDetails);
            given(jwtService.getToken(any(UserDetails.class))).willReturn("jwt-token-123");

            AuthResponse response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token-123");
            assertThat(response.getIssuedAt()).isNotNull();
            assertThat(response.getExpiration()).isNotNull();
        }

        @Test
        @DisplayName("Email inexistente -> 401 Unauthorized")
        void login_emailNotFound_throwsUnauthorized() {
            LoginRequest request = new LoginRequest("noexiste@test.com", "password123");
            given(userRepository.findByEmail("noexiste@test.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        @DisplayName("Password incorrecta -> 401 Unauthorized")
        void login_wrongPassword_throwsUnauthorized() {
            LoginRequest request = new LoginRequest("analista@test.com", "wrongpassword");
            given(userRepository.findByEmail("analista@test.com")).willReturn(Optional.of(savedUser));
            willThrow(new BadCredentialsException("Invalid credentials"))
                    .given(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid email or password");
        }
    }

    @Nested
    @DisplayName("createUser - registro")
    class CreateUserTest {

        @Test
        @DisplayName("Registro exitoso -> retorna AuthResponse con token")
        void createUser_validRequest_returnsAuthResponse() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("nuevoanalista")
                    .email("nuevo@test.com")
                    .password("password123")
                    .build();

            given(userRepository.existsByEmail("nuevo@test.com")).willReturn(false);
            given(roleRepository.findByName("ANALISTA")).willReturn(Optional.of(analistaRole));
            given(passwordEncoder.encode("password123")).willReturn("$2a$encoded");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(jwtService.getToken(any(UserDetails.class))).willReturn("jwt-token-456");

            AuthResponse response = authService.createUser(request);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token-456");
        }

        @Test
        @DisplayName("Email ya existe -> UserAlreadyExistsException")
        void createUser_emailExists_throwsException() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("otro")
                    .email("analista@test.com")
                    .password("password123")
                    .build();

            given(userRepository.existsByEmail("analista@test.com")).willReturn(true);

            assertThatThrownBy(() -> authService.createUser(request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Email ya existe");
        }

        @Test
        @DisplayName("Request nulo -> NullPointerException")
        void createUser_nullRequest_throwsNPE() {
            assertThatThrownBy(() -> authService.createUser(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("registerUser - construccion de usuario")
    class RegisterUserTest {

        @Test
        @DisplayName("registerUser crea usuario con rol ANALISTA por defecto")
        void registerUser_validRequest_createsUserWithAnalistaRole() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("nuevoanalista")
                    .email("nuevo@test.com")
                    .password("password123")
                    .build();

            given(roleRepository.findByName("ANALISTA")).willReturn(Optional.of(analistaRole));
            given(passwordEncoder.encode("password123")).willReturn("$2a$encoded");

            User user = authService.registerUser(request);

            assertThat(user.getUsername()).isEqualTo("nuevoanalista");
            assertThat(user.getEmail()).isEqualTo("nuevo@test.com");
            assertThat(user.getRoles()).containsExactly(analistaRole);
            then(passwordEncoder).should().encode("password123");
        }

        @Test
        @DisplayName("registerUser con request nulo -> IllegalArgumentException")
        void registerUser_nullRequest_throwsIllegalArgument() {
            assertThatThrownBy(() -> authService.registerUser(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nulo");
        }

        @Test
        @DisplayName("registerUser con rol ANALISTA no encontrado -> RuntimeException")
        void registerUser_roleNotFound_throwsRuntime() {
            RegisterRequest request = RegisterRequest.builder()
                    .username("nuevoanalista")
                    .email("nuevo@test.com")
                    .password("password123")
                    .build();

            given(roleRepository.findByName("ANALISTA")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerUser(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ANALISTA");
        }
    }
}
