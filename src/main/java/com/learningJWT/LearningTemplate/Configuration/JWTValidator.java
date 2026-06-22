package com.learningJWT.LearningTemplate.Configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;


@Component
public class JWTValidator {

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }


    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new java.util.Date());
    }
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(JWTConstant.JWT_SECRET)
                .parseClaimsJws(token)
                .getBody();
    }

}
