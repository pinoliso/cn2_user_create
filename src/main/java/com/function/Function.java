package com.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Optional;
import java.util.Properties;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("user_create")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
       try {
            // Convertir JSON a objeto
            ObjectMapper mapper = new ObjectMapper();
            String json = request.getBody().orElse("{}");
            User user = mapper.readValue(json, User.class);

            // Insertar en Oracle
            insertIntoOracle(user, context);

            return request.createResponseBuilder(HttpStatus.OK)
                .body("Usuario insertado correctamente.")
                .build();

        } catch (Exception e) {
            context.getLogger().severe("Error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al insertar: " + e.getMessage())
                .build();
        }
    }

    private void insertIntoOracle(User user, ExecutionContext context) throws Exception {

        File walletDir = new File("/home/site/wwwroot");
        if (walletDir.exists() && walletDir.isDirectory()) {
            for (File f : walletDir.listFiles()) {
                context.getLogger().info("Archivo en wallet: " + f.getName());
            }
        } else {
            context.getLogger().warning("Wallet no encontrado en /home/site/wwwroot/wallet");
        }

        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");
        String dbAlias = System.getenv("DB_ALIAS");
        String walletPath = System.getenv("WALLET_PATH");

        String jdbcUrl = "jdbc:oracle:thin:@" + dbAlias + "?TNS_ADMIN=" + walletPath;

        Properties props = new Properties();
        props.put("user", dbUser);
        props.put("password", dbPass);
        props.put("oracle.net.ssl_server_dn_match", "true");

        try {
            context.getLogger().severe("Connect db: " + jdbcUrl);
            Connection conn = DriverManager.getConnection(jdbcUrl, props);
            context.getLogger().severe("Connected db: ");
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users (username, password, name, rol) VALUES (?, ?, ?, ?)"
            );
            context.getLogger().severe("Prepared: ");
            // stmt.setLong(1, user.id);
            stmt.setString(1, user.username);
            stmt.setString(2, user.password);
            stmt.setString(3, user.name);
            stmt.setString(4, user.rol);
            stmt.executeUpdate();
            
            context.getLogger().severe("Executed: ");
        }catch (Exception e) {
            context.getLogger().severe("Error: " + e.getMessage());
            throw new Exception("Error al insertar: " + e.getMessage());
        }
    }
}
