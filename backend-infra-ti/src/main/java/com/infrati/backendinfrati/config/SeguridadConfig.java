package com.infrati.backendinfrati.config;
import com.infrati.backendinfrati.security.*;
import com.infrati.backendinfrati.service.AccesoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.*;
import org.springframework.security.web.authentication.session.*;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;
import java.util.*;

@Configuration
public class SeguridadConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean AuthenticationManager authenticationManager(UsuarioDetallesService detalles,PasswordEncoder encoder) {
        var proveedor=new DaoAuthenticationProvider(detalles);proveedor.setPasswordEncoder(encoder);
        return new ProviderManager(proveedor);
    }
    @Bean CookieCsrfTokenRepository csrfTokenRepository() {
        var repositorio=CookieCsrfTokenRepository.withHttpOnlyFalse();repositorio.setCookiePath("/");return repositorio;
    }
    @Bean SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean SessionAuthenticationStrategy sessionAuthenticationStrategy(CookieCsrfTokenRepository csrf) {
        return new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(),new CsrfAuthenticationStrategy(csrf)));
    }
    @Bean UrlBasedCorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origenes-front}") String origenes) {
        var config=new CorsConfiguration();config.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::trim).filter(s->!s.isBlank()).toList());
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type","X-XSRF-TOKEN","Accept"));config.setAllowCredentials(true);
        var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/api/**",config);return source;
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,CookieCsrfTokenRepository csrf,SecurityContextRepository contexto,UsuarioDetallesService detalles) throws Exception {
        var manejador=new CsrfTokenRequestAttributeHandler();manejador.setCsrfRequestAttributeName(null);
        http.cors(c->{}).csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(manejador))
            .securityContext(c->c.securityContextRepository(contexto))
            .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->ActualizarSesionFilter.error(res,401,"Inicia sesión para continuar."))
                .accessDeniedHandler((req,res,ex)->ActualizarSesionFilter.error(res,403,"No tienes permiso para realizar esta acción.")))
            .authorizeHttpRequests(a->a
                .requestMatchers("/error","/api/auth/login","/api/auth/csrf").permitAll()
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/usuarios","/api/usuarios/**","/api/roles","/api/roles/**").hasAuthority("administrar_usuarios")
                .requestMatchers(HttpMethod.POST,"/api/conexiones-snmp","/api/conexiones-snmp/probar").hasAuthority("gestionar_conexiones")
                .requestMatchers(HttpMethod.PATCH,"/api/conexiones-snmp/activos/*").hasAuthority("gestionar_conexiones")
                .requestMatchers(HttpMethod.PATCH,"/api/activos/*/administrativos").hasAuthority("editar_ciclo_vida")
                .requestMatchers(HttpMethod.PATCH,"/api/activos/*/mantenimiento").hasAuthority("gestionar_mantenimiento")
                .requestMatchers(HttpMethod.PATCH,"/api/activos/*/baja").hasAuthority("dar_baja")
                .requestMatchers(HttpMethod.PUT,"/api/umbrales/*").hasAuthority("configurar_umbrales")
                .requestMatchers(HttpMethod.POST,"/api/datacenters","/api/datacenters/**","/api/clusters","/api/clusters/**").hasAuthority("gestionar_ubicaciones")
                .requestMatchers(HttpMethod.PUT,"/api/datacenters/**","/api/clusters/**").hasAuthority("gestionar_ubicaciones")
                .requestMatchers(HttpMethod.DELETE,"/api/datacenters/**","/api/clusters/**").hasAuthority("gestionar_ubicaciones")
                .requestMatchers(HttpMethod.GET,"/api/**").hasAuthority("ver_inventario")
                .anyRequest().denyAll())
            .logout(l->l.logoutUrl("/api/auth/logout").invalidateHttpSession(true).clearAuthentication(true)
                .deleteCookies("JSESSIONID").logoutSuccessHandler((req,res,auth)->res.setStatus(204)))
            .addFilterBefore(new ActualizarSesionFilter(detalles),AuthorizationFilter.class);
        return http.build();
    }
    @Bean ApplicationRunner primerAdministrador(AccesoService acceso,
        @Value("${ADMIN_CORREO:}") String correo,@Value("${ADMIN_NOMBRE:}") String nombre,@Value("${ADMIN_CONTRASENA:}") String contrasena) {
        return args->acceso.crearPrimerAdministrador(correo,nombre,contrasena);
    }
}
