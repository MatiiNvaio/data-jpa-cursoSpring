package com.bolsadeideas.springboot.app.models.services;

import com.bolsadeideas.springboot.app.models.dao.IUsuarioDao;
import com.bolsadeideas.springboot.app.models.entity.Role;
import com.bolsadeideas.springboot.app.models.entity.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service("jpaUserDetailService")
public class JpaUserDatailService implements UserDetailsService {

    @Autowired
    private IUsuarioDao usuarioDao;

    private Logger logger = LoggerFactory.getLogger(JpaUserDatailService.class);

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Usuario usuario = usuarioDao.findByUsername(username);

        if(usuario == null){
            logger.error("Error login: no existe el usuario '"+username+"'");
            throw new UsernameNotFoundException("Username "+username+" no existe en el sistema!");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();

        for(Role roles : usuario.getRoles()){
            logger.info("Role: ".concat(roles.getAuthority()));
            authorities.add((new SimpleGrantedAuthority(roles.getAuthority())));
        }

        if(authorities.isEmpty()){
            logger.error("Error login: usuario '"+username+"' no tiene roles asignados!");
            throw new UsernameNotFoundException("Error login: usuario '"+username+"' no tiene roles asignados!");
        }

        return new User(username, usuario.getPassword(), usuario.getEnable(), true, true, true, authorities);
    }
}
