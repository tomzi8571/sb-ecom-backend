package com.ecommerce.project.security.jwt;

import com.ecommerce.project.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Calendar;
import java.util.Date;

@Component
public class JwtUtils {
    public static final Logger LOG = LoggerFactory.getLogger(JwtUtils.class);
    public static final int MAX_AGE_SECONDS_ONE_DAY = 24 * 60 * 60;
    @Value("${spring.ecom.app.jwtCookieName}")
    private String jwtCookie;
    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirationTime;
    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;
    @Value("${spring.app.secureCookie}")
    private boolean secureCookie;

    // Get JWT  Header
    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        LOG.debug("Authorization bearerToken is {}", bearerToken);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // remove Bearer prefix
        }
        return null;
    }

    public String getJwtFromCookies(HttpServletRequest request) {
        Cookie jwtCookie = WebUtils.getCookie(request, this.jwtCookie);
        if (jwtCookie != null) {
            return jwtCookie.getValue();
        }
        return null;
    }

    public ResponseCookie generateJwtCookie(UserDetailsImpl user) {
        String jwtToken = generateTokenFromUsername(user.getUsername());
        return ResponseCookie.from(jwtCookie, jwtToken)
                .path("/api")
                .maxAge(MAX_AGE_SECONDS_ONE_DAY)
                .httpOnly(false)
                // Should be true in production with HTTPS
                .secure(secureCookie)
                .build();
    }

    public ResponseCookie generateCleanJwtCookie() {
        return ResponseCookie.from(jwtCookie, null)
                .path("/api")
                .build();
    }

    public String generateTokenFromUsername(UserDetails userDetails) {
        String username = userDetails.getUsername();
        return generateTokenFromUsername(username);
    }

    private String generateTokenFromUsername(String username) {
        return Jwts.builder().subject(username)
                .issuedAt(Calendar.getInstance().getTime())
                .expiration(new Date(new Date().getTime() + jwtExpirationTime))
                .signWith(key())
                .compact();
    }

    public String getUserNameFromJWTToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build().parseSignedClaims(token)
                .getPayload().getSubject();
    }

    public Key key() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret));
    }

    public boolean validateJwtToken(String authToken) {
        try {
            System.out.println("Validating JWT Token");
            Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build().parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            LOG.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            LOG.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            LOG.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            LOG.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
