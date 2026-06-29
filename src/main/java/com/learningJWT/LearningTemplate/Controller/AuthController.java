package com.learningJWT.LearningTemplate.Controller;

import com.learningJWT.LearningTemplate.Exception.UserException;
import com.learningJWT.LearningTemplate.Paylod.DTO.UserDto;
import com.learningJWT.LearningTemplate.Paylod.Response.AuthResponse;
import com.learningJWT.LearningTemplate.Services.AuthService;
import lombok.RequiredArgsConstructor;
import com.learningJWT.LearningTemplate.Services.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Optional: enable if frontend is separate
public class AuthController {

    private final PasswordResetService passwordResetService;

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


    // ── Password Reset Flow ─────────────────────────────────────────────────
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody java.util.Map<String,String> body) {
        try {
            passwordResetService.initiateReset(body.get("email"));
            return ResponseEntity.ok(java.util.Map.of("message",
                "If that email is registered, a reset link has been sent."));
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Map.of("message",
                "If that email is registered, a reset link has been sent."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody java.util.Map<String,String> body) {
        try {
            String token    = body.get("token");
            String password = body.get("password");
            if (token == null || password == null || password.length() < 6)
                return ResponseEntity.badRequest().body(java.util.Map.of("message","Invalid request"));
            passwordResetService.resetPassword(token, password);
            return ResponseEntity.ok(java.util.Map.of("message","Password reset successful. Please login."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    // ── Test Endpoint ───────────────────────────────────────────────────────

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("AuthController is working fine ✅");
    }
}
