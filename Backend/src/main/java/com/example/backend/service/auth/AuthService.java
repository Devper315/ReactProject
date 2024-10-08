package com.example.backend.service.auth;

import com.example.backend.dto.request.auth.IntrospectRequest;
import com.example.backend.dto.request.auth.LoginRequest;
import com.example.backend.dto.response.IntrospectResponse;
import com.example.backend.dto.response.LoginResponse;
import com.example.backend.entity.user.User;
import com.example.backend.repository.UserRepo;
import com.example.backend.utils.DateUtils;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.util.*;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuthService {
    UserRepo userRepo;
    PasswordEncoder passwordEncoder;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.validDuration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshableDuration}")
    protected long REFRESHABLE_DURATION;


    public LoginResponse authenticate(LoginRequest request) {
        User user = userRepo.findByEmail(request.getEmail().toLowerCase());
        boolean result = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!result) throw new RuntimeException("Thông tin tài khoản hoặc mật khẩu không chính xác");
        String token = generateToken(user);
        return LoginResponse.builder()
                .token(token)
                .build();
    }

    public IntrospectResponse introspect(IntrospectRequest request) {
        String token = request.getToken();
        boolean isValid = true;
        try {
            verifyToken(token, false);
        } catch (Exception e) {
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    public String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("devper315")
                .issueTime(new Date())
                .expirationTime(DateUtils.addSecondsToDate(new Date(), VALID_DURATION))
                .jwtID(UUID.randomUUID().toString())
                .claim("customClaim", buildCustomClaim(user))
                .claim("scope", buildScope(user))
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            e.printStackTrace();
            return "Không thể tạo token";
        }
    }

    public Map<String, Object> buildCustomClaim(User user) {
        Map<String, Object> customClaims = new HashMap<>();
        customClaims.put("name", user.getFullName());
        customClaims.put("role", user.getRoles());
        customClaims.put("id", user.getId());
        return customClaims;
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
        Date expiryTime = claimsSet.getExpirationTime();
        boolean verified = signedJWT.verify(verifier);
        if (! (verified && expiryTime.after(new Date()))){
            throw new RuntimeException("Token không hợp lệ");
        }
        return signedJWT;
    }

    private String buildScope(User user) {
        StringJoiner joiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> {
                joiner.add("ROLE_" + role.getName());
//				if (!role.getPermissionSet().isEmpty())
//					role.getPermissionSet().forEach(permission -> joiner.add(permission.getName()));
            });
        }
        return joiner.toString();
    }
}
