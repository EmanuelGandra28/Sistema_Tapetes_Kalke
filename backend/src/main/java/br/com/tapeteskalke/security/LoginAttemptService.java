package br.com.tapeteskalke.security;
import br.com.tapeteskalke.exception.BusinessException;
import java.time.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
// Limita tentativas de login por e-mail + IP (em memória; reinicia junto com a API).
// A chave inclui o IP para que um terceiro não consiga bloquear o login do dono a partir de outra máquina.
@Service public class LoginAttemptService {
 static final int MAX_FAILURES=5; static final Duration WINDOW=Duration.ofMinutes(15);
 private record Attempts(int failures,Instant firstFailure){}
 private final ConcurrentHashMap<String,Attempts> attempts=new ConcurrentHashMap<>(); private final Clock clock;
 public LoginAttemptService(Clock clock){this.clock=clock;}
 public void checkAllowed(String email,String ip){var a=attempts.get(key(email,ip));if(a==null)return;if(expired(a)){attempts.remove(key(email,ip));return;}if(a.failures()>=MAX_FAILURES){long minutes=Math.max(1,Duration.between(Instant.now(clock),a.firstFailure().plus(WINDOW)).toMinutes());throw new BusinessException("LOGIN_BLOQUEADO","Muitas tentativas inválidas. Tente novamente em "+minutes+" minuto(s)",HttpStatus.TOO_MANY_REQUESTS);}}
 public void failed(String email,String ip){attempts.compute(key(email,ip),(k,a)->a==null||expired(a)?new Attempts(1,Instant.now(clock)):new Attempts(a.failures()+1,a.firstFailure()));}
 public void succeeded(String email,String ip){attempts.remove(key(email,ip));}
 private boolean expired(Attempts a){return Instant.now(clock).isAfter(a.firstFailure().plus(WINDOW));}
 private String key(String email,String ip){return email.trim().toLowerCase()+"|"+ip;}
}
