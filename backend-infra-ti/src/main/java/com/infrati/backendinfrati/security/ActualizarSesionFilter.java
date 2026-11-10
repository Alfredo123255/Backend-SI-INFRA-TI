package com.infrati.backendinfrati.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class ActualizarSesionFilter extends OncePerRequestFilter {
    private final UsuarioDetallesService detalles;
    public ActualizarSesionFilter(UsuarioDetallesService detalles) { this.detalles=detalles; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var contexto=SecurityContextHolder.getContext();var autenticacion=contexto.getAuthentication();
        if(autenticacion!=null && autenticacion.getPrincipal() instanceof UsuarioPrincipal previo) {
            var actual=detalles.cargarPorId(previo.getId());
            if(actual==null || !actual.isEnabled()) {
                SecurityContextHolder.clearContext();var sesion=request.getSession(false);if(sesion!=null)sesion.invalidate();
                error(response,401,"Tu sesión expiró. Vuelve a iniciar sesión.");return;
            }
            actual.eraseCredentials();
            var renovada=UsernamePasswordAuthenticationToken.authenticated(actual,null,actual.getAuthorities());
            renovada.setDetails(autenticacion.getDetails());contexto.setAuthentication(renovada);
        }
        chain.doFilter(request,response);
    }
    public static void error(HttpServletResponse respuesta,int estado,String mensaje) throws IOException {
        respuesta.setStatus(estado);respuesta.setContentType("application/json");respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write("{\"message\":\""+mensaje+"\"}");
    }
}
