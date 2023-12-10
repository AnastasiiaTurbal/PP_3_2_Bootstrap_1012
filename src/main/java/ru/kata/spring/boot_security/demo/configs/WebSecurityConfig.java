package ru.kata.spring.boot_security.demo.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.RoleServiceImpl;
import ru.kata.spring.boot_security.demo.service.UserService;
import ru.kata.spring.boot_security.demo.service.UserServiceImpl;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserService userService;
    private final RoleService roleService;
    private final SuccessUserHandler successUserHandler;
    private final PasswordEncoder passwordEncoder;

    public WebSecurityConfig(UserService userService, SuccessUserHandler successUserHandler, RoleServiceImpl roleService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.successUserHandler = successUserHandler;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeRequests()
                .antMatchers("/", "/login").permitAll()
                .antMatchers("/index**").hasAnyRole("ADMIN", "USER")
                .antMatchers("/otherRole").permitAll()
                .and()
                .formLogin()
                .successHandler(successUserHandler)
                .permitAll()
                .and()
                .logout()
                .logoutSuccessUrl("/login")
                .permitAll();
    }

    // аутентификация inMemory, пользователи хранятся в памяти
    /*@Bean
    @Override
    public UserDetailsService userDetailsService() {
        UserDetails user = //UserDetails - минимальное инфо о пользователях
                User.withDefaultPasswordEncoder()
                        .username("user")
                        .password("user")
                        .roles("USER")
                        .build();

        /*UserDetails admin =
                User.withDefaultPasswordEncoder()
                        .username("admin")
                        .password("admin")
                        .roles("ADMIN", "USER")
                        .build();

        return new InMemoryUserDetailsManager(user); //загоняем пользователей в память
    } */

    /*@Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); } */

    //хранение инфо, которое будет использоваться для аутентификации пользователей,в собственных таблицах (вместо памяти или стандартных таблиц, предоставляемых Spring)
    //получает логин и пароль и возвращает инфо, существует такой пользователь или нет
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        //daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        daoAuthenticationProvider.setUserDetailsService(userService); //предоставляет базу с пользователями для поиска в ней поступившего username (UserDetailsService вместо inMemory)

        //добавляем роли в БД
        for (String role : Arrays.asList("ROLE_USER","ROLE_ADMIN")) {
            if(!(roleService.findAllRoles().stream().map(Role::getName).collect(Collectors.toList()).contains(role))) {
                roleService.addRole(new Role(role));
            }
        }

        //удаляем админа, если уже есть в базе; добавляем с корректными ролями
        if (!(userService.findUserByUserName("admin") == null)) {
            userService.deleteUser(userService.findUserByUserName("admin").getId());
        }
        //userService.addUser(new ru.kata.spring.boot_security.demo.model.User("admin", passwordEncoder().encode("admin"),
        userService.addUser(new ru.kata.spring.boot_security.demo.model.User("admin", "admin",
                roleService.findAllRoles()));

        return daoAuthenticationProvider;
    }

}