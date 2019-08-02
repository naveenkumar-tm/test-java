package com.test;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JWTJavaWithPublicPrivateKey {

    public static void main(String[] args) {

        System.out.println("generating keys");
        Map<String, Object> rsaKeys = null;

        try {
            rsaKeys = getRSAKeys();
        } catch (Exception e) {

            e.printStackTrace();
        }
        PublicKey publicKey = (PublicKey) rsaKeys.get("public");
        PrivateKey privateKey = (PrivateKey) rsaKeys.get("private");

        System.out.println("generated keys " + publicKey.toString());

        String token = generateToken(privateKey);
        System.out.println("Generated Token:\n" + token);

        verifyToken(token, publicKey);

    }

    public static String generateToken(PrivateKey privateKey) {
        String token = null;
        try {
            Map<String, Object> claims = new HashMap<String, Object>();

            // put your information into claim
            claims.put("id", "xxx");
            claims.put("role", "user");
            claims.put("created", new Date());

            token = Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.RS512, privateKey).compact();


        } catch (Exception e) {
            e.printStackTrace();
        }
        return token;
    }

    // verify and get claims using public key

    private static Claims verifyToken(String token, PublicKey publicKey) {
        Claims claims;
        try {
            claims = Jwts.parser().setSigningKey(publicKey).parseClaimsJws(token).getBody();

            System.out.println(claims.get("id"));
            System.out.println(claims.get("role"));

        } catch (Exception e) {

            claims = null;
        }
        return claims;
    }

    // Get RSA keys. Uses key size of 2048.
    private static Map<String, Object> getRSAKeys() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        Map<String, Object> keys = new HashMap<String, Object>();
        keys.put("private", privateKey);
        keys.put("public", publicKey);
        return keys;
    }
}