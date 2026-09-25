package com.example.kartu.services;

import com.example.kartu.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;

/** SKPL-F03: Google login over plain OAuth2 (no "openid" scope). Account mapping lives in GoogleAccountService. */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final GoogleAccountService googleAccountService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        User user = googleAccountService.upsert(
                oAuth2User.getAttribute("email"),
                oAuth2User.getAttribute("name"),
                oAuth2User.getAttribute("sub"),
                oAuth2User.getAttribute("picture"));

        String role = user.getRole() != null ? user.getRole() : "ROLE_USER";
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(role)),
                oAuth2User.getAttributes(),
                "email");
    }
}
