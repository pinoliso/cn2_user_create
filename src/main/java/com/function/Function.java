package com.function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Properties;

public class Function {

     private static final Gson gson = new Gson();

    @FunctionName("rolesEventHandler")
    public void rolesEventHandler(
        @EventGridTrigger(name = "event", dataType = "String")
        String eventJson,
        final ExecutionContext context) {

        context.getLogger().info("Event Grid trigger ejecutado: " + eventJson);

        try {

            JsonObject json = gson.fromJson(eventJson, JsonObject.class);
            Rol rol = gson.fromJson(json.get("data").getAsString(), Rol.class);
            String op = json.get("subject").getAsString();

            switch (op) {
                case "create":
                    crearRol(rol, context);
                    break;
                case "update":
                    actualizarRol(rol, context);
                    break;
                case "delete":
                    eliminarRol(rol.id, context);
                    break;
                default:
                    context.getLogger().warning("Operación desconocida: " + op);
            }
        } catch (Exception e) {
            context.getLogger().severe("Error procesando evento: " + e.getMessage());
        }
    }

    private void crearRol(Rol rol, ExecutionContext context) throws Exception {
        Connection conn = conectarOracle(context);
        PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO rols (id, name) VALUES (?, ?)");
        stmt.setLong(1, rol.id);
        stmt.setString(2, rol.name);
        stmt.executeUpdate();
    }

    private void actualizarRol(Rol rol, ExecutionContext context) throws Exception {
        Connection conn = conectarOracle(context);
        PreparedStatement stmt = conn.prepareStatement(
            "UPDATE rols SET name = ? WHERE id = ?");
        stmt.setString(1, rol.name);
        stmt.setLong(2, rol.id);
        stmt.executeUpdate();
    }

    private void eliminarRol(Long id, ExecutionContext context) throws Exception {
        Connection conn = conectarOracle(context);
        PreparedStatement stmt = conn.prepareStatement(
            "DELETE FROM rols WHERE id = ?");
        stmt.setLong(1, id);
        stmt.executeUpdate();
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
