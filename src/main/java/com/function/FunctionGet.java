package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class FunctionGet {

    @FunctionName("user_get")
    public HttpResponseMessage run(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.GET},
            route = "usuarios/{id?}",
            authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<Optional<String>> request,
        @BindingName("id") String id,
        final ExecutionContext context
    ) {
        Logger log = context.getLogger();
        try {
            Connection conn = conectarOracle(context);

            if (id == null || id.isEmpty()) {
                log.info("Obteniendo todos los usuarios...");
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users");
                ResultSet rs = stmt.executeQuery();

                List<Map<String, Object>> usuarios = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> usuario = new HashMap<>();
                    usuario.put("id", rs.getLong("ID"));
                    usuario.put("username", rs.getString("USERNAME"));
                    usuario.put("password", rs.getString("PASSWORD"));
                    usuario.put("name", rs.getString("NAME"));
                    usuario.put("rol", rs.getString("ROL"));
                    usuarios.add(usuario);
                }

                return request.createResponseBuilder(HttpStatus.OK).body(usuarios).build();

            } else {
                log.info("Obteniendo usuario con ID: " + id);
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE ID = ?");
                stmt.setLong(1, Long.parseLong(id));
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    Map<String, Object> usuario = new HashMap<>();
                    usuario.put("id", rs.getLong("ID"));
                    usuario.put("username", rs.getString("USERNAME"));
                    usuario.put("password", rs.getString("PASSWORD"));
                    usuario.put("name", rs.getString("NAME"));
                    usuario.put("rol", rs.getString("ROL"));

                    return request.createResponseBuilder(HttpStatus.OK).body(usuario).build();
                } else {
                    return request.createResponseBuilder(HttpStatus.NOT_FOUND).body("Usuario no encontrado").build();
                }
            }

        } catch (Exception e) {
            log.severe("Error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage()).build();
        }
    }

    private Connection conectarOracle(ExecutionContext context) throws Exception {
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");
        String dbAlias = System.getenv("DB_ALIAS");
        String walletPath = System.getenv("WALLET_PATH");

        String jdbcUrl = "jdbc:oracle:thin:@" + dbAlias + "?TNS_ADMIN=" + walletPath;

        Properties props = new Properties();
        props.put("user", dbUser);
        props.put("password", dbPass);
        props.put("oracle.net.ssl_server_dn_match", "true");

        context.getLogger().info("Conectando a DB: " + jdbcUrl);
        return DriverManager.getConnection(jdbcUrl, props);
    }
}
