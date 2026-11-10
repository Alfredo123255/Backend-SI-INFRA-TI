package com.infrati.backendinfrati.security;
import com.infrati.backendinfrati.model.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
public final class UsuarioPrincipal extends User {
    private final Integer id;
    public UsuarioPrincipal(Usuario usuario) {
        super(usuario.getCorreo(),usuario.getContrasenaHash(),usuario.isActivo(),true,true,true,
            usuario.getRol().permisos().entrySet().stream().filter(e->e.getValue())
            .map(e->new SimpleGrantedAuthority(e.getKey())).toList());
        this.id=usuario.getId();
    }
    public Integer getId() { return id; }
}
