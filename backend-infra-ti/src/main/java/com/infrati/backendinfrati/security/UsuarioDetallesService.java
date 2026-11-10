package com.infrati.backendinfrati.security;
import com.infrati.backendinfrati.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
@Service
public class UsuarioDetallesService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    public UsuarioDetallesService(UsuarioRepository usuarios) { this.usuarios=usuarios; }
    @Override @Transactional(readOnly=true)
    public UserDetails loadUserByUsername(String correo) {
        return usuarios.findByCorreo(correo.trim().toLowerCase(Locale.ROOT)).map(UsuarioPrincipal::new)
            .orElseThrow(()->new UsernameNotFoundException("Usuario no encontrado"));
    }
    @Transactional(readOnly=true)
    public UsuarioPrincipal cargarPorId(Integer id) {
        return usuarios.findById(id).map(UsuarioPrincipal::new).orElse(null);
    }
}
