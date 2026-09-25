package com.example.kartu.services;

import com.example.kartu.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * SKPL-F03: Google login over OpenID Connect (used when the scope contains "openid").
 * Account mapping lives in GoogleAccountService.
 */
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final GoogleAccountService googleAccountService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail() != null ? oidcUser.getEmail() : oidcUser.getAttribute("email");
        User user = googleAccountService.upsert(email, oidcUser.getFullName(), oidcUser.getSubject(), oidcUser.getPicture());

        String role = user.getRole() != null ? user.getRole() : "ROLE_USER";
        return new DefaultOidcUser(
                Collections.singleton(new SimpleGrantedAuthority(role)),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "email");
    }
}
