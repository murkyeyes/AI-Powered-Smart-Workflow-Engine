package com.workflow.engine.controller;

import com.workflow.engine.dto.auth.*;
import com.workflow.engine.model.RefreshToken;
import com.workflow.engine.model.User;
import com.workflow.engine.repository.UserRepository;
import com.workflow.engine.security.jwt.JwtUtil;
import com.workflow.engine.security.services.RefreshTokenService;
import com.workflow.engine.security.services.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository,
                          PasswordEncoder encoder, JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String jwt = jwtUtil.generateJwtToken(authentication);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        ResponseCookie jwtRefreshCookie = ResponseCookie.from("refreshToken", refreshToken.getToken())
                .maxAge(7 * 24 * 60 * 60)
                .httpOnly(true)
                .path("/api/v1/auth")
                .secure(false) // Trong thực tế trên HTTPS sẽ để secure(true)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtRefreshCookie.toString())
                .body(JwtResponse.builder()
                .accessToken(jwt)
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .roles(roles)
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại!");
        }

        // Tạo tài khoản mới
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setPasswordHash(encoder.encode(signUpRequest.getPassword()));

        // Mặc định gán Role (Có logic check nếu là truyền lên ADMIN)
        String strRole = signUpRequest.getRole();
        if (strRole != null && strRole.equals("ADMIN")) {
            user.setRole("ADMIN");
        } else {
            user.setRole("USER");
        }

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Đăng ký thành công!"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        // Lấy token từ HttpOnly Cookie
        String requestRefreshToken = getCookieValue(request, "refreshToken");
        if (requestRefreshToken == null || requestRefreshToken.isEmpty()) {
            throw new RuntimeException("Refresh Token trống!");
        }

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    // Xóa token cũ đi để tiến hành Rotation
                    refreshTokenService.deleteByToken(requestRefreshToken);

                    // Tạo Refresh Token mới
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

                    // Tạo Access Token mới
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            UserDetailsImpl.build(user), null, UserDetailsImpl.build(user).getAuthorities());
                    
                    String token = jwtUtil.generateJwtToken(authentication);

                    ResponseCookie jwtRefreshCookie = ResponseCookie.from("refreshToken", newRefreshToken.getToken())
                            .maxAge(7 * 24 * 60 * 60)
                            .httpOnly(true)
                            .path("/api/v1/auth")
                            .secure(false) // Trong thực tế trên HTTPS sẽ để secure(true)
                            .build();
                    
                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, jwtRefreshCookie.toString())
                            .body(JwtResponse.builder()
                            .accessToken(token)
                            .id(user.getId())
                            .username(user.getUsername())
                            .roles(UserDetailsImpl.build(user).getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .collect(Collectors.toList()))
                            .build());
                })
                .orElseThrow(() -> new RuntimeException(
                        "Refresh token không tìm thấy trong database!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        String requestRefreshToken = getCookieValue(request, "refreshToken");
        if (requestRefreshToken != null && !requestRefreshToken.isEmpty()) {
            refreshTokenService.deleteByToken(requestRefreshToken);
        }

        ResponseCookie jwtRefreshCookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0) // Xóa Cookie
                .httpOnly(true)
                .path("/api/v1/auth")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtRefreshCookie.toString())
                .body(new MessageResponse("Đăng xuất thiết bị hiện tại thành công!"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAllDevices() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            refreshTokenService.deleteByUserId(userDetails.getId());
        }

        ResponseCookie jwtRefreshCookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0) // Xóa Cookie
                .httpOnly(true)
                .path("/api/v1/auth")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtRefreshCookie.toString())
                .body(new MessageResponse("Đã đăng xuất khỏi mọi thiết bị!"));
    }

    private String getCookieValue(HttpServletRequest req, String cookieName) {
        if (req.getCookies() != null) {
            for (jakarta.servlet.http.Cookie c : req.getCookies()) {
                if (c.getName().equals(cookieName)) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
