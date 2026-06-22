package com.learningJWT.LearningTemplate.Services.Impl;
import com.learningJWT.LearningTemplate.Configuration.JWTProvider;
import com.learningJWT.LearningTemplate.Enum.UserRole;
import com.learningJWT.LearningTemplate.Exception.UserException;
import com.learningJWT.LearningTemplate.Model.User;
import com.learningJWT.LearningTemplate.Paylod.DTO.UserDto;
import com.learningJWT.LearningTemplate.Paylod.Response.AuthResponse;
import com.learningJWT.LearningTemplate.Repository.UserRepository;
import com.learningJWT.LearningTemplate.Services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTProvider jwtProvider;
    private final AuthenticationManager authenticationManager; // use AuthenticationManager to authenticate
    private final CustomerUserDetailServices customerUserDetailServices;

    @Override
    public AuthResponse signup(UserDto userDto) throws UserException {

        if (userDto.getUsername() == null || userDto.getPassword() == null) {
            throw new UserException("Username and password are required");
        }

        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new UserException("User already exists!");
        }

//        if (userDto.getRole() != null && userDto.getRole() == UserRole.ROLE_SUPERADMIN) {
//            throw new UserException("SuperAdmin cannot be created through signup");
//        }

        User newUser = User.builder()
                .username(userDto.getUsername())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .role(userDto.getRole() != null ? userDto.getRole() : UserRole.ROLE_STUDENT) // default role if null
                .fullName(userDto.getFullName())
                .phone(userDto.getPhone())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .lastlogin(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(newUser);

        // Authenticate the newly created user (optional). Using AuthenticationManager is better.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userDto.getUsername(), userDto.getPassword())
        );

        // Generate JWT using saved user entity
        String jwt = jwtProvider.generateToken(savedUser);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setJwt(jwt);
        authResponse.setMessage("Registered Successfully");
        return authResponse;
    }

    @Override
    public AuthResponse login(UserDto userDto) throws UserException {
        if (userDto.getUsername() == null || userDto.getPassword() == null) {
            throw new UserException("Username and password are required");
        }

        try {
            // Authenticate using AuthenticationManager (this will call your UserDetailsService internally)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDto.getUsername(), userDto.getPassword())
            );

            // At this point authentication is successful
            UserDetails principal = (UserDetails) authentication.getPrincipal();

            // fetch full User entity to include additional claims (like library id etc.)
            Optional<User> optionalUser = userRepository.findByUsername(principal.getUsername());
            if (optionalUser.isEmpty()) {
                throw new UserException("User not found after authentication");
            }
            User user = optionalUser.get();

            // update last login
            user.setLastlogin(LocalDateTime.now());
            userRepository.save(user);

            // generate token
            String jwt = jwtProvider.generateToken(user);

            // prepare response
            AuthResponse response = new AuthResponse();
            response.setJwt(jwt);
            response.setMessage("Login Successfully");
            return response;

        } catch (BadCredentialsException ex) {
            throw new UserException("Invalid username or password");
        } catch (Exception ex) {
            throw new UserException("Authentication failed: " + ex.getMessage());
        }
    }
}
