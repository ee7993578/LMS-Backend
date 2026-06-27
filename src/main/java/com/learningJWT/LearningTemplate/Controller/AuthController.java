package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Exception.UserException;
import com.learningJWT.LearningTemplate.Paylod.DTO.UserDto;
import com.learningJWT.LearningTemplate.Paylod.Response.AuthResponse;
import com.learningJWT.LearningTemplate.Services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Optional: enable if frontend is separate
public class AuthController {

    private final AuthService authService;

    // ------------------- Signup -------------------
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody UserDto userDto) {
        try {
            AuthResponse response = authService.signup(userDto);
            return ResponseEntity.ok(response);
        } catch (UserException e) {
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder().message(e.getMessage()).build()
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    AuthResponse.builder().message("Something went wrong: " + e.getMessage()).build()
            );
        }
    }

    // ------------------- Login -------------------
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody UserDto userDto) {
        try {
            AuthResponse response = authService.login(userDto);
            return ResponseEntity.ok(response);
        } catch (UserException e) {
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder().message(e.getMessage()).build()
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    AuthResponse.builder().message("Something went wrong: " + e.getMessage()).build()
            );
        }
    }

    // ------------------- Test Endpoint -------------------
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("AuthController is working fine ✅");
    }
}
