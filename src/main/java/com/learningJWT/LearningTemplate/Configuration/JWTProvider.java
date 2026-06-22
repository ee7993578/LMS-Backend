package com.learningJWT.LearningTemplate.Configuration;

import com.learningJWT.LearningTemplate.Model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JWTProvider {
    public String generateToken(User user) {

        return Jwts.builder().setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis()+JWTConstant.EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256,JWTConstant.JWT_SECRET)
                .claim("role", user.getRole())
                .compact();
    }
}
