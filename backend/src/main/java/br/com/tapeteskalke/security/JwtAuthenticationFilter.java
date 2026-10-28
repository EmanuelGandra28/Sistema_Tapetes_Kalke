package br.com.tapeteskalke.security;
import br.com.tapeteskalke.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component public class JwtAuthenticationFilter extends OncePerRequestFilter {
 private final JwtTokenService tokens; private final UsuarioRepository users; public JwtAuthenticationFilter(JwtTokenService tokens,UsuarioRepository users){this.tokens=tokens;this.users=users;}
 // O cargo vem do banco a cada requisição: usuário desativado ou com cargo alterado perde o acesso antigo na hora.
 // A versão de sessão no token precisa bater com a do banco; logout e troca de senha a incrementam.
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException { String header=request.getHeader("Authorization"); if(header!=null&&header.startsWith("Bearer ")) try { Claims c=tokens.parse(header.substring(7)); Number sv=c.get(JwtTokenService.SESSION_CLAIM,Number.class); users.activeSession(c.getSubject()).filter(s->sv!=null&&s.getVersao()==sv.longValue()).ifPresentOrElse(s->{var principal=new UserPrincipal(c.getSubject(),c.get("email",String.class),s.getRole()); SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));},SecurityContextHolder::clearContext); } catch(Exception ignored) { SecurityContextHolder.clearContext(); } chain.doFilter(request,response); }
}
