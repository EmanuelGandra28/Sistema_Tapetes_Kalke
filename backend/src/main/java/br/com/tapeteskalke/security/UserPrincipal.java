package br.com.tapeteskalke.security;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
public record UserPrincipal(String id,String email,String role) implements UserDetails {
 public Collection<? extends GrantedAuthority> getAuthorities(){ return List.of(new SimpleGrantedAuthority("ROLE_"+role)); }
 public String getPassword(){ return ""; } public String getUsername(){ return email; }
 public boolean isAccountNonExpired(){return true;} public boolean isAccountNonLocked(){return true;} public boolean isCredentialsNonExpired(){return true;} public boolean isEnabled(){return true;}
}