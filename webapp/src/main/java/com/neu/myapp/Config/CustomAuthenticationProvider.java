package com.neu.myapp.Config;

import com.neu.myapp.model.User;
import com.neu.myapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@EnableWebSecurity
public class CustomAuthenticationProvider
        implements AuthenticationProvider {

    @Autowired
    private UserRepository userRepository;

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String name = authentication.getName();
        String password = authentication.getCredentials().toString();

        User user = userRepository.findByEmail(name);

        if (user!=null && BCrypt.checkpw(password, user.getPassword())) {
            return new UsernamePasswordAuthenticationToken(
                    name, password, new ArrayList<>());
        }
        else if(authentication.getName().isEmpty() || authentication.getCredentials().toString().isEmpty()){
            return new UsernamePasswordAuthenticationToken(
                    "BadRequest", "BadRequest", new ArrayList<>());
        }
        else {
            return new UsernamePasswordAuthenticationToken(
                    "Unauthorized", "Unauthorized", new ArrayList<>());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(
                UsernamePasswordAuthenticationToken.class);
    }
}