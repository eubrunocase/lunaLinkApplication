package com.LunaLink.application.infrastructure.security;

import com.LunaLink.application.application.businnesRules.AdministratorService;
import com.LunaLink.application.application.businnesRules.ResidentService;
import com.LunaLink.application.application.jwtService.TokenService;
import com.LunaLink.application.infrastructure.repository.AdministratorRepository;
import com.LunaLink.application.infrastructure.repository.ResidentRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final ResidentRepository residentRepository;
    private final AdministratorRepository administratorRepository;

    public SecurityFilter(TokenService tokenService, ResidentRepository residentRepository,
                          AdministratorRepository administratorRepository) {
        this.tokenService = tokenService;
        this.residentRepository = residentRepository;
        this.administratorRepository = administratorRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try {
            var token = this.recoverToken(request);
            if (token != null) {
                System.out.println("Token encontrado: " + token.substring(0, Math.min(10, token.length())) + "...");
                var login = tokenService.validadeToken(token);

                if (!"Invalid token".equals(login)) {
                    System.out.println("Token válido para usuário: " + login);

                    UserDetails user = administratorRepository.findByLogin(login);

                    if (user == null) {
                        user = residentRepository.findByLogin(login);
                    }

                    if (user != null) {
                        System.out.println("Configurando autenticação para: " + login + " com roles: " + user.getAuthorities());
                        var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        System.out.println("Usuário não encontrado para o login: " + login);
                    }
                } else {
                    System.out.println("Token inválido");
                }
            }
        } catch (Exception e) {
            System.err.println("Erro no filtro de segurança: " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }

   public String recoverToken(HttpServletRequest request) {
       var authHeader = request.getHeader("Authorization");
       if(authHeader == null) return null;
       return authHeader.replace("Bearer ", "");
   }

}




