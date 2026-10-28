package br.com.tapeteskalke.security;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import br.com.tapeteskalke.exception.AccessDeniedBusinessException;
@Component public class CurrentUser {
 public UserPrincipal require(){ Authentication a=SecurityContextHolder.getContext().getAuthentication(); if(a==null || !(a.getPrincipal() instanceof UserPrincipal p)) throw new AccessDeniedBusinessException("Usuário não autenticado"); return p; }
 public boolean hasRole(String role){ try{return require().role().equals(role);}catch(AccessDeniedBusinessException e){return false;} }
}